# ADR-013 — Repository Control Protocol as the project operating system

**Status:** ACCEPTED (2026-09-20) · **Owner:** project owner
**Supersedes:** nothing. **Superseded by:** nothing.

## Context

Architecture recovery (`PHASE-001`) produced a large body of durable knowledge: root
causes, findings, requirements, invariants, an authority registry, layer coverage,
protection requirements and a design handoff. Recovery also established that the
repository's *management* of that knowledge was itself defective, on evidence:

- **Project state lived in conversations.** No file answered *where are we, what is next,
  what is forbidden*. A new agent — human or AI — had to reconstruct it from `git diff`,
  filenames and chat scrollback.
- **Authority was inferable only from names and dates.** Twenty-one documents carried
  `final` / `canonical` / `complete` / `master` in their filenames while none carried a
  status marker (CF-042). "Latest file wins" was the de-facto rule.
- **The same responsibility had several owners.** RC-001, RC-003, CF-033, CF-038 are the
  architectural instances; the documentation plane had its own (see `DEF-03`).
- **The rehabilitation program itself could be silently truncated.** Two separate roadmap
  corrections were needed before every step had its own phase and gate.

These are management failures, not architecture failures, and none of them is fixed by
knowing more about the system.

## Decision

Adopt the **Repository Control Protocol (RCP)** as this repository's project operating
system, under the invariant `ONE FACT → ONE CANONICAL OWNER → MANY REFERENCES`.

1. **Project state lives in files, never in conversation history.** `docs/project/` is the
   control plane; `/AGENTS.md` routes every session into it.
2. **The generic protocol and this project's instance are separate artifacts.**
   `docs/standards/PROJECT-OPERATING-SYSTEM.md` contains zero project facts and is reusable
   by any project; `docs/project/*` contains no generic protocol text. Mixing them is what
   makes a process framework unportable and a project unreadable.
3. **Control artifacts route; they do not hold knowledge.** `CURRENT`, `MANIFEST`,
   `CATALOG`, `ADOPTION` own state, lifecycle, navigation and bootstrap mapping
   respectively — and nothing else. Recovery knowledge keeps its existing canonical owners.
4. **Governance is machine-enforced, not merely written.** `ops/cli/validation/rcp-verify.py`
   decides the protocol's checkable claims; `OBL-RCP-VERIFY` requires it to exit `0` before
   a session closes.
5. **Pre-existing documents are classified, not rewritten.** Authority is assigned in the
   adoption register. Bulk-editing historical files for uniformity destroys evidence and is
   forbidden (`OBL-NO-SILENT-LOSS`).

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Keep working from chat context and `git log` | The failure being fixed. It cannot survive a session boundary, an agent change or a handover, and it silently loses program steps. |
| One "current state of everything" document | Merges state, knowledge, decisions, work, evidence and history into one artifact. It goes stale as a whole, and every reader pays full context cost for any question. |
| Adopt an external framework wholesale | None of the general-purpose ones distinguish *canonical authority* from *supporting representation*, which is the specific failure class this repository has. |
| Enforce the protocol by instruction only | Tried first. The implementation audit found the control plane internally inconsistent while every instruction was being followed — discipline does not detect drift. |
| Move existing documents into a new structure | Navigation correctness outranks folder aesthetics (RCP §20). Moving breaks inbound references and buys nothing that classification does not. |

## Consequences

**Accepted costs.** Every governed artifact carries metadata. Adding a document means
answering who already owns that responsibility (CAD-03). A session that does not update
the control plane is not closed. The validator can fail a change for reasons unrelated to
the code it touches.

**Obtained.** A fresh agent can continue from repository state alone. Authority is
decidable without reading a document's body. A lifecycle step cannot be dropped silently
(`RCP-511`). A second canonical owner for one responsibility fails the build (`RCP-404`).

**Explicitly not obtained.** RCP governs knowledge and process. It certifies nothing about
runtime, production readiness or architectural correctness — a green validator says the
repository is *navigable and self-consistent*, not that the platform works.

## What this decision must never become

A second roadmap · a second authority registry · a second findings register · a second
current-state file · a knowledge base that copies recovery findings out of their owners ·
a set of ceremonial directories. Each of these is the failure RCP exists to prevent, and
adopting RCP is not a licence to recreate it under a new name (CAD-16).

## Evidence

Implementation state, gaps found and closed, validator baseline and the repository-only
continuation proof: `docs/work/cards/repository-control-protocol/governance.json`.
Machine check: `python ops/cli/validation/rcp-verify.py`.
