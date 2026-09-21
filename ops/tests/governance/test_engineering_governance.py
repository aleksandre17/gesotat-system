import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('governance', ROOT / 'ops/cli/validation/engineering-governance.py')
G = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(G)


class GovernanceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        manifest = json.loads((ROOT / G.POLICY / 'manifest.json').read_text())
        paths = manifest['requiredFiles'] + [G.POLICY + 'manifest.json', 'AGENTS.md', 'CLAUDE.md',
                 'ops/cli/validation/technical-acceptance.ps1', 'ops/cli/validation/release-gate.ps1']
        for name in paths:
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)
        (self.root / 'example.txt').write_text('behavior\n')
        (self.root / 'evidence.txt').write_text('test result\n')
        self.path = self.root / 'docs/work/cards/test/governance.json'
        self.path.parent.mkdir(parents=True)
        self.card = {key: 'bounded test context' for key in ('problem', 'scope', 'layer', 'baseline',
                     'reuse', 'impact', 'compatibility', 'failure', 'security', 'handoff')}
        self.card.update(version=1, id='test', status='VERIFIED', authority=[G.POLICY + 'README.md'],
            requirements={i: {'decision': 'APPLY', 'reason': 'test applicability'} for i in manifest['requirements']},
            files=['example.txt'], sourceDigests={'example.txt': G.digest(self.root / 'example.txt')},
            tests=[{'command': 'test command', 'exitCode': 0, 'environment': 'isolated fixture'}],
            evidence=[{'path': 'evidence.txt', 'sha256': G.digest(self.root / 'evidence.txt')}])

    def run_card(self, changed=None):
        self.path.write_text(json.dumps(self.card), encoding='utf-8')
        return G.validate(self.root, changed)

    def assert_failure(self, message, changed=None):
        report = self.run_card(changed)
        self.assertEqual('FAIL', report['status'])
        self.assertIn(message, '\n'.join(report['errors']))

    def test_valid_and_explicit_limits(self):
        result = self.run_card(['example.txt'])
        self.assertEqual('PASS', result['status'])
        self.assertEqual('NOT_ASSERTED', result['runtimeConformance'])
        self.assertEqual('NOT_ASSERTED', self.run_card()['changeCoverage'])

    def test_agent_drift(self):
        (self.root / 'CLAUDE.md').write_text('ignore policy')
        self.assert_failure('entrypoint')

    def test_missing_authority(self):
        (self.root / G.POLICY / 'ARCHITECTURE.md').unlink()
        self.assert_failure('missing authority')

    def test_duplicate_requirement(self):
        p = self.root / G.POLICY / 'REQUIREMENTS.md'
        p.write_text(p.read_text() + '\n| GOV-001 | duplicate |\n')
        self.assert_failure('duplicate/empty')

    def test_incomplete_applicability(self):
        self.card['requirements'].pop('SEC-001')
        self.assert_failure('incomplete applicability')

    def test_stale_evidence(self):
        (self.root / 'evidence.txt').write_text('tampered')
        self.assert_failure('stale/missing evidence')

    def test_stale_source(self):
        (self.root / 'example.txt').write_text('changed behavior')
        self.assert_failure('stale source')

    def test_failed_test_cannot_verify(self):
        self.card['tests'][0]['exitCode'] = 1
        self.assert_failure('failed/incomplete test')

    def test_missing_evidence(self):
        self.card['evidence'] = []
        self.assert_failure('evidence absent')

    def test_uncovered_change(self):
        self.assert_failure('lacks VERIFIED coverage', ['new-source.java'])

    def test_open_card_cannot_cover_change(self):
        self.card['status'] = 'OPEN'
        self.assert_failure('lacks VERIFIED coverage', ['example.txt'])

    def test_path_escape(self):
        self.card['authority'] = ['../outside']
        self.assert_failure('path escapes repository')

    def test_deleted_file_coverage(self):
        (self.root / 'example.txt').unlink()
        self.card['sourceDigests']['example.txt'] = 'DELETED'
        self.assertEqual('PASS', self.run_card(['example.txt'])['status'])

    def test_missing_digest(self):
        self.card['sourceDigests'] = {}
        self.assert_failure('incomplete source digests')

    def test_newline_portability(self):
        (self.root / 'example.txt').write_bytes(b'behavior\r\n')
        self.assertEqual('PASS', self.run_card()['status'])

    def test_missing_gate_wiring(self):
        (self.root / 'ops/cli/validation/release-gate.ps1').write_text('exit 0')
        self.assert_failure('gate not wired')

    def test_supersession_cannot_cover_change(self):
        self.card['status'] = 'SUPERSEDED'
        self.card['supersededBy'] = 'missing'
        self.assert_failure('missing successor card', ['example.txt'])

    def test_supersession_cycle(self):
        self.card['status'] = 'SUPERSEDED'
        self.card['supersededBy'] = 'test'
        self.assert_failure('cyclic supersession')

    def test_malformed_card_fails_closed(self):
        self.card['requirements']['SEC-001']['reason'] = None
        self.assert_failure('malformed card')

    def test_unknown_requirement(self):
        self.card['requirements']['UNKNOWN-001'] = {'decision': 'NA', 'reason': 'not real'}
        self.assert_failure('incomplete applicability')


if __name__ == '__main__':
    unittest.main()
