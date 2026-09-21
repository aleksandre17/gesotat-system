# Platform engineering constitution — version 1

ეს პაკეტი აკონკრეტებს არსებულ [ხარისხის doctrine-ს](../ENGINEERING-QUALITY-DOCTRINE.md). მიზანია ერთი სემანტიკური authority, სრული სისტემური ხედვა და მტკიცებულებით გადაბარება. ახალი სესია არ იწყებს ახალ არქიტექტურას მეხსიერებიდან.

## Authority and reading order

1. Read [canonical tree](../CANONICAL-FULL-TREE.md) first, then the [platform audit](../../platform-capability-and-architecture-audit-2026-09-13.md), existing quality doctrine and this package.
2. Read [architecture](ARCHITECTURE.md), [requirements](REQUIREMENTS.md), [anti-patterns](ANTI-PATTERNS.md), [change protocol](CHANGE-PROTOCOL.md) and [standards](STANDARDS.md). `manifest.json` is the machine index, not a second semantic authority.
3. Read the affected approved contracts, ADRs, current checklist and [architecture improvement register](../../work/ARCHITECTURE-IMPROVEMENT-REGISTER.md). Historical PASS is not current evidence.

Repository authority flows from root engineering policy and doctrine to these operational requirements, approved domain contracts/ADRs, implementation, then generated artifacts. A subordinate document or local agent file MUST NOT silently relax a parent invariant. Explicit user instructions retain their instruction-level precedence; record their architectural consequences. Conflicting approved contracts require resolution in the existing decision register before dependent implementation. Plans, examples, legacy implementations and generated schemas do not override approved semantics.

MUST/MUST NOT are obligations; SHOULD permits a documented reason and consequences; MAY is optional, using BCP 14 terminology. Each requirement below has an ID, owner layer, obligation and acceptance evidence. All MUST requirements apply when their subject is affected. Not-applicable decisions require a reason in the change record, never silent omission. No claim is made that every requirement is already implemented.

## Cardinal invariants

- Preserve the platform's purpose: governed contracts define reusable capabilities across sites, providers and data families. A consumer fixture cannot redefine the platform.
- One semantic concept has one authoritative definition and stable identity. Reuse precedes extension; extension precedes replacement.
- Grammar validation, semantic compilation, policy authorization and runtime execution are separate responsibilities and all must agree.
- Unsupported capability is an explicit rejection or approved evolution, never silent feature loss or a site-specific core branch.
- A change is complete only for its declared scope and evidence. Static policy PASS is not runtime PASS; runtime PASS is not release readiness.

## Enforcement boundary

Run `python ops/cli/validation/engineering-governance.py` and its negative tests with `python -m unittest discover -s ops/tests/governance -p "test_*.py"`. The checker is called by technical acceptance and release gate. It validates policy wiring, referenced files, requirement identifiers and change-record evidence integrity. It does **not** prove architecture correctness, test adequacy, author identity or runtime security. Human review and the existing behavioral/release suites remain mandatory.

For a review range run `python ops/cli/validation/engineering-governance.py --base <review-base> --head <candidate-commit>`. Every changed source path must be covered by a change record. Without a range the report explicitly says coverage NOT_ASSERTED. CI/branch protection must require this range check, negative tests and existing applicable acceptance checks; local files cannot prevent an administrator bypass. Protection configuration is external and is not asserted as installed by this package.

Policy changes require their own change record, negative tests and review of the checker. No blanket waivers: an exception must name requirement, bounded scope, owner, justification, compensating control, expiry and approved ADR; the current checker does not implement waivers. Security, confidentiality and publication gates cannot be bypassed by a documentation exception. Existing debt stays open in the existing AIR register; do not clone a second debt register.
