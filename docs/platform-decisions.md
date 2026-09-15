# Platform Decisions — დამტკიცებული საწყისი გადაწყვეტილებები

## ADR-001 — Database topology

**გადაწყვეტილება:**

```text
geostat-system  → SQL Server Control Plane
geostat-data    → SQL Server Shared Active Data Plane
geostat-archive → SQL Server Immutable Historical Archive
S3 storage      → source files, exports, large binary artifacts
```

ახალი portal/site არ ქმნის ახალ database-ს. იგი ხდება `data_product` shared `geostat-data` ბაზაში. მხოლოდ სამართლებრივი isolation ან უკიდურესი მოცულობა იძლევა მომავალ shard-ს; shard ინარჩუნებს იმავე schema-ს.

## ADR-002 — External file authority

**გადაწყვეტილება:** ფაილი შეიძლება შეიცავდეს source data-ს და declarative source contract-ს, მაგრამ ვერ მიიღებს თვითონ გადაწყვეტილებას ახალი schema/classifier/metric-ის გამოქვეყნებაზე.

| შემთხვევა | მოქმედება |
|---|---|
| არსებული approved contract | ავტომატური validate → import → policy-ით publish/review |
| ახალი field, code, relation | preview + steward approval |
| breaking schema/unit/grain ცვლილება | ახალი version + explicit approval |
| unknown source | quarantine/catalog, public data არ იცვლება |

ეს უზრუნველყოფს „გარედან შესაძლებელი იყოს“ და ამავდროულად იცავს სისტემას დაუდეკლარირებელი მნიშვნელობის, შეცდომის ან მავნე payload-ისგან.

## ADR-003 — History and archive

`geostat-data` ინახავს აქტიურ published snapshots და მოკლე/საშუალო staging window-ს. `geostat-archive` ინახავს immutable historical snapshots, lineage და audit retention-ს. ორიგინალი Access/CSV/XLSX/JSON binary არასოდეს ინახება SQL Server blob-ად; ინახება S3 storage-ში checksum/version/object-lock policy-ით.

**Retention გადაწყვეტილება:** source artifact, archived data snapshot და quarantine artifact ინახება **1 წელი**. 1 წლის შემდეგ მათი purge კეთდება lifecycle job-ით მხოლოდ მაშინ, როცა არსებობს დამოწმებული checksum/audit trail. პატარა Core metadata — contract, schema version, publication identity, actor და audit event — არ იშლება, რადგან ის აუცილებელია lineage/reproducibility-სთვის.

## ADR-004 — Scale baseline

პლატფორმა იგეგმება მილიონობით row-ზე: background ingestion queue, batch processing, time/product partitioning, typed analytical indexes, metric aggregate cache და read/write workload isolation. საბაზო schedule არის **თვეში ერთხელ**, დამატებით დაშვებულია ხელით დაწყებული import; near-real-time pipeline ახლა არ გვჭირდება.

## ADR-005 — პირველი migration pilot

პირველი end-to-end pilot არის `kids` source. იგი source-only/read-only რჩება; ახალ platform-ში გადადის Access/API ingestion → shared data plane → archive → publication flow-ით.

## S3 bucket layout

```text
geostat-ingest       immutable uploaded source artifacts
geostat-quarantine   invalid/unapproved artifacts; restricted access
geostat-archive      retained package/snapshot exports
geostat-export       generated user exports; expiration policy
```

Key convention:

```text
<product-code>/<dataset-code>/<YYYY>/<MM>/<batch-id>/<checksum>-<original-name>
```

## ADR-006 — Production object storage deployment

**გადაწყვეტილება:** MinIO გაეშვება API container-ისგან განცალკევებულ private storage node/VM-ზე, persistent encrypted volume-ით. S3 API API-service-სთვის ხელმისაწვდომია მხოლოდ private network/TLS-ით; MinIO Console არ გამოდის public ინტერნეტში.

```text
Primary:  dedicated MinIO storage volume/node
Backup:   ყოველდღიური encrypted off-site backup/replication
Access:   service account per application role; least privilege bucket policy
Recovery: ყოველთვიური restore test
```

## ADR-007 — Publication policy

**გადაწყვეტილება:** ნაგულისხმევი მდგომარეობაა `REVIEW_REQUIRED`. დამტკიცებული contract-ის import შეიძლება ავტომატურად გაიაროს catalog, mapping, type, relation და quality validation, მაგრამ public publication მოითხოვს publisher/steward approval-ს. მხოლოდ ცალკე მონიშნულ trusted machine feed-ს შეიძლება ჰქონდეს `AUTO_PUBLISH` policy, მკაცრი quality SLA-ით.
