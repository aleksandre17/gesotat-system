# GeoStat Canonical Directory Tree

```text
gesotat-system/
├── README.md · AGENTS.md · CLAUDE.md · CHANGELOG.md
├── package.json · pnpm-workspace.yaml
├── .gitignore · .gitattributes · .editorconfig
├── agent-framework/kit/                         [EMPTY]
├── .agents/
│   ├── kit/
│   ├── project/{project.json,profile.json,kit.lock.json,roster.json}
│   ├── project/{roles,doctrine}/
│   ├── knowledge/{private,shared}/
│   ├── generated/{roles,skills,runtimes}/
│   └── runtime/{receipts,telemetry,ground,guards}/
├── platform/
│   ├── apps/{REGISTRY.json,geostat/}
│   │   └── geostat/{backend/{settings.gradle,build.gradle,gradlew*,core,api,mobile},frontend/{geostat-system-app,web[LEGACY]}}
│   ├── packages/
│   ├── kits/
│   ├── tools/
│   ├── data/
│   └── e2e/journey/
├── ops/
│   ├── infra/geostat-platform/
│   ├── compose/projects/geostat/
│   │   ├── docker-compose.dev.yml
│   │   ├── docker-compose.prod.yml
│   │   ├── services/{api,mobile,web,control-plane-ui,infra}/
│   │   └── environments/
│   ├── config/{schema,projects/REGISTRY.json}/
│   │   └── projects/geostat/{shared/{templates,secrets},services/{api,mobile,web,control-plane-ui,infra}}/
│   ├── runtime/projects/geostat/{services/{api,mobile,web,control-plane-ui,infra},receipts,telemetry,ground,guards}/
│   ├── cli/{lifecycle,validation,data,portability}/
│   ├── scripts/{shell,powershell,python,java,shared}/
│   ├── tests/
│   └── runbook/
├── docs/{intent,decisions,reference,guides,archive}/
│   └── work/{cards/<id>,evidence/<card>}/
├── samples/{*.accdb,chartjson.dat}/
│   └── contracts/kids-portal-v1-r7-full-contract.json
├── documentation/complete-package/
├── artifacts/recovered/
└── build/                                      [IGNORED]
```
