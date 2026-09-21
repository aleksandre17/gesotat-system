"""Adversarial tests for the Repository Control Protocol validator.

Each test injects one defect into an otherwise-valid control plane and asserts that the
specific check fires. A validator that has only ever been run against a passing repository
is an unproven validator: PASS then means "no defect present" and "cannot detect defects"
equally well, and the two are indistinguishable from the outside.

Run: python -m unittest discover -s ops/tests/governance
"""

import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('rcp', ROOT / 'ops/cli/validation/rcp-verify.py')
V = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(V)


def meta(doc_id, title, authority='CANONICAL', doc_type='CONTROL', **extra):
    lines = [f'id: {doc_id}', f'type: {doc_type}', f'title: {title}',
             'status: ACTIVE', f'authority: {authority}']
    lines += [f'{k}: {v}' for k, v in extra.items()]
    return '---\n' + '\n'.join(lines) + '\n---\n\n'


CURRENT = meta('CTRL-CURRENT', 'Current state') + """# CURRENT

| Field | Value |
|---|---|
| **STATUS** | `COMPLETE` |
| **CURRENT GATE** | `GATE-STANDARDS` |

Next: `PHASE-002`.

## Blockers

None.

## Required read set

| # | Artifact |
|---|---|
| 1 | `docs/project/MANIFEST.md` |
"""

MANIFEST = meta('CTRL-MANIFEST', 'Lifecycle') + """# MANIFEST

| ID | Phase | Status | Purpose |
|---|---|---|---|
| `PHASE-001` | RECOVERY | COMPLETE | understand the system |
| `PHASE-002` | STANDARDS | READY | benchmark it |
| `PHASE-003` | DONE | NOT_STARTED | terminal state; nothing follows |

| Gate | Result |
|---|---|
| `GATE-RECOVERY` | PASS |
| `GATE-STANDARDS` | NOT_STARTED |
| `GATE-DONE` | NOT_STARTED |
"""

CATALOG = meta('CTRL-CATALOG', 'Catalog') + """# CATALOG

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `CTRL-CURRENT` | `docs/project/CURRENT.md` | CONTROL | CANONICAL | current state | L0 |
| `CTRL-MANIFEST` | `docs/project/MANIFEST.md` | CONTROL | CANONICAL | lifecycle and gates | L0 |
| `CTRL-ADOPTION` | `docs/project/ADOPTION.md` | REGISTER | CANONICAL | pre-existing mapping | bootstrap |
| `STD-RCP` | `docs/standards/PROJECT-OPERATING-SYSTEM.md` | REFERENCE | CANONICAL | the generic protocol | on change |
"""

ADOPTION = meta('CTRL-ADOPTION', 'Adoption', doc_type='REGISTER') + """# ADOPTION

| Path | Authority | Disposition |
|---|---|---|
| `docs/legacy-final-plan.md` | HISTORICAL | `ARCHIVE_LATER` |
"""

STANDARD = meta('STD-RCP', 'Protocol', doc_type='REFERENCE') + '# RCP\n'


class RcpValidatorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.write('docs/project/CURRENT.md', CURRENT)
        self.write('docs/project/MANIFEST.md', MANIFEST)
        self.write('docs/project/CATALOG.md', CATALOG)
        self.write('docs/project/ADOPTION.md', ADOPTION)
        self.write('docs/standards/PROJECT-OPERATING-SYSTEM.md', STANDARD)
        self.write('docs/legacy-final-plan.md', '# legacy\n')

    def write(self, rel, text):
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')
        return path

    def codes(self):
        report, _ = V.verify(self.root)
        return [e.split(']')[0].lstrip('[') for e in report.errors]

    def assertFires(self, code):
        found = self.codes()
        self.assertIn(code, found, f'{code} did not fire; errors were {found}')

    # ---------------------------------------------------------- baseline
    def test_valid_control_plane_passes(self):
        """The fixture itself must be clean, or every negative test is meaningless."""
        report, _ = V.verify(self.root)
        self.assertEqual(report.errors, [], f'fixture is not clean: {report.errors}')
        self.assertGreater(report.checks, 20)

    # ---------------------------------------------------------- singletons
    def test_second_current_file_detected(self):
        self.write('docs/other/CURRENT.md', CURRENT)
        self.assertFires('RCP-101')

    def test_second_manifest_detected(self):
        self.write('docs/other/MANIFEST.md', MANIFEST)
        self.assertFires('RCP-102')

    # ---------------------------------------------------------- metadata
    def test_missing_front_matter_detected(self):
        self.write('docs/project/CURRENT.md', '# CURRENT\n')
        self.assertFires('RCP-201')

    def test_missing_required_field_detected(self):
        self.write('docs/project/ADOPTION.md',
                   '---\nid: X\ntype: REGISTER\nstatus: ACTIVE\nauthority: CANONICAL\n---\n')
        self.assertFires('RCP-202')

    def test_type_outside_vocabulary_detected(self):
        self.write('docs/project/ADOPTION.md', meta('CTRL-ADOPTION', 'A', doc_type='WHATEVER'))
        self.assertFires('RCP-203')

    def test_authority_outside_vocabulary_detected(self):
        self.write('docs/project/ADOPTION.md', meta('CTRL-ADOPTION', 'A', authority='FINAL'))
        self.assertFires('RCP-204')

    def test_duplicate_id_detected(self):
        self.write('docs/project/ADOPTION.md', meta('CTRL-CURRENT', 'Clone', doc_type='REGISTER'))
        self.assertFires('RCP-205')

    # ---------------------------------------------------------- supersession
    def test_unresolvable_superseded_by_detected(self):
        self.write('docs/project/ADOPTION.md',
                   meta('CTRL-ADOPTION', 'A', doc_type='REGISTER', superseded_by='GHOST-1'))
        self.assertFires('RCP-301')

    def test_superseded_without_target_detected(self):
        self.write('docs/project/ADOPTION.md',
                   meta('CTRL-ADOPTION', 'A', authority='SUPERSEDED', doc_type='REGISTER'))
        self.assertFires('RCP-303')

    def test_supersession_cycle_detected(self):
        self.write('docs/project/ADOPTION.md',
                   meta('CTRL-ADOPTION', 'A', doc_type='REGISTER', superseded_by='CTRL-CATALOG'))
        self.write('docs/project/CATALOG.md', CATALOG.replace(
            'authority: CANONICAL', 'authority: CANONICAL\nsuperseded_by: CTRL-ADOPTION', 1))
        self.assertFires('RCP-304')

    # ---------------------------------------------------------- catalog
    def test_catalog_reference_to_missing_path_detected(self):
        self.write('docs/project/CATALOG.md', CATALOG + '\n| x | `docs/ghost.md` | R | S | y | z |\n')
        self.assertFires('RCP-402')

    def test_canonical_artifact_absent_from_catalog_detected(self):
        self.write('docs/project/CATALOG.md', CATALOG.replace(
            '| `CTRL-ADOPTION` | `docs/project/ADOPTION.md` | REGISTER | CANONICAL'
            ' | pre-existing mapping | bootstrap |\n', ''))
        self.assertFires('RCP-403')

    def test_adopted_canonical_without_catalog_routing_detected(self):
        """Authority without navigation is authority nobody can reach."""
        self.write('docs/important-plan.md', '# plan\n')
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `docs/important-plan.md` | CANONICAL | `KEEP_IN_PLACE` |\n')
        self.assertFires('RCP-403')

    def test_authority_is_read_from_a_cell_not_the_filename(self):
        """A path containing an authority word must not classify itself."""
        self.write('docs/CANONICAL-OBJECT-FLOW.md', '# flow\n')
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `docs/CANONICAL-OBJECT-FLOW.md` | SUPPORTING | `KEEP_IN_PLACE` |\n')
        codes = self.codes()
        self.assertNotIn('RCP-403', codes)  # SUPPORTING needs no catalog row
        self.assertNotIn('RCP-701', codes)  # but it IS classified

    def test_duplicate_canonical_ownership_detected(self):
        """Two CANONICAL rows owning the same responsibility — the CAD-01 failure."""
        self.write('docs/project/CATALOG.md', CATALOG +
                   '| `CTRL-RIVAL` | `docs/project/MANIFEST.md` | CONTROL | CANONICAL'
                   ' | Lifecycle and Gates! | L0 |\n')
        self.assertFires('RCP-404')

    # ---------------------------------------------------------- current/manifest
    def test_gate_unknown_to_manifest_detected(self):
        self.write('docs/project/CURRENT.md', CURRENT.replace('GATE-STANDARDS', 'GATE-INVENTED'))
        self.assertFires('RCP-504')

    def test_phase_unknown_to_manifest_detected(self):
        self.write('docs/project/CURRENT.md', CURRENT.replace('PHASE-002', 'PHASE-099'))
        self.assertFires('RCP-505')

    def test_status_outside_vocabulary_detected(self):
        self.write('docs/project/CURRENT.md', CURRENT.replace('`COMPLETE`', '`MOSTLY_DONE`'))
        self.assertFires('RCP-506')

    def test_missing_required_read_path_detected(self):
        self.write('docs/project/CURRENT.md',
                   CURRENT.replace('`docs/project/MANIFEST.md`', '`docs/project/GONE.md`'))
        self.assertFires('RCP-508')

    def test_superseded_artifact_in_required_read_set_detected(self):
        self.write('docs/project/MANIFEST.md', MANIFEST.replace(
            'authority: CANONICAL', 'authority: SUPERSEDED\nsuperseded_by: CTRL-CURRENT', 1))
        self.assertFires('RCP-509')

    # ---------------------------------------------------------- work items
    def _work_item(self, folder, status, body='required_context: none\n'):
        self.write(f'docs/project/work/{folder}/TASK-001-x.md',
                   meta('TASK-001', 'x', authority='SUPPORTING', doc_type='WORK').replace(
                       'status: ACTIVE', f'status: {status}') + body)

    def test_completed_item_in_active_folder_detected(self):
        self._work_item('active', 'COMPLETE')
        self.assertFires('RCP-602')

    def test_open_item_in_completed_folder_detected(self):
        self._work_item('completed', 'ACTIVE')
        self.assertFires('RCP-603')

    def test_work_item_without_required_context_detected(self):
        self._work_item('active', 'ACTIVE', body='No context declared.\n')
        self.assertFires('RCP-604')

    def test_work_item_reference_to_missing_path_detected(self):
        self._work_item('active', 'ACTIVE',
                        body='required_context: `docs/project/GONE.md`\n')
        self.assertFires('RCP-605')

    def test_unresolvable_work_dependency_detected(self):
        self._work_item('active', 'ACTIVE',
                        body='required_context: none\n\nDepends on `TASK-404`.\n')
        self.assertFires('RCP-606')

    def test_superseded_artifact_in_work_context_detected(self):
        self.write('docs/dead.md', meta('DEAD-1', 'Dead', authority='SUPERSEDED',
                                        doc_type='REFERENCE', superseded_by='CTRL-CURRENT'))
        self._work_item('active', 'ACTIVE', body='required_context: `docs/dead.md`\n')
        self.assertFires('RCP-607')

    # ---------------------------------------------------------- CF-042 class
    def test_unclassified_canonical_looking_document_detected(self):
        self.write('docs/the-final-architecture.md', '# whatever\n')
        self.assertFires('RCP-701')

    def test_mention_without_assigned_authority_is_not_classification(self):
        """The earlier, weaker rule let a bare mention clear the check. It must not."""
        self.write('docs/the-final-architecture.md', '# whatever\n')
        self.write('docs/project/ADOPTION.md',
                   ADOPTION + '\nSee also `docs/the-final-architecture.md`.\n')
        self.assertFires('RCP-701')

    def test_assigned_authority_clears_the_check(self):
        self.write('docs/the-final-architecture.md', '# whatever\n')
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `docs/the-final-architecture.md` | HISTORICAL | `ARCHIVE_LATER` |\n')
        self.assertNotIn('RCP-701', self.codes())

    def test_state_asserting_filename_detected(self):
        """CF-043 class: a handoff claiming to be read first is a current-state authority."""
        self.write('docs/CONTINUATION_HANDOFF.md', '# authoritative continuation note\n')
        self.assertFires('RCP-701')

    def test_root_level_transcript_detected(self):
        """CF-044 class: repository-root markdown is in scope, not only docs/."""
        self.write('codex-session-deadbeef.md', '# Codex conversation\n')
        self.assertFires('RCP-701')

    def test_root_artifact_classified_by_bare_name_passes(self):
        self.write('codex-session-deadbeef.md', '# Codex conversation\n')
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `codex-session-deadbeef.md` | HISTORICAL | `ARCHIVE_LATER` |\n')
        self.assertNotIn('RCP-701', self.codes())

    def test_bare_name_in_prose_does_not_invent_a_path(self):
        """A filename that does not exist at the root must not become a governed path."""
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `NOT-A-REAL-FILE.md` | HISTORICAL | `ARCHIVE_LATER` |\n')
        self.assertNotIn('RCP-702', self.codes())

    def test_adoption_row_pointing_at_nothing_detected(self):
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `docs/never-existed.md` | HISTORICAL | `ARCHIVE_LATER` |\n')
        self.assertFires('RCP-702')

    # ---------------------------------------------------------- roadmap integrity
    def test_dropped_lifecycle_step_detected(self):
        """A step removed from the middle leaves a gap in the phase sequence."""
        self.write('docs/project/MANIFEST.md', MANIFEST.replace(
            '| `PHASE-002` | STANDARDS | READY | benchmark it |\n', ''))
        self.assertFires('RCP-511')

    def test_absorbed_terminal_step_detected(self):
        """Merging the last step into its predecessor removes the terminal marker."""
        self.write('docs/project/MANIFEST.md', MANIFEST.replace(
            '| `PHASE-003` | DONE | NOT_STARTED | terminal state; nothing follows |\n', ''))
        self.assertFires('RCP-511')

    def test_two_terminal_phases_detected(self):
        self.write('docs/project/MANIFEST.md', MANIFEST.replace(
            '| `PHASE-002` | STANDARDS | READY | benchmark it |',
            '| `PHASE-002` | STANDARDS | READY | terminal state; nothing follows |'))
        self.assertFires('RCP-511')

    def test_phase_without_entry_gate_detected(self):
        self.write('docs/project/MANIFEST.md', MANIFEST.replace(
            '| `GATE-DONE` | NOT_STARTED |\n', ''))
        self.assertFires('RCP-512')

    # ---------------------------------------------------------- RCP #22 residue
    def test_missing_blockers_section_detected(self):
        self.write('docs/project/CURRENT.md', CURRENT.replace('## Blockers\n\nNone.\n', ''))
        self.assertFires('RCP-510')

    def test_blocked_status_claiming_no_blockers_detected(self):
        """A blocked project that reads as unblocked hides the cause from the next agent."""
        self.write('docs/project/CURRENT.md', CURRENT.replace('`COMPLETE`', '`BLOCKED`'))
        self.assertFires('RCP-510')

    def test_orphan_decision_record_detected(self):
        self.write('docs/decisions/ADR-nobody-classified-this.md', '# ADR\n')
        self.assertFires('RCP-703')

    def test_classified_decision_record_passes(self):
        self.write('docs/decisions/ADR-known.md', '# ADR\n')
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `docs/decisions/ADR-known.md` | CANONICAL | `KEEP_IN_PLACE` |\n')
        self.assertNotIn('RCP-703', self.codes())

    def test_decision_missing_from_the_register_detected(self):
        """A register that stops at an older ADR answers the question wrongly."""
        self.write('docs/platform-decisions.md',
                   '# Decisions\n\n## ADR-001 — first\n\nfull text: `docs/decisions/ADR-first.md`\n')
        self.write('docs/decisions/ADR-first.md', '# ADR-001\n')
        self.write('docs/decisions/ADR-second.md', '# ADR-002\n')
        self.write('docs/project/ADOPTION.md', ADOPTION +
                   '| `docs/decisions/ADR-first.md` | CANONICAL | `KEEP_IN_PLACE` |\n'
                   '| `docs/decisions/ADR-second.md` | CANONICAL | `KEEP_IN_PLACE` |\n')
        self.assertFires('RCP-704')

    def test_oversized_artifact_warns_but_does_not_fail(self):
        """Length is a cohesion signal, not a defect — an evidence trail is legitimately long."""
        self.write('docs/project/ADOPTION.md',
                   ADOPTION + ('padding line\n' * (V.COHESION_LINES + 10)))
        report, _ = V.verify(self.root)
        self.assertTrue(any('RCP-801' in w for w in report.warnings), report.warnings)
        self.assertEqual(report.errors, [])


if __name__ == '__main__':
    unittest.main()
