# Environment configuration boundary

`ops/config` is the sole configuration contract for deploys. Keep schemas and
placeholder templates tracked; keep resolved credentials outside Git. Runtime
commands must receive an explicit environment file or secret-provider binding
and must pass preflight validation before starting services.

See `docs/reference/canonical-directory-blueprint.md` for the complete
placement, ownership and rotation rules.
