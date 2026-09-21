#!/usr/bin/env python3
"""Repository Control Protocol (RCP) validator.

Deterministic, portable, stdlib-only. Implements the checks specified in
docs/standards/PROJECT-OPERATING-SYSTEM.md #19.

Exit 0 = PASS, 1 = FAIL, 2 = usage/internal error.

    python ops/cli/validation/rcp-verify.py [--repo <path>] [--quiet]

It validates the control plane only. It never reads or judges application code.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# ---------------------------------------------------------------- vocabularies
# Closed per RCP #5. Adding a value is a governed change to the standard.
TYPES = {"CONTROL", "ARCHITECTURE", "DECISION", "CONTRACT", "REFERENCE",
         "GUIDE", "WORK", "EVIDENCE", "REGISTER", "REPORT", "HISTORICAL"}
AUTHORITIES = {"CANONICAL", "SUPPORTING", "HISTORICAL", "SUPERSEDED", "MIGRATION_ONLY"}
STATUSES = {"NOT_STARTED", "READY", "ACTIVE", "BLOCKED", "VERIFYING",
            "PASS", "FAIL", "COMPLETE", "SUPERSEDED", "CANCELLED"}
REQUIRED_META = ("id", "type", "title", "status", "authority")

# Filenames that assert currency or authority. These must carry an assigned authority.
#
# Two groups, both learned from real defects in this repository:
#   CF-042 — names asserting finality  (final, canonical, complete, unified, master)
#   CF-043 — names asserting current state or continuation
#            (handoff, status, session, current, state, snapshot)
# The second group was found only after the first had been "closed": four documents each
# claimed to be what a new session reads first, and not one of them contains the word
# "final". Detecting finality alone is not enough.
CANONICAL_LOOKING = re.compile(
    r"(^|[-_])(final|canonical|complete|unified|master"
    r"|handoff|status|session|current|state|snapshot)([-_.]|$)", re.I)

# Soft cohesion limit. A control artifact past this is usually carrying two responsibilities.
COHESION_LINES = 1500

CONTROL_PLANE = Path("docs/project")
STANDARD_SPEC = Path("docs/standards/PROJECT-OPERATING-SYSTEM.md")


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.checks = 0

    def check(self, ok: bool, code: str, message: str, warn: bool = False) -> bool:
        self.checks += 1
        if not ok:
            (self.warnings if warn else self.errors).append(f"[{code}] {message}")
        return ok


def front_matter(path: Path) -> dict[str, str] | None:
    """Minimal YAML front-matter reader: flat `key: value` pairs only."""
    try:
        text = path.read_text(encoding="utf-8")
    except OSError:
        return None
    if not text.startswith("---"):
        return None
    end = text.find("\n---", 3)
    if end == -1:
        return None
    meta: dict[str, str] = {}
    for line in text[3:end].splitlines():
        line = line.strip()
        if not line or line.startswith("#") or ":" not in line:
            continue
        key, _, value = line.partition(":")
        meta[key.strip()] = value.strip()
    return meta


def governed_docs(repo: Path, catalog_text: str) -> list[Path]:
    """Artifacts the protocol governs.

    An artifact becomes governed by being catalogued. That is what makes authority
    structurally decidable: a document cannot be canonical without an explicit CATALOG
    entry. Pre-existing documents are therefore NOT force-migrated — they are classified
    (see check_cf042), which is a register change rather than a bulk file edit.
    """
    out: list[Path] = []
    for rel in (CONTROL_PLANE, STANDARD_SPEC.parent):
        base = repo / rel
        if base.is_dir():
            out.extend(sorted(p for p in base.rglob("*.md") if p.is_file()))
    for ref in sorted(set(re.findall(r"`((?:docs|ops)/[^`\s]+?\.md)`", catalog_text))):
        if "*" in ref:
            continue
        path = repo / ref
        if path.is_file() and path not in out:
            out.append(path)
    return out


def adoption_map(repo: Path) -> dict[str, str]:
    """Authority assigned externally by CTRL-ADOPTION, keyed by repo-relative path.

    A document that predates the protocol carries its metadata in the adoption register
    instead of a front-matter header. Editing those files to add headers would be a bulk
    content change to historical material, which the protocol forbids; the register is the
    sanctioned alternative carrier. A row counts as classification only when it assigns a
    real authority token — a mere mention classifies nothing.
    """
    adoption = repo / CONTROL_PLANE / "ADOPTION.md"
    if not adoption.is_file():
        return {}
    out: dict[str, str] = {}
    for line in adoption.read_text(encoding="utf-8").splitlines():
        if not line.lstrip().startswith("|"):
            continue
        # Read the authority from a table cell, never from the whole row: a path such as
        # `docs/reference/CANONICAL-OBJECT-STORAGE-FLOW.md` contains an authority word in
        # its own name, and a row-wide search would classify it by its filename — the very
        # mistake this register exists to prevent.
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        authority = next((a for c in cells for a in AUTHORITIES
                          if re.fullmatch(rf"\*{{0,2}}{a}\*{{0,2}}", c)), None)
        if not authority:
            continue
        for ref in re.findall(r"`((?:docs|ops|platform)/[^`\s]+?\.\w+)`", line):
            out.setdefault(ref, authority)
        # Repository-root artifacts are named without a directory prefix. Only accept one
        # that actually exists at the root, so a bare filename mentioned in prose cannot
        # invent a governed path.
        for ref in re.findall(r"`([A-Za-z0-9][A-Za-z0-9._-]*\.md)`", line):
            if (repo / ref).is_file():
                out.setdefault(ref, authority)
    return out


def body_of(path: Path) -> str:
    text = path.read_text(encoding="utf-8", errors="replace")
    if text.startswith("---"):
        end = text.find("\n---", 3)
        if end != -1:
            return text[end + 4:]
    return text


# --------------------------------------------------------------------- checks
def check_singletons(repo: Path, r: Report) -> None:
    """Exactly one CURRENT, MANIFEST and CATALOG in the whole repository."""
    for name, code in (("CURRENT.md", "RCP-101"),
                       ("MANIFEST.md", "RCP-102"),
                       ("CATALOG.md", "RCP-103")):
        found = [p for p in repo.rglob(name)
                 if p.is_file() and ".git" not in p.parts and "node_modules" not in p.parts]
        rel = ", ".join(str(p.relative_to(repo)) for p in found) or "none"
        r.check(len(found) == 1, code,
                f"expected exactly one {name}, found {len(found)}: {rel}")


def check_metadata(docs: list[Path], repo: Path, adopted: dict[str, str],
                   r: Report) -> dict[str, Path]:
    """Every governed artifact carries valid metadata; IDs are unique."""
    ids: dict[str, Path] = {}
    for path in docs:
        rel = path.relative_to(repo)
        posix = str(rel).replace("\\", "/")
        meta = front_matter(path)
        if meta is None and posix in adopted:
            continue  # metadata carried by the adoption register instead
        if not r.check(meta is not None, "RCP-201",
                       f"{rel}: no metadata — needs YAML front matter or an ADOPTION row"):
            continue
        assert meta is not None
        for field in REQUIRED_META:
            r.check(field in meta, "RCP-202", f"{rel}: missing required field '{field}'")
        if "type" in meta:
            r.check(meta["type"] in TYPES, "RCP-203",
                    f"{rel}: type '{meta['type']}' not in the closed vocabulary")
        if "authority" in meta:
            r.check(meta["authority"] in AUTHORITIES, "RCP-204",
                    f"{rel}: authority '{meta['authority']}' not in the closed vocabulary")
        doc_id = meta.get("id")
        if doc_id:
            if doc_id in ids:
                r.check(False, "RCP-205",
                        f"duplicate id '{doc_id}': {rel} and {ids[doc_id].relative_to(repo)}")
            else:
                ids[doc_id] = path
    return ids


def check_supersession(docs: list[Path], ids: dict[str, Path], repo: Path, r: Report) -> None:
    """superseded_by / supersedes must resolve, and the graph must be acyclic."""
    edges: dict[str, str] = {}
    for path in docs:
        meta = front_matter(path) or {}
        rel = path.relative_to(repo)
        target = meta.get("superseded_by")
        if target and target.lower() not in ("none", "~", ""):
            if r.check(target in ids, "RCP-301",
                       f"{rel}: superseded_by '{target}' does not resolve to a governed id"):
                edges[meta.get("id", str(rel))] = target
        back = meta.get("supersedes")
        if back and back.lower() not in ("none", "~", ""):
            r.check(back in ids, "RCP-302",
                    f"{rel}: supersedes '{back}' does not resolve to a governed id")
        if meta.get("authority") == "SUPERSEDED":
            r.check(bool(target), "RCP-303",
                    f"{rel}: authority SUPERSEDED requires a superseded_by target")
    for start in list(edges):
        seen, node = set(), start
        while node in edges:
            if node in seen:
                r.check(False, "RCP-304", f"supersession cycle involving '{start}'")
                break
            seen.add(node)
            node = edges[node]


def check_catalog(repo: Path, docs: list[Path], adopted: dict[str, str], r: Report) -> str:
    """Catalog references resolve; every canonical artifact is catalogued."""
    catalog = repo / CONTROL_PLANE / "CATALOG.md"
    if not r.check(catalog.is_file(), "RCP-401", "docs/project/CATALOG.md is missing"):
        return ""
    text = catalog.read_text(encoding="utf-8")
    for ref in sorted(set(re.findall(r"`((?:docs|ops|platform)/[^`\s]+?\.(?:md|py|json|sh))`", text))):
        if "*" in ref:
            continue  # a documented glob is a group reference, not a path
        r.check((repo / ref).exists(), "RCP-402", f"CATALOG references a missing path: {ref}")
    for path in docs:
        rel = str(path.relative_to(repo)).replace("\\", "/")
        if rel.startswith("docs/project/work/"):
            continue  # work items are indexed by the work register, not the catalog
        if path == catalog:
            continue  # the catalog lists itself as "this file"
        authority = (front_matter(path) or {}).get("authority") or adopted.get(rel)
        if authority == "CANONICAL":
            r.check(rel in text or path.name in text, "RCP-403",
                    f"CANONICAL artifact not listed in CATALOG: {rel}")

    # A document the register calls CANONICAL must be navigable, whether or not it is
    # otherwise governed. Authority without navigation is authority nobody can reach.
    catalogued = {str(p.relative_to(repo)).replace("\\", "/") for p in docs}
    for rel, authority in sorted(adopted.items()):
        if authority != "CANONICAL" or rel in catalogued:
            continue
        if "*" in rel or "{" in rel or "<" in rel:
            continue
        r.check(rel in text or Path(rel).name in text, "RCP-403",
                f"ADOPTION marks this CANONICAL but CATALOG does not route to it: {rel}")

    # RCP-404 — no duplicate canonical ownership (CAD-01, RCP #19).
    # The catalogue's `Owns` cell is the declared responsibility. Two CANONICAL rows
    # declaring the same one is the parallel-authority failure in its earliest, cheapest
    # form: before any code exists, while it is still only two documents.
    owners: dict[str, str] = {}
    for line in text.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) != 6 or cells[3] != "CANONICAL":
            continue
        owns = re.sub(r"[^a-z0-9 ]", " ", cells[4].lower())
        owns = " ".join(owns.split())
        if not owns:
            continue
        if owns in owners:
            r.check(False, "RCP-404",
                    f"duplicate canonical ownership of '{cells[4]}': "
                    f"{cells[0]} and {owners[owns]}")
        else:
            owners[owns] = cells[0]
    return text


def check_current_manifest(repo: Path, r: Report) -> None:
    """CURRENT must agree with MANIFEST: gates exist, statuses are legal."""
    cur = repo / CONTROL_PLANE / "CURRENT.md"
    man = repo / CONTROL_PLANE / "MANIFEST.md"
    if not (r.check(cur.is_file(), "RCP-501", "CURRENT.md is missing")
            and r.check(man.is_file(), "RCP-502", "MANIFEST.md is missing")):
        return
    cur_text, man_text = body_of(cur), body_of(man)

    gates = set(re.findall(r"`(GATE-[A-Z0-9-]+)`", man_text))
    r.check(bool(gates), "RCP-503", "MANIFEST declares no gates")
    for gate in sorted(set(re.findall(r"`(GATE-[A-Z0-9-]+)`", cur_text))):
        r.check(gate in gates, "RCP-504",
                f"CURRENT references '{gate}' which MANIFEST does not declare")

    phases = set(re.findall(r"`?(PHASE-\d{3})`?", man_text))
    for phase in sorted(set(re.findall(r"`(PHASE-\d{3})`", cur_text))):
        r.check(phase in phases, "RCP-505",
                f"CURRENT references '{phase}' which MANIFEST does not declare")

    for status in re.findall(r"\*\*STATUS\*\*\s*\|\s*`?([A-Z_]+)`?", cur_text):
        r.check(status in STATUSES, "RCP-506",
                f"CURRENT status '{status}' not in the closed vocabulary")

    # Every required-read path must exist: a broken read set silently under-contexts an agent.
    reads = re.findall(r"`((?:docs|ops)/[^`\s]+?\.md)`", cur_text)
    r.check(bool(reads), "RCP-507", "CURRENT declares no required read set")
    for ref in sorted(set(reads)):
        r.check((repo / ref).exists(), "RCP-508",
                f"CURRENT required-read path does not exist: {ref}")

    # A superseded artifact must never be mandatory reading.
    for ref in sorted(set(reads)):
        meta = front_matter(repo / ref) or {}
        r.check(meta.get("authority") != "SUPERSEDED", "RCP-509",
                f"CURRENT requires reading a SUPERSEDED artifact: {ref}")

    # RCP-511 — roadmap integrity. Phases must be contiguous from 001 with exactly one
    # terminal phase. This exists because the same defect occurred twice: a lifecycle step
    # survived as vocabulary while losing its own phase and gate. A dropped step leaves a
    # gap; an absorbed one removes the highest ID. Both now fail here.
    declared = sorted(int(p[6:]) for p in phases)
    if r.check(bool(declared), "RCP-511", "MANIFEST declares no phases"):
        expected = list(range(1, len(declared) + 1))
        r.check(declared == expected, "RCP-511",
                f"phase IDs are not contiguous from 001: {declared}")
        terminal = re.findall(r"`?(PHASE-\d{3})`?[^\n|]*\|[^\n|]*\|[^\n|]*\|[^\n]*"
                              r"terminal state", man_text)
        r.check(len(terminal) == 1, "RCP-511",
                f"expected exactly one terminal phase, found {len(terminal)}")

    # RCP-512 — every declared phase needs an entry gate, or it can be entered by
    # assertion. Gate count is the cheapest proxy: fewer gates than phases means at least
    # one phase is ungoverned.
    r.check(len(gates) >= len(declared), "RCP-512",
            f"{len(declared)} phases but only {len(gates)} gates — "
            f"at least one phase has no entry gate")

    # RCP-510 — hidden blockers. A blocked project that reads as unblocked is worse than
    # one that reads as blocked, because nobody goes looking for the cause.
    blockers = re.search(r"^##+\s*Blockers\s*$(.*?)(?=^##\s|\Z)",
                         cur_text, re.M | re.S)
    if r.check(blockers is not None, "RCP-510", "CURRENT has no Blockers section"):
        assert blockers is not None
        declared_none = re.search(r"\bnone\b", blockers.group(1), re.I) is not None
        if "BLOCKED" in re.findall(r"\*\*STATUS\*\*\s*\|\s*`?([A-Z_]+)`?", cur_text):
            r.check(not declared_none, "RCP-510",
                    "CURRENT status is BLOCKED but the Blockers section names none")


def check_work(repo: Path, r: Report) -> None:
    """Work items carry the mandatory schema; completed work is not active."""
    work = repo / CONTROL_PLANE / "work"
    if not work.is_dir():
        return  # no work yet: lazy instantiation is legal (RCP #9)
    for state in ("active", "blocked", "completed"):
        folder = work / state
        if not folder.is_dir():
            continue
        for item in sorted(folder.glob("*.md")):
            if item.name.startswith("_") or item.name == "README.md":
                continue
            rel = item.relative_to(repo)
            meta = front_matter(item) or {}
            status = meta.get("status", "")
            r.check(status in STATUSES, "RCP-601",
                    f"{rel}: status '{status}' not in the closed vocabulary")
            if state == "active":
                r.check(status not in ("COMPLETE", "CANCELLED", "SUPERSEDED"), "RCP-602",
                        f"{rel}: status '{status}' but the item sits in work/active")
            if state == "completed":
                r.check(status in ("COMPLETE", "CANCELLED", "SUPERSEDED"), "RCP-603",
                        f"{rel}: status '{status}' but the item sits in work/completed")
            body = body_of(item)
            r.check("required_context" in body or "required_context" in meta, "RCP-604",
                    f"{rel}: no required_context declared")
            for ref in sorted(set(re.findall(r"`((?:docs|ops|platform)/[^`\s]+?\.\w+)`", body))):
                if not r.check((repo / ref).exists(), "RCP-605",
                               f"{rel}: references a missing path: {ref}"):
                    continue
                # A superseded artifact must never be loaded as working context.
                if (front_matter(repo / ref) or {}).get("authority") == "SUPERSEDED":
                    r.check(False, "RCP-607",
                            f"{rel}: required_context includes a SUPERSEDED artifact: {ref}")
            for dep in sorted(set(re.findall(r"`(TASK-\d+)`", body))):
                found = any((work / s).is_dir() and list((work / s).glob(f"{dep}*.md"))
                            for s in ("active", "blocked", "completed"))
                r.check(found, "RCP-606", f"{rel}: dependency '{dep}' does not exist")


def check_cf042(repo: Path, adopted: dict[str, str], r: Report) -> None:
    """CF-042 class: a currency-asserting filename must carry an assigned authority.

    Being mentioned somewhere is not classification. The hazard is a document whose *name*
    reads as current while its authority is undeclared, so the only thing that clears it is
    an explicit authority — in the file's own header, or in the adoption register.
    """
    docs_root = repo / "docs"
    if not docs_root.is_dir():
        return
    # Repository-root markdown is in scope too: a transcript or handoff sitting at the root
    # is the most visible thing in the repository, and CF-044 was exactly that.
    candidates = sorted(docs_root.rglob("*.md")) + sorted(repo.glob("*.md"))
    for path in candidates:
        rel = str(path.relative_to(repo)).replace("\\", "/")
        if rel.startswith("docs/project/") or rel.startswith("docs/standards/"):
            continue
        if not CANONICAL_LOOKING.search(path.stem):
            continue
        own = (front_matter(path) or {}).get("authority")
        assigned = own if own in AUTHORITIES else adopted.get(rel)
        r.check(bool(assigned), "RCP-701",
                f"currency-asserting filename has no assigned authority "
                f"(CF-042 class): {rel}")

    # An adoption row pointing at nothing silently stops governing its document.
    for ref in sorted(adopted):
        if "*" in ref or "{" in ref or "<" in ref:
            continue  # documented group reference, not a path
        r.check((repo / ref).exists(), "RCP-702",
                f"ADOPTION classifies a path that does not exist: {ref}")

    # RCP-703 — orphan decisions. A decision nobody can find is a decision that gets made
    # again, differently. Every decision record must carry an authority.
    decisions = repo / "docs" / "decisions"
    register = repo / "docs" / "platform-decisions.md"
    register_text = register.read_text(encoding="utf-8") if register.is_file() else ""
    if decisions.is_dir():
        for path in sorted(decisions.rglob("*.md")):
            rel = str(path.relative_to(repo)).replace("\\", "/")
            own = (front_matter(path) or {}).get("authority")
            r.check(own in AUTHORITIES or rel in adopted, "RCP-703",
                    f"decision record has no assigned authority (orphan decision): {rel}")

            # RCP-704 — the decision index must be complete. A register that stops at an
            # older identifier tells a reader the later decisions do not exist, which is
            # worse than having no register: it answers the question, wrongly.
            if register_text:
                r.check(rel in register_text, "RCP-704",
                        f"decision is missing from the decision register: {rel}")


def check_cohesion(repo: Path, docs: list[Path], r: Report) -> None:
    """RCP-801 — oversized artifacts. A warning, never an error.

    Length is a cohesion signal, not a defect: an evidence trail is legitimately long.
    Failing the build on it would push authors to split documents for the validator's
    benefit rather than the reader's, which is the opposite of the goal.
    """
    for path in docs:
        lines = path.read_text(encoding="utf-8", errors="replace").count("\n") + 1
        r.check(lines <= COHESION_LINES, "RCP-801",
                f"{path.relative_to(repo)}: {lines} lines — review cohesion "
                f"(soft limit {COHESION_LINES})", warn=True)


def verify(repo: Path) -> tuple[Report, int]:
    """Run every check against `repo`. Returns the report and the governed-artifact count."""
    r = Report()
    catalog_file = repo / CONTROL_PLANE / "CATALOG.md"
    catalog_seed = catalog_file.read_text(encoding="utf-8") if catalog_file.is_file() else ""
    docs = governed_docs(repo, catalog_seed)
    adopted = adoption_map(repo)
    check_singletons(repo, r)
    ids = check_metadata(docs, repo, adopted, r)
    check_supersession(docs, ids, repo, r)
    check_catalog(repo, docs, adopted, r)
    check_current_manifest(repo, r)
    check_work(repo, r)
    check_cf042(repo, adopted, r)
    check_cohesion(repo, docs, r)
    return r, len(docs)


def main() -> int:
    ap = argparse.ArgumentParser(description="Validate the Repository Control Protocol.")
    ap.add_argument("--repo", default=".", help="repository root (default: cwd)")
    ap.add_argument("--quiet", action="store_true", help="print only the verdict")
    args = ap.parse_args()

    repo = Path(args.repo).resolve()
    if not (repo / CONTROL_PLANE).is_dir():
        print(f"RCP FAIL: no control plane at {CONTROL_PLANE}", file=sys.stderr)
        return 2

    r, governed = verify(repo)

    if not args.quiet:
        for w in r.warnings:
            print(f"WARN  {w}")
        for e in r.errors:
            print(f"ERROR {e}")
    verdict = "PASS" if not r.errors else "FAIL"
    print(f"RCP {verdict}: {r.checks} checks, {len(r.errors)} errors, "
          f"{len(r.warnings)} warnings, {governed} governed artifacts")
    return 0 if not r.errors else 1


if __name__ == "__main__":
    sys.exit(main())
