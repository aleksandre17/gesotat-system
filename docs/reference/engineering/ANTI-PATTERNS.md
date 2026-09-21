# Prohibited patterns and corrective direction

| Pattern | Why it fails | Required direction / requirements |
|---|---|---|
| Copy an existing service/compiler/contract under a new name | Two meanings and divergent fixes | Find owner, reuse or extend; GOV-001, SCH-002 |
| Provider/site/table-name switch inside generic engine | Consumer controls platform architecture | Capability contract plus boundary adapter; SCH-003, ARC-001 |
| Universal JSON/EAV table as unplanned substitute for all families | Loses grain, constraints and query guarantees | Approved typed family model; DAT-001 |
| SQL, expressions, credentials or executable business rules inside caller metadata/Access | Turns data into authority/code | Closed grammar and compiled allowlisted operations; API-001, SEC-002 |
| DTO/DDL/generated file becomes master semantic definition | Reverses schema hierarchy | Change approved source contract and regenerate; SCH-001 |
| Unknown field/capability silently ignored | Hidden functional loss | Version-aware rejection and explicit evolution; SCH-001, UI-001 |
| Green by weakening assertion, disabling test, adding blanket mocks or permissive role | Conceals regression | Reproduce failure, fix owner layer, preserve behavioral proof; TST-001, SEC-001 |
| Annotation/reflection check presented as authorization proof | Does not exercise proxy/filter/policy/data scope | Real denied/allowed execution tests; SEC-001, TST-002 |
| Optional infrastructure silently becomes mandatory at boot | Breaks declared deployment profiles | Explicit capability/profile contract and boot tests; ARC-001, EVO-001 |
| Broad grant to existing users presented as dedicated permission | Changes trust boundary without evidence | Least privilege policy migration and denial tests; SEC-001 |
| Catch-and-continue, success on partial persistence, unbounded retry | Hidden corruption or resource exhaustion | Atomic state transition, bounded retry and reconciliation; DAT-002, OBS-001 |
| Delete legacy flow when new happy path works | Loses unmeasured capabilities and rollback | Capability crosswalk and governed retirement; UI-001, EVO-001 |
| Reuse stale PASS or test counts as evidence | Different source/environment may behave differently | Revision/input-bound reproducible evidence; TST-002, REL-001 |
| Parallel document registries, latest-final-v2 copies, a new handoff each session | Splits authority and causes loops | Update canonical card/register; GOV-001, GOV-003 |
| Framework/pattern proliferation without observed need | More coupling and maintenance without capability | Minimal justified boundary, alternatives and acceptance; ARC-001, GOV-004 |
| Claim production complete after local fixes or documentation | Omits deployment and operational proof | Existing full release invariant; REL-001 |

Detection of an existing violation is not authority to rewrite unrelated subsystems. Record it in the existing AIR register with severity, affected invariant, evidence and bounded next action. Stop dependent work on unresolved correctness/security blockers; continue independent authorized work.
