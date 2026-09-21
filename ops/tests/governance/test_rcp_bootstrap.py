"""Repository-only continuation proof.

Asserts against the *real* repository that a fresh agent with no access to any previous
conversation can answer every question needed to continue the rehabilitation. Each test is
one such question.

This is a regression test for chat-dependency. A claim that "a new agent could continue"
is worth nothing as a sentence in a report — it decays the moment someone edits a control
artifact. Here it fails the build instead.

Run: python -m unittest discover -s ops/tests/governance
"""

import importlib.util
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('rcp', ROOT / 'ops/cli/validation/rcp-verify.py')
V = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(V)

CURRENT = (ROOT / 'docs/project/CURRENT.md').read_text(encoding='utf-8')
MANIFEST = (ROOT / 'docs/project/MANIFEST.md').read_text(encoding='utf-8')
CATALOG = (ROOT / 'docs/project/CATALOG.md').read_text(encoding='utf-8')
AGENTS = (ROOT / 'AGENTS.md').read_text(encoding='utf-8')

# The commissioned rehabilitation program, in order. A phase may be renamed only with the
# roadmap correction recorded in MANIFEST; it may never disappear.
PROGRAM = [
    'RECOVERY',
    'STANDARDS',
    'ARTIFACT COMPARATIVE AUDIT',
    'CANONICAL DESIGN',
    'MASTER REHABILITATION PLAN',
    'EXECUTION PACKAGE',
    'INDEPENDENT ADVERSARIAL REVIEW',
    'CORRECTED / APPROVED CANONICAL PLAN',
    'PROTECTION',
    'CONTROLLED IMPLEMENTATION / MIGRATION',
    'LEGACY ELIMINATION',
    'FINAL ARCHITECTURE AUDIT',
    'REHABILITATION COMPLETE',
]


class BootstrapTests(unittest.TestCase):
    """Can a fresh agent determine … ?"""

    def test_what_project_this_is(self):
        """A cold-start agent must learn what the system is, not only where the work is."""
        self.assertIn('**Project:**', CURRENT)
        for token in ('Contract-Driven', 'Metadata-Driven', 'Schema-Agnostic'):
            self.assertIn(token, CURRENT)
        self.assertIn('CANONICAL-FULL-TREE.md', CURRENT)

    def test_entry_point_routes_to_the_control_plane(self):
        self.assertIn('docs/project/CURRENT.md', AGENTS)
        self.assertIn('rcp-verify.py', AGENTS)

    def test_current_phase_and_state(self):
        self.assertRegex(CURRENT, r'LIFECYCLE PHASE\W+`?PHASE-\d{3}`?|LIFECYCLE PHASE\W+`\w+`')
        self.assertRegex(CURRENT, r'\*\*STATUS\*\*\s*\|\s*`?[A-Z_]+`?')

    def test_last_completed_gate(self):
        self.assertRegex(CURRENT, r'LAST COMPLETED GATE.*GATE-[A-Z0-9-]+')

    def test_next_permitted_phase(self):
        self.assertIn('Next permitted action', CURRENT)
        self.assertRegex(CURRENT, r'`PHASE-\d{3}`')

    def test_complete_remaining_lifecycle_is_recoverable(self):
        """Every step of the commissioned program must still be a declared phase."""
        for step in PROGRAM:
            self.assertIn(step, MANIFEST, f'lifecycle step lost from MANIFEST: {step}')

    def test_lifecycle_has_no_second_roadmap(self):
        manifests = [p for p in ROOT.rglob('MANIFEST.md')
                     if '.git' not in p.parts and 'node_modules' not in p.parts]
        self.assertEqual(len(manifests), 1, f'more than one roadmap: {manifests}')

    def test_required_context_for_the_next_phase(self):
        self.assertIn('Required read set', CURRENT)
        reads = re.findall(r'`((?:docs|ops)/[^`\s]+?\.md)`', CURRENT)
        self.assertTrue(reads, 'CURRENT declares no required reads')
        for ref in reads:
            self.assertTrue((ROOT / ref).is_file(), f'required read is missing: {ref}')

    def test_blockers_and_material_unknowns(self):
        self.assertIn('## Blockers', CURRENT)
        self.assertIn('Material unknowns', CURRENT)

    def test_future_obligations(self):
        self.assertIn('Standing obligations', MANIFEST)
        self.assertIn('Deferred hardening', MANIFEST)
        self.assertRegex(MANIFEST, r'`DEF-0\d`')
        self.assertIn('CLEAN_BUILD_UPGRADE_EQUIVALENCE', MANIFEST)

    def test_canonical_authority_locations(self):
        self.assertIn('DOC-CAD', CATALOG)
        doctrine = ROOT / 'docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md'
        self.assertTrue(doctrine.is_file())
        self.assertIn('CAD-02', doctrine.read_text(encoding='utf-8'))

    def test_where_decisions_findings_evidence_and_history_live(self):
        for marker in ('REC-CONSOLIDATION', 'REC-PASS3', 'REC-AUDIT', 'ADR-'):
            self.assertIn(marker, CATALOG, f'CATALOG does not route to {marker}')

    def test_rcp_adoption_decision_is_recorded(self):
        adr = ROOT / 'docs/decisions/ADR-repository-control-protocol.md'
        self.assertTrue(adr.is_file(), 'the decision to adopt RCP has no decision record')
        text = adr.read_text(encoding='utf-8')
        for section in ('## Context', '## Decision', '## Alternatives considered',
                        '## Consequences'):
            self.assertIn(section, text)

    def test_forbidden_actions(self):
        self.assertIn('Forbidden actions', CURRENT)

    def test_control_plane_validates_clean(self):
        report, _ = V.verify(ROOT)
        self.assertEqual(report.errors, [], f'control plane is not clean: {report.errors}')


if __name__ == '__main__':
    unittest.main()
