# Site-serving API contract

## Identity and authorization

A consuming site never receives SQL credentials and never addresses internal database identities. It identifies a public resource by stable codes:

```text
productCode / datasetCode / externalKey
KIDS_PORTAL / KIDS_FILE_RESOURCE / 162
```

`productCode`, `datasetCode`, `metricCode`, `visualizationCode`, and an entity's source-stable `externalKey` are public integration identities. Internal `BIGINT` keys (`product_id`, `entity_id`, `snapshot_id`) are join/implementation keys only and must not be API contract identifiers.

Each site integration uses a machine client (`client_id`) authenticated through OAuth 2.0 client credentials or an API gateway-managed service token. The issued token contains a product scope, for example:

```text
geostat.product.KIDS_PORTAL.read
```

The API resolves the code to its internal product identity and rejects a token without that exact product scope. A generic `READ_RESOURCE` role alone is not the long-term site-isolation boundary.

## Snapshot rule

Every response is read only from the currently `PUBLISHED` publication snapshot for the requested product. No endpoint reads a draft, staging table, legacy source database, superseded snapshot, or `raw.source_record` by default.

The response includes a release/snapshot ETag. A site sends `If-None-Match` on subsequent calls; a rollback changes the selected published snapshot atomically, so the site sees either the old complete release or the new complete release—never a mixture.

## Public endpoint families

```text
GET /platform/products/{productCode}/catalog
GET /platform/products/{productCode}/datasets/{datasetCode}/entities
GET /platform/products/{productCode}/datasets/{datasetCode}/entities/{externalKey}
GET /platform/products/{productCode}/metrics/{metricCode}/observations
GET /platform/products/{productCode}/visualizations/{visualizationCode}
```

Collection endpoints use bounded `limit` and opaque cursor pagination. They return no internal database IDs.

### Non-statistical response

An entity/resource response is a readable typed document assembled from canonical relational tables:

```json
{
  "key": "162",
  "type": "KIDS_FILE_RESOURCE",
  "title": {"ka": "…", "en": "…"},
  "classifications": [{"scheme": "UN_SDG_GOAL", "code": "2"}],
  "locators": [{"kind": "ROUTE_PATH", "language": "ka", "value": "…"}],
  "relationships": [],
  "release": "7"
}
```

- titles/text come from `entity.localized_text`;
- paths/URIs come from `entity.resource_locator`;
- classifiers come from the publication's immutable classification mirror;
- relationships come from `entity.entity_link` only when source evidence proves them.

This is neither a raw-table proxy nor an EAV/key-value dump. A page can render files, goals, glossary entries, translations, categories and navigable resources from one stable API shape.

### Statistical response

Statistics are returned by metric/dimension/period, from `statistics.series`, `statistics.observation`, and `statistics.observation_dimension`. A chart definition returns presentation metadata plus the same published metric observations; it does not own a copied value table.

## Restricted endpoints

Raw payload, artifact URI, validation issue, lineage/source row, draft metadata and archive snapshot access are governance endpoints. They require separate internal scopes and are never exposed to a public site client.
