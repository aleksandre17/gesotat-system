# Project configuration registry

This directory is a multi-project namespace. Each immediate child is one
independent `projectId`; project configuration, overlays and secrets never
cross child boundaries.

```text
projects/
├── REGISTRY.json              canonical project inventory
├── geostat/                   GEOSTAT project only
│   ├── shared/                common project configuration
│   └── services/              api, mobile, web and infra boundaries
└── <other-project>/           future independent project sibling
```

Adding a project requires a new registry entry, its own templates/overlays/
secrets tree, manifest, runtime namespace and isolation acceptance. No project
may be nested inside another project or reuse another project's resolved env.
