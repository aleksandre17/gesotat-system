# Universal Metadata Plane

Metadata is governed contract data, distinct from raw payload, canonical facts, statistical observations, and UI code. Every publishable subject (product, page, dataset, metric, classifier, resource, visualization, or distribution) is identified by `subject_type + subject_code + subject_revision` in `platform.metadata_subject`.

## Layers

- `CORE`: localized title, description, keywords, owner and lifecycle.
- `SEMANTIC`: dataset/metric/unit/aggregation/dimension meaning.
- `PRESENTATION`: chart type, axes, legend, formatting and default filters.
- `PROVENANCE`: source, citation, license, steward and update cadence.
- `GOVERNANCE`: quality, privacy, retention, visibility and approval evidence.
- `ACCESSIBILITY`: screen-reader text, captions and alternative descriptions.
- `DISTRIBUTION`: API route, contract revision, snapshot and cache policy.

`platform.metadata_namespace` defines stable namespaces and URI identifiers. `platform.metadata_schema` versions the JSON schema for a namespace. `platform.metadata_assertion` stores typed, optionally localized values (`TEXT`, `NUMBER`, `BOOLEAN`, `JSON`, `URI`, `DATE`, `DATETIME`, `CODE`) and preserves source/hash information. `platform.metadata_relation` connects metadata subjects without coupling them to a specific data family.

Extensions are allowed only in registered namespaces and approved schema revisions. Unknown extensions are retained during ingest but cannot be published until their schema and governance state are approved. Breaking changes create a new schema revision; published revisions remain immutable.

## API rule

Visualization responses contain `metadata` and data separately. A governed metadata subject is preferred; legacy `config_json` is a compatibility fallback and is never treated as SQL. Page metadata may provide defaults, while visualization metadata may override only fields declared by its contract. Public responses exclude restricted governance and raw-source assertions.

## Access rule

The Access package remains a transport artifact. `__gs_*` tables declare contract and projection structure; metadata assertions are materialized through the approved Control Plane contract and never become hidden business logic in Access. An Access extension must declare its namespace, schema revision, property type, language policy, lifecycle and source reference before publication.

## Lifecycle and gates

`DRAFT → REVIEW → APPROVED → PUBLISHED → DEPRECATED/RETIRED`. Publication requires schema validity, relation integrity, provenance, quality, privacy and snapshot consistency. Metadata cannot change the metric's unit, aggregation or observation semantics; those remain controlled by the statistical contract.
