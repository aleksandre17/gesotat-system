# KIDS frontend environment profile

This is the environment/configuration namespace for the KIDS user-facing site.
The application source remains at `platform/apps/geostat/frontend/kids`; this
directory contains only deployment configuration and the contract binding.

## Runtime contract

The site is bound to `KIDS_PORTAL_V1` revision 8. Page identity is declared in
`service.manifest.json` and must be resolved from Control Plane discovery at
runtime. The frontend must not invent table names or SQL.

## Environment separation

- `templates/app.env.example` is the non-secret variable contract.
- `overlays/` contains environment-specific, non-secret overrides.
- Tokens, client secrets and signing material are runtime secret-manager inputs;
  they must never be committed here.
- Legacy API fallback is disabled in production by default and exists only for
  an explicitly approved migration window.

## Registration flow

1. Register/verify the KIDS site contract and page bindings in Control Plane.
2. Inject the environment overlay and approved secret references.
3. Build the frontend with the immutable release revision.
4. Run discovery, page capability and response-shape acceptance.
5. Publish only after CORS, OIDC and snapshot gates pass.
