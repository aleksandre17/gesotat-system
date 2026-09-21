"""Structural governance gate. No claim of runtime or architectural conformance."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

POLICY = 'docs/reference/engineering/'
BEGIN = '<!-- ENGINEERING-GOVERNANCE:BEGIN -->'
END = '<!-- ENGINEERING-GOVERNANCE:END -->'


def digest(path):
    data = path.read_bytes()
    try:
        data = data.decode('utf-8').replace('\r\n', '\n').encode('utf-8')
    except UnicodeDecodeError:
        pass
    return hashlib.sha256(data).hexdigest()


def local(root, value):
    if not isinstance(value, str) or not value or '\\' in value:
        raise ValueError('expected nonempty repository-relative POSIX path')
    path = (root / value).resolve()
    if Path(value).is_absolute() or not path.is_relative_to(root.resolve()):
        raise ValueError('path escapes repository: ' + value)
    return path


def validate(root, changed=None):
    errors, coverage = [], set()
    def require(condition, message):
        if not condition:
            errors.append(message)
    try:
        manifest = json.loads((root / POLICY / 'manifest.json').read_text(encoding='utf-8'))
        require(manifest['version'] == 1, 'unsupported manifest version')
        for path in manifest['requiredFiles']:
            require(local(root, path).is_file(), 'missing authority: ' + path)
        expected = (root / POLICY / 'agent-entrypoint.md').read_text(encoding='utf-8').strip()
        for name in ('AGENTS.md', 'CLAUDE.md'):
            content = (root / name).read_text(encoding='utf-8')
            require(content.count(BEGIN) == 1 and content.count(END) == 1,
                    name + ': missing/duplicate governance entrypoint')
            block = content[content.find(BEGIN):content.find(END) + len(END)].strip()
            require(block == expected, name + ': governance entrypoint drift')
        catalogue = (root / POLICY / 'REQUIREMENTS.md').read_text(encoding='utf-8')
        ids = re.findall(r'^\| ([A-Z]+-\d{3}) \|', catalogue, re.M)
        require(bool(ids) and len(ids) == len(set(ids)), 'duplicate/empty requirement IDs')
        require(set(ids) == set(manifest['requirements']), 'manifest/catalogue mismatch')
        for name in ('technical-acceptance.ps1', 'release-gate.ps1'):
            content = (root / 'ops/cli/validation' / name).read_text(encoding='utf-8')
            require('engineering-governance.py' in content, name + ': gate not wired')
        cards = sorted((root / 'docs/work/cards').glob('*/governance.json'))
        require(bool(cards), 'no change records')
        card_ids, supersessions = set(), {}
        for path in cards:
            label = str(path.relative_to(root))
            try:
                card = json.loads(path.read_text(encoding='utf-8'))
                require(card['version'] == 1, label + ': unsupported version')
                require(card['id'] == path.parent.name and card['id'] not in card_ids,
                        label + ': invalid/duplicate card ID')
                card_ids.add(card['id'])
                for field in ('problem', 'scope', 'layer', 'baseline', 'reuse', 'impact',
                              'compatibility', 'failure', 'security', 'handoff'):
                    require(isinstance(card[field], str) and bool(card[field].strip()),
                            label + ': empty ' + field)
                require(bool(card['authority']), label + ': missing authority')
                for ref in card['authority']:
                    require(local(root, ref).is_file(), label + ': missing authority ' + ref)
                require(card['status'] in ('OPEN', 'VERIFIED', 'SUPERSEDED'), label + ': invalid status')
                if card['status'] == 'SUPERSEDED':
                    target = card['supersededBy']
                    require(isinstance(target, str) and bool(target), label + ': invalid successor')
                    if isinstance(target, str):
                        supersessions[card['id']] = target
                require(set(card['requirements']) == set(ids), label + ': incomplete applicability')
                for key, decision in card['requirements'].items():
                    require(decision['decision'] in ('APPLY', 'NA') and bool(decision['reason'].strip()),
                            label + ': invalid applicability ' + key)
                require(bool(card['files']) and len(card['files']) == len(set(card['files'])),
                        label + ': empty/duplicate file scope')
                for file in card['files']:
                    local(root, file)
                if card['status'] == 'VERIFIED':
                    require(bool(card['tests']) and bool(card['evidence']), label + ': evidence absent')
                    for test in card['tests']:
                        require(bool(test['command'].strip()) and type(test['exitCode']) is int
                                and test['exitCode'] == 0 and bool(test['environment'].strip()),
                                label + ': failed/incomplete test')
                    for item in card['evidence']:
                        evidence = local(root, item['path'])
                        require(evidence.is_file() and digest(evidence) == item['sha256'],
                                label + ': stale/missing evidence ' + item['path'])
                    own = path.relative_to(root).as_posix()
                    require(set(card['sourceDigests']) == set(card['files']) - {own},
                            label + ': incomplete source digests')
                    for file, expected_digest in card['sourceDigests'].items():
                        source = local(root, file)
                        require((not source.exists() and expected_digest == 'DELETED') or
                                (source.is_file() and digest(source) == expected_digest),
                                label + ': stale source ' + file)
                    coverage.update(card['files'])
            except (KeyError, TypeError, ValueError, AttributeError, OSError) as exc:
                errors.append(label + ': malformed card: ' + str(exc))
        for start, target in supersessions.items():
            require(target in card_ids, start + ': missing successor card')
            seen, current = set(), start
            while current in supersessions:
                if current in seen:
                    errors.append(start + ': cyclic supersession')
                    break
                seen.add(current)
                current = supersessions[current]
        if changed is not None:
            for path in sorted(set(changed) - coverage):
                errors.append('changed path lacks VERIFIED coverage: ' + path)
    except (KeyError, TypeError, ValueError, AttributeError, OSError) as exc:
        errors.append('invalid governance package: ' + str(exc))
    return {'schema': 'engineering-governance-report.v1',
            'status': 'FAIL' if errors else 'PASS', 'errors': errors,
            'changeCoverage': 'CHECKED' if changed is not None else 'NOT_ASSERTED',
            'runtimeConformance': 'NOT_ASSERTED', 'productionReadiness': 'NOT_ASSERTED'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument('--base')
    parser.add_argument('--head')
    args = parser.parse_args()
    if bool(args.base) != bool(args.head):
        parser.error('--base and --head must be supplied together')
    changed = None
    if args.base:
        def git(*parts):
            return subprocess.check_output(['git', '-C', str(args.root), *parts])
        try:
            base = git('rev-parse', '--verify', args.base + '^{commit}').decode().strip()
            head = git('rev-parse', '--verify', args.head + '^{commit}').decode().strip()
            if git('rev-parse', 'HEAD').decode().strip() != head:
                parser.error('checkout must match --head')
            if git('status', '--porcelain', '--untracked-files=normal').strip():
                parser.error('review-range validation requires a clean checkout')
            changed = [p.decode('utf-8') for p in git('diff', '--no-renames', '--name-only',
                                                     '-z', base, head, '--').split(b'\0') if p]
        except subprocess.CalledProcessError:
            parser.error('cannot resolve review range')
    report = validate(args.root.resolve(), changed)
    print(json.dumps(report, indent=2))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    raise SystemExit(main())
