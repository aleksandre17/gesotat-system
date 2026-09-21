"""Original Project Operating System specification — coverage against the repository.

One test per mechanism the commissioning specification required. Each asserts repository
evidence, because the specification's own rule is that a mechanism existing only as prose
is not implemented. Prose claims cannot fail; these can.

Classification vocabulary used in the docstrings:
  IMPLEMENTED_OPERATIONAL  - the mechanism exists and is exercised
  LEGITIMATE_LAZY          - deliberately not instantiated until first use (RCP #9)

Run: python -m unittest discover -s ops/tests/governance
"""

import importlib.util
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('rcp', ROOT / 'ops/cli/validation/rcp-verify.py')
V = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(V)


def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8', errors='replace')


CURRENT = read('docs/project/CURRENT.md')
MANIFEST = read('docs/project/MANIFEST.md')
CATALOG = read('docs/project/CATALOG.md')
ADOPTION = read('docs/project/ADOPTION.md')
README = read('docs/project/README.md')
STANDARD = read('docs/standards/PROJECT-OPERATING-SYSTEM.md')
AGENTS = read('AGENTS.md')
WORK = read('docs/project/work/README.md')
TEMPLATE = read('docs/project/work/_TASK-TEMPLATE.md')


class PlaneTests(unittest.TestCase):
    """The seven semantic planes. Ownership and routing, never seven directory trees."""

    def test_control_plane(self):
        self.assertIn('| Control |', CATALOG)
        self.assertIn('LIFECYCLE PHASE', CURRENT)
        self.assertIn('Forbidden actions', CURRENT)

    def test_knowledge_plane(self):
        self.assertIn('| Knowledge |', CATALOG)
        self.assertTrue((ROOT / 'docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md').is_file())

    def test_decision_plane(self):
        self.assertIn('## Decisions', CATALOG)
        adrs = sorted((ROOT / 'docs/decisions').glob('ADR-*.md'))
        self.assertGreaterEqual(len(adrs), 4)
        for adr in adrs:
            rel = 'docs/decisions/' + adr.name
            self.assertTrue(rel in ADOPTION or adr.name in ADOPTION,
                            f'orphan decision: {rel}')

    def test_work_plane(self):
        self.assertIn('| Work |', CATALOG)
        self.assertIn('required_context', WORK)
        self.assertIn('required_context', TEMPLATE)

    def test_governance_plane(self):
        self.assertIn('| Governance |', CATALOG)
        self.assertTrue((ROOT / 'ops/cli/validation/rcp-verify.py').is_file())
        self.assertIn('OBL-RCP-VERIFY', MANIFEST)

    def test_evidence_plane(self):
        self.assertIn('| Evidence |', CATALOG)
        card = ROOT / 'docs/work/cards/repository-control-protocol/governance.json'
        self.assertTrue(card.is_file())
        data = json.loads(card.read_text(encoding='utf-8'))
        self.assertTrue(data['evidence'])
        for item in data['evidence']:
            self.assertTrue((ROOT / item['path']).is_file())

    def test_implementation_plane_is_routed_not_catalogued(self):
        """Code must be reachable, and must never become a project-state authority."""
        self.assertIn('| Implementation |', CATALOG)
        self.assertIn('CANONICAL-FULL-TREE.md', CATALOG)
        self.assertIn('never a state authority', CATALOG)


class MechanismTests(unittest.TestCase):
    """Every mechanism the original specification named."""

    # ---- state, lifecycle, work ------------------------------------------
    def test_current_project_state(self):
        for field in ('LIFECYCLE PHASE', 'STATUS', 'LAST COMPLETED GATE', 'CURRENT GATE'):
            self.assertIn(field, CURRENT)

    def test_complete_lifecycle_and_roadmap(self):
        phases = sorted(int(p) for p in set(re.findall(r'PHASE-(\d{3})', MANIFEST)))
        self.assertEqual(phases, list(range(1, len(phases) + 1)))
        self.assertGreaterEqual(len(phases), 13)

    def test_phase_work_item_task_model(self):
        self.assertIn('PHASE-', MANIFEST)
        self.assertIn('TASK-', TEMPLATE)
        self.assertIn('active', WORK)

    def test_dependencies(self):
        self.assertIn('Dependencies', TEMPLATE)
        self.assertIn('RCP-606', WORK)

    def test_blockers(self):
        self.assertIn('## Blockers', CURRENT)
        self.assertIn('blocked/', WORK)

    def test_gates(self):
        gates = set(re.findall(r'`(GATE-[A-Z0-9-]+)`', MANIFEST))
        self.assertGreaterEqual(len(gates), 13)
        self.assertIn('Failure consequence', MANIFEST)

    def test_acceptance_and_verification(self):
        self.assertIn('## Verification', TEMPLATE)
        self.assertIn('Evidence required', TEMPLATE)

    # ---- knowledge, decisions, authority ---------------------------------
    def test_architecture_domain_contract_knowledge(self):
        self.assertIn('REC-CONSOLIDATION', CATALOG)
        self.assertIn('docs/reference/engineering', ADOPTION)

    def test_decisions_and_rationale(self):
        adr = read('docs/decisions/ADR-repository-control-protocol.md')
        for section in ('## Context', '## Decision', '## Alternatives considered',
                        '## Consequences'):
            self.assertIn(section, adr)

    def test_invariants(self):
        self.assertIn('ONE FACT', STANDARD + read('docs/decisions/ADR-repository-control-protocol.md'))
        self.assertIn('CAD-01', read('docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md'))

    def test_canonical_authorities(self):
        self.assertIn('AUTHORITY REGISTRY', read(
            'docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md').upper())

    # ---- document governance ---------------------------------------------
    def test_document_taxonomy(self):
        self.assertGreaterEqual(len(V.TYPES), 8)
        self.assertGreaterEqual(len(V.AUTHORITIES), 5)

    def test_metadata(self):
        self.assertEqual(V.REQUIRED_META, ('id', 'type', 'title', 'status', 'authority'))
        for rel in ('docs/project/CURRENT.md', 'docs/project/MANIFEST.md'):
            self.assertIsNotNone(V.front_matter(ROOT / rel))

    def test_supersession(self):
        self.assertIn('SUPERSEDED', ADOPTION)
        self.assertIn('superseded_by', STANDARD + read('ops/cli/validation/rcp-verify.py'))

    def test_history_is_distinguishable_from_current(self):
        self.assertIn('HISTORICAL', ADOPTION)
        self.assertIn('| HISTORICAL |', ADOPTION)

    def test_evidence(self):
        self.assertTrue((ROOT / 'docs/work/evidence/repository-control-protocol/tests.txt').is_file())

    def test_artifact_catalog_and_navigation(self):
        self.assertIn('READ_WHEN', CATALOG)
        self.assertIn('Where each kind of knowledge lives', CATALOG)

    def test_progressive_disclosure(self):
        self.assertRegex(CATALOG, r'\*\*L[0-4]')
        self.assertIn('Do not read the documentation tree by default', README)

    def test_required_context(self):
        self.assertIn('Required read set', CURRENT)
        self.assertIn('required_context', TEMPLATE)

    # ---- protocols and agnosticism ---------------------------------------
    def test_session_start_protocol(self):
        self.assertIn('Start of any substantial work', AGENTS)
        self.assertIn('docs/project/CURRENT.md', AGENTS)

    def test_session_end_protocol(self):
        self.assertIn('End of any substantial work', AGENTS)
        self.assertIn('rcp-verify.py', AGENTS)

    def test_global_agent_constitution(self):
        self.assertIn('PROJECT-CONTROL:BEGIN', AGENTS)
        self.assertIn('ENGINEERING-GOVERNANCE:BEGIN', AGENTS)

    def test_scoped_instructions_only_when_justified(self):
        """A second instruction file must not silently weaken the global one."""
        self.assertIn('local instructions cannot silently weaken them', read('CLAUDE.md'))

    def test_large_register_scaling(self):
        """Registers scale by row, and oversize is surfaced rather than ignored."""
        self.assertIn('COHESION_LINES', read('ops/cli/validation/rcp-verify.py'))

    def test_derived_information_is_not_an_authority(self):
        self.assertIn('never a state authority', CATALOG)
        self.assertIn('derived', STANDARD.lower())

    def test_external_tool_agnosticism(self):
        """The validator must be stdlib-only: no dependency may gate governance."""
        src = read('ops/cli/validation/rcp-verify.py')
        imports = set(re.findall(r'^(?:from|import)\s+(\w+)', src, re.M))
        self.assertTrue(imports <= {'__future__', 'argparse', 're', 'sys', 'pathlib'},
                        f'non-stdlib dependency: {imports}')

    def test_domain_technology_agent_agnosticism(self):
        """The generic protocol must contain zero project facts."""
        for token in ('GEOSTAT', 'KIDS', 'Geostat', 'Access', 'SQL Server', 'CF-0'):
            self.assertNotIn(token, STANDARD, f'project fact leaked into the standard: {token}')

    # ---- protections ------------------------------------------------------
    def test_anti_bloat(self):
        """RCP #9 - lazy instantiation. A work-state folder may exist only while it holds work.

        This assertion previously read `active/ must not exist`, which was true while no item
        was ever in flight and became wrong the moment one was: PHASE-004 opened TASK-006. The
        rule being protected is "no ceremonial EMPTY folder", not "no folder", so the check now
        tests the rule instead of the circumstance. It is strictly stronger: it fires on an
        empty active/, blocked/ or completed/ directory, which the old form could not detect.
        """
        self.assertIn('no ceremonial folders', CATALOG)
        for state in ('active', 'blocked', 'completed'):
            folder = ROOT / 'docs/project/work' / state
            if folder.exists():
                items = [p for p in folder.glob('*.md')
                         if p.name != 'README.md' and not p.name.startswith('_')]
                self.assertTrue(items,
                                f'ceremonial empty work folder was created: work/{state}')

    def test_anti_parallelism(self):
        self.assertIn('RCP-404', read('ops/cli/validation/rcp-verify.py'))
        self.assertIn('OBL-ONE-AUTHORITY', MANIFEST)

    def test_deterministic_validation(self):
        report, _ = V.verify(ROOT)
        self.assertEqual(report.errors, [])
        self.assertGreater(report.checks, 200)

    def test_repository_only_resumability(self):
        self.assertTrue((ROOT / 'ops/tests/governance/test_rcp_bootstrap.py').is_file())

    def test_legitimate_lazy_instantiation(self):
        """Absent work folders are legal, and the rule is written down."""
        self.assertIn('lazy instantiation', WORK)
        self.assertIn('never pre-created empty', WORK)


if __name__ == '__main__':
    unittest.main()
