# საერთო სტატისტიკური კონტრაქტი — დიზაინი და განხორციელების გეგმა

თარიღი: 2026-09-19  
სტატუსი: **მიმართულება შეთანხმებულია; დეტალური დიზაინი შემოთავაზებულია; იმპლემენტაცია NOT READY.**  
იმპლემენტაცია: [checklist](STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md) — increment 1 (compiler core) DONE test evidence-ით; release NOT READY.

## 1. მიზანი და შეთანხმება

მომხმარებლის მოთხოვნაა ყველა პროექტისთვის საერთო სტატისტიკური კონტრაქტი: ნეიტრალური სახელები, მხოლოდ დასაბუთებული სვეტები, მრავალი განზომილება და საზომი, მარტივი შევსება. აქ დაფიქსირებულია წინა განხილვაც და ამ მოთხოვნით დამატებული მიმართულება.

საერთოა კონტრაქტის ენა, სემანტიკა, ვალიდაცია და დამუშავების მექანიზმი. კონკრეტული თემის განზომილებები და საზომები აღიწერება versioned სტრუქტურით. პროექტის სახელები საერთო ცხრილების/სვეტების სახელებში არ შედის. დომენური სახელები, როგორიცაა REGION ან AGE_GROUP, დასაშვებია კონკრეტული სტრუქტურის კომპონენტებად; მათი ჩანაცვლება dimension_1-ით მნიშვნელობას დაკარგავს.

ეს ცვლილება მოიცავს სტატისტიკურ authoring/ingestion პროფილს და მის კავშირს canonical მოდელთან. არსებული ფაილების, ბაზების, API-ების ან გამოქვეყნებული snapshot-ების ცვლილება ამ დოკუმენტით შესრულებულად არ ითვლება. ყველა ანალიტიკური ამოცანის ერთ უნივერსალურ JSON/EAV ცხრილში მოქცევა არ არის სამიზნე. მიკრომონაცემებს, გეოსივრცით მონაცემებსა და სხვა სპეციალურ ოჯახებს შესაბამისი დამატებითი პროფილი და ტესტები სჭირდება.

## 2. საწყისი მდგომარეობა და რისკი

წაკითხული წყაროები:

- [Directory authority](../reference/CANONICAL-FULL-TREE.md)
- [Architecture audit](../platform-capability-and-architecture-audit-2026-09-13.md)
- [Engineering doctrine](../reference/ENGINEERING-QUALITY-DOCTRINE.md)
- [Access / Control Plane responsibility](../access-and-control-plane-responsibility-model.md)
- [Access package-ის ორგანიზება](ACCESS-PACKAGE-ORGANIZATION-PLAN.md)
- [არსებული Access გენერატორი](../../ops/scripts/java/KidsPortalCanonicalAccessPackageGenerator.java)
- [საწყისი Data Plane სქემა](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/002_data_plane.sql)
- [არსებული statistical DSD/component declarations](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/022_kids_complete_site_contract.sql)

გენერატორის სტატისტიკური input ემსახურება legacy chartdata-ს ამოღებას: carrier, cell ordinal, raw period/dimension, lexical value, JSON path და encoding. ეს კარგი lineage ინფორმაციაა, მაგრამ მოუხერხებელი ხელით შესავსები ზედაპირი. metric მიბმულია source resource-ზე. value_decimal ფიზიკურად DOUBLE-ია; ზუსტი decimal round-trip ჯერ დასამტკიცებელია.

ეს არის source review; მიმდინარე ACCDB-ის სრული ინვენტარიზაცია და ყველა შემდგომი migration-ის/runtime შესაძლებლობის აუდიტი განხორციელების პირველი ეტაპია. არსებული observation/component ცხრილების არსებობა არ ამტკიცებს სრულ multi-measure მხარდაჭერას. მონაცემის ან lineage-ის დაკარგვა, grain-ის შეცვლა, სიზუსტის დაკარგვა და ძველი consumer-ის გატეხვა ძირითადი რისკებია.

## 3. საერთაშორისო მოდელი და ჩვენი პროფილი

სემანტიკური ორიენტირია SDMX 3.1 Information Model. ოფიციალური სპეციფიკაციების მიხედვით 3.1 გამოქვეყნდა 2025 წლის მაისში. ეს არის ჩვენი პროფილის საფუძველი და არა უკვე მიღწეული სრული შესაბამისობის განცხადება. [SDMX specifications](https://sdmx.org/standards-2/)

Dimension განსაზღვრავს დაკვირვების იდენტობას; Measure — გაზომილ მნიშვნელობას; Attribute — ინტერპრეტაციის დამატებით ინფორმაციას; Codelist — დასაშვებ კოდებს; DSD — სტრუქტურას. SDMX 3.0-მ შემოიტანა რამდენიმე საზომის მხარდაჭერა; 2.1-ში ასეთი მონაცემის ექსპორტს შესაძლოა გარდაქმნა ან შეზღუდვა დასჭირდეს. [ოფიციალური ცვლილებები](https://sdmx.org/wp-content/uploads/SDMX_3-0-0_Major_Changes_FINAL-1_0.pdf)

შიდა ცხრილების ქვემოთ შეთავაზებული სახელები ჩვენი დიზაინია; SDMX ამ SQL/Access სახელებს არ გვავალდებულებს. საჭიროა დამოუკიდებელი conformance matrix: ინფორმაციული მოდელი, მხარდაჭერილი ტიპები, attachment დონეები, exchange ფორმატი/ვერსია და lossless round-trip. დაუმუშავებელი შესაძლებლობა წინასწარ უნდა უარვყოთ გასაგები შეცდომით.

## 4. პასუხისმგებლობები და სრული ჯაჭვი

| Layer | პასუხისმგებლობა |
|---|---|
| Control | დამტკიცებული სტრუქტურები, კომპონენტები, კლასიფიკატორები, ერთეულები, წესები და ვერსიები |
| Delivery | კონტრაქტიდან Access-ის typed ცხრილები, captions, ფორმები და არჩევანის სიები |
| Ingestion | სქემის/კოდის/ტიპის შემოწმება, იდემპოტენტური მიღება და lineage |
| Data | canonical დაკვირვებები და measure identity; provider-ისგან დამოუკიდებელი სემანტიკა |
| Archive | უცვლელი არტეფაქტები და აღდგენის წყარო |
| Serving | snapshot-ზე მიბმული API, ექსპორტი და query-cost კონტროლი |
| Security | tenant/product scope, მოქმედების უფლებები და კონფიდენციალურობა |
| Observability | validation შედეგები, counters, trace, reconciliation და audit |

ჯაჭვი: approved contract → generated template → filled artifact → preview → ingestion → canonical observations → reconciliation/privacy gates → approved snapshot → API/export.

Access შეიცავს approved კონტრაქტის გადასატან ასლს. ხელით შეცვლილი metadata ახალი authority ვერ ხდება. საერთო აბსტრაქცია tenant-ებს შორის მონაცემის გაზიარების ნებართვას არ ნიშნავს. აქ tenant policy-ის ღია გადაწყვეტილება არ წყდება.

## 5. აუცილებელი სვეტის წესი

სვეტი რჩება მხოლოდ მაშინ, თუ ის არის: ბიზნესგასაღების ნაწილი, გაზომილი მნიშვნელობა, აუცილებელი ინტერპრეტაცია, versioned კავშირის ნაწილი ან დასაბუთებული ავტომატური lineage. თითოეულს აქვს ერთი owner, ტიპი, nullability და validation.

- ერთი მნიშვნელობა ერთ authority-ში განისაზღვრება; package-ის ასლი immutable revision-ს ემთხვევა.
- მუდმივი dataset/structure metadata ყოველ observation-ში არ მეორდება.
- ტექნიკური სვეტი არ ხდება ხელით შესავსები.
- optional ცხრილი/სვეტი იქმნება მხოლოდ პროფილის მოთხოვნით; ასობით ცარიელი წინასწარი ველი არ იგენერირება.
- კონფიგურაციის metadata არ არის თავისუფალი executable SQL, script ან შეუზღუდავი JSON.
- უფრო ნაკლები სვეტი თავისთავად მიზანი არ არის: ერთეულს, სტატუსს, იდენტობასა და lineage-ს ვერ წავშლით საჭიროების მიუხედავად.

## 6. სამიზნე ლოგიკური კატალოგი

ქვემოთ არის **შემოთავაზებული ლოგიკური** ცხრილები/პროექციები. არსებული Control registry გამოიყენება ხელახლა; იდენტური მნიშვნელობის მეორე registry არ იქმნება. ფიზიკური mapping და metadata schema implementation-მდე უნდა დამტკიცდეს. `ref` ნიშნავს ზუსტად ერთ immutable ვერსიაზე resolving მითითებას; `latest` დაუშვებელია. საერთო namespace/agency, code, version registry-შია და თითოეულ reference-row-ში არ მეორდება.

| ობიექტი / შეთავაზებული სახელი | მინიმალური ველები | პირობითი ველები და წესები |
|---|---|---|
| Structure / `__stat_structure` | `structure_ref` | შინაარსი component rows-ით; იდენტობის სამეული საერთო registry-ში |
| Component / `__stat_component` | `structure_ref`, `component_code`, `role`, `concept_ref`, `representation_ref`, `required`, `position` | role მხოლოდ დამტკიცებული enum; უნიკალური structure+code და structure+position |
| Representation / არსებული ტიპების registry-ის პროექცია | `representation_ref`, `logical_type` | `codelist_ref`, `precision`, `scale`, `max_length`, `format_ref` მხოლოდ შესაბამისი ტიპისთვის; closed schema |
| Measure definition / `__stat_measure` | `measure_ref`, `concept_ref`, `representation_ref` | `unit_ref` რაოდენობრივი საზომისთვის; საზომის გადამოწმებადი სემანტიკა |
| Component–measure / `__stat_measure_binding` | `structure_ref`, `component_code`, `measure_ref` | component role უნდა იყოს MEASURE; representation/concept შესაბამისობა სავალდებულოა და authority measure registry-ში რჩება |
| Unit / `__stat_unit` | `unit_ref`, `quantity_kind_ref` | `base_unit_ref`, `scale_factor`, `offset` მხოლოდ conversion-ისთვის; ზუსტი decimal და acyclic conversion graph |
| Attribute attachment / `__stat_attribute_attachment` | `structure_ref`, `attribute_code`, `attachment_ref` | attachment-ის versioned აღწერა განსაზღვრავს dataset-ს, dimension subset-ს, observation-ს ან კონკრეტულ measure-ს |
| Rule binding / არსებული policy registry-ის პროექცია | `structure_ref`, `rule_ref` | კონკრეტულ component-ზე `component_code`; თავისუფალი executable expression არ მიიღება |
| Dataset binding / არსებული `__gs_*`-ის გაფართოება | `dataset_code`, `structure_ref` | dataset იდენტობა ერთხელ არსებობს; ახალი პარალელური dataset registry არ იქმნება |

ეს კატალოგი მთლიანად მომხმარებლის შესავსები არ არის. მინიმალური პროფილი მხოლოდ გამოყენებულ განსაზღვრებებს ატარებს. Structure ref-ის ერთი სვეტის ფიზიკური ცხრილი სავალდებულო არ არის: თუ არსებული registry სრულად ფარავს მის ownership-ს, გამოიყენება მისი პროექცია.

Component-ის role არის DIMENSION, MEASURE ან ATTRIBUTE. განზომილებების დალაგებული ნაკრები ქმნის observation key-ს; ტექნიკური position არ არის ბიზნესიდენტობა. TIME_PERIOD მხოლოდ საჭირო სტრუქტურებში მონაწილეობს. concept აღწერს მნიშვნელობას, representation — ფორმას; სხვადასხვა პროექტის ერთი და იგივე concept შეიძლება სხვადასხვა დამტკიცებული representation-ით გამოიყენებოდეს.

Measure-binding-ში განმეორებითი აღწერა არ არის ახალი authority: component-ის resolved concept/representation გენერირებული reference-ია და measure definition-ს უნდა ემთხვეოდეს. ალტერნატივაა serialization-ში მათი მხოლოდ ერთხელ შენახვა; საბოლოო grammar-მა ერთ-ერთი გზა უნდა დააფიქსიროს.

Unit-ის თავისუფალი denominator_text მარტო ვერ განსაზღვრავს თანაფარდობის მნიშვნელობას. denominator population/concept და base-period მეთოდოლოგია ეკუთვნის measure definition-ის typed, versioned metadata-ს. display label და თარგმანები საერთო localized metadata-დან მოდის; ფიქსირებული title_ka/title_en სვეტები საერთო ახალ კონტრაქტში აღარ ემატება.

## 7. შესავსები მონაცემის ფიზიკური ფორმა

თითო დამტკიცებულ observation structure-ზე გენერირდება typed source table; რამდენიმე dataset შეიძლება იმავე DSD-ს იყენებდეს. source table name კონტრაქტში რეგისტრირდება, collision-safe და provider-ის naming წესებთან თავსებადია. ეს **არ გულისხმობს სერვერზე თითო პროექტისთვის ახალი business table-ის შექმნას**.

| სვეტის სახეობა | რა შედის |
|---|---|
| Dimensions | მხოლოდ DSD-ში გამოცხადებული dimension კომპონენტები |
| Measures | მხოლოდ DSD-ში გამოცხადებული measure კომპონენტები |
| Attributes | მხოლოდ observation/measure დონეზე ცვალებადი, გამოცხადებული ატრიბუტები |
| Technical row reference | ავტომატური `row_ref`, მხოლოდ როცა mutation/lineage mapping-ს სტაბილური source key დამატებით სჭირდება |

მაგალითი, არა core-code-ში ჩასაწერი ფიქსირებული სქემა:

| TIME_PERIOD | REGION | SEX | AGE_GROUP | EMPLOYED | UNEMPLOYED |
|---|---|---|---|---:|---:|
| 2025 | GE-TB | F | Y25T34 | 12000 | 1800 |

Access captions ქართულად აჩვენებს წელს, რეგიონს, სქესს, ასაკობრივ ჯგუფს, დასაქმებულებს და უმუშევრებს. სიაში ჩანს label, ინახება stable code. row grain არის ოთხი dimension-ის კომბინაცია; ორი measure ერთ grain-ზეა. measure identity არ იკარგება canonical გარდაქმნისას.

ფორმის სათაურში ერთხელ შერჩეული მუდმივი dimension შეიძლება sparse authoring-ში არ მეორდებოდეს, მაგრამ საჭიროა explicit contract binding და export/ingestion-ის დეტერმინისტული გაფართოება. ამ binding-ის გარეშე მისი სვეტი რჩება აუცილებელი.

სტატუსის საჭიროებისას იქმნება მხოლოდ გამოცხადებული attribute field და მისი attachment, მაგალითად კონკრეტული measure-ის სტატუსი. null, zero, not available, not applicable და suppressed არ ერთიანდება. ნაგულისხმევი public სტატუსი არ უნდა გამოიგონოს გენერატორმა.

ფართო ცხრილის provider-limit-ის გადაჭრის გზა იქნება კონტრაქტით გამოცხადებული key-preserving დაყოფა ან სხვა exchange adapter. `dimension_1…dimension_10`/`measure_1…measure_5` ხელოვნური ზღვარი საერთო მოდელში არ შედის. ფიზიკური ლიმიტები და რესურსული ბიუჯეტები capability negotiation-ში ცხადად გამოცხადდება.

## 8. არსებული ცხრილები — სრული ცვლილების რუკა

### 8.1 `__stat_unit`

| არსებული სვეტი | სამიზნე |
|---|---|
| `unit_code` | versioned `unit_ref`-ზე explicit crosswalk |
| `quantity_kind` | controlled `quantity_kind_ref` |
| `scale_factor` | რჩება მხოლოდ განსაზღვრული base-unit conversion-ისთვის; exact numeric |
| `denominator_text` | measure methodology metadata; საჭიროებისას ორიგინალი lineage-ში |
| `title` | საერთო localized metadata |

### 8.2 `__stat_metric`

| არსებული სვეტი | სამიზნე |
|---|---|
| `metric_code` | `measure_ref` crosswalk; სახელის მსგავსება semantic equivalence არ არის |
| `source_resource_id` | ცალკე source/measure lineage relation |
| `title_ka`, `title_en` | localized metadata language tag-ით |
| `measure_component_code` | structure component/measure binding |
| `unit_code` | measure-ის დამტკიცებული unit reference |
| `aggregation` | versioned rule binding; განზომილების მიხედვით საჭიროებისამებრ |
| `lifecycle_state` | Control lifecycle; raw observation-ში არ მეორდება |
| `inference_confidence`, `inference_rationale` | migration/inference evidence; ახალ ხელით შეტანილ measure-ს არ მოეთხოვება |

### 8.3 `__raw_kids_statistical_carrier`

| არსებული სვეტი | სამიზნე |
|---|---|
| `carrier_code` | source envelope reference |
| `source_resource_id` | existing resource relation |
| `payload_checksum` | immutable artifact/payload evidence |
| `parse_status` | ingestion evidence |
| `source_row_key` | lineage reference |
| `operation` | versioned ingestion operation envelope |

არსებული raw carrier ინახება legacy replay-ისთვის. ახალ generic authoring-ს carrier-ის ხელოვნური შექმნა არ მოეთხოვება. ახალი envelope გამოიყენებს არსებულ artifact/lineage კონტრაქტს; მეორე raw registry არ იქმნება.

### 8.4 `__stat_kids_statistical_input`

| არსებული სვეტი | სამიზნე |
|---|---|
| `input_key` | legacy crosswalk და საჭირო ავტომატური row reference |
| `carrier_code` | lineage, არა მომხმარებლის dimension |
| `cell_ordinal` | source evidence; არ განსაზღვრავს canonical grain-ს |
| `period_raw` | ორიგინალი lineage-ში |
| `period_normalized` | გამოცხადებული time component-ის typed მნიშვნელობა |
| `dimension_key_raw` | ორიგინალი lineage-ში |
| `age_group_item_ref` | AGE_GROUP მხოლოდ იმ DSD-ში, რომელსაც სჭირდება; classifier version pinned |
| `value_lexical` | raw lexical evidence |
| `value_decimal` | შესაბამისი typed measure field; scale/precision დამტკიცებული |
| `json_path`, `source_encoding` | adapter/parser evidence |
| `source_row_key` | lineage mapping |
| `operation` | mutation envelope; default ან delete semantics implicit არ არის |

### 8.5 `__rel_kids_statistical_semantic_binding`

| არსებული სვეტი | სამიზნე |
|---|---|
| `carrier_code` | legacy source binding |
| `metric_code` | structure measure reference |
| `unit_code` | measure authority; override მხოლოდ explicit contract-ით |
| `aggregation` | policy/rule reference |
| `obs_status` | declared attribute + attachment |
| `conf_status` | declared confidentiality attribute + authorized policy |
| `quality_policy_code`, `confidentiality_policy_code` | versioned Control references |
| `inference_method` | inference evidence |
| `operation` | ingestion envelope |

Generic profile-ში carrier-ზე დაფუძნებული binding აღარ არის სავალდებულო. dataset↔structure↔component↔measure კავშირები მას ცვლიან. ძველი binding-ის წაშლა დასაშვებია მხოლოდ retention/migration პოლიტიკით, parity-ის შემდეგ.

### 8.6 კლასიფიკატორები, metadata და რესურსები

| არსებული ობიექტი | გადაწყვეტილება |
|---|---|
| `__cl_scheme` | scheme identity და authority შენარჩუნდება |
| `__cl_version` | ვერსია და მოქმედების პერიოდი შენარჩუნდება |
| `__cl_item` | stable code + version; თარგმანი shared metadata-ში ახალი revision-ისას |
| `__cl_alias` | მხოლოდ adapter/crosswalk საჭიროებისას; ჩვეულებრივ შემტანთან არ ჩანს |
| `__cl_hierarchy` | საჭირო იერარქიის კავშირები; cycle/duplicate შემოწმება |
| `__gs_package`, `__gs_dataset`, `__gs_field`, `__gs_key` | საერთო source identity/schema authority; statistical schema ამ მექანიზმს ავსებს |
| `__gs_relation`, `__gs_projection` | მხოლოდ გამოცხადებული კავშირები და declarative mapping; caller SQL აკრძალულია |
| `__gs_page` | არსებული package contract-ის მოთხოვნა; statistical semantics page-ზე არ დამოკიდებულდება |
| `__gs_metadata_schema`, `__gs_metadata` | გამოყენება approved namespace/type-ებით; arbitrary observation EAV store-ად არ გამოიყენება |
| `__raw_document` | მიკვლევადობისთვის საჭირო ველები რჩება ავტომატურ ფენაში |
| `__ent_kids_resource` | ამ სტატისტიკური დიზაინის ფარგლებს გარეთ; კანონიკური სახელი შენარჩუნდება |

მხარდამჭერი ცხრილების სრული სვეტობრივი შემცირება ცალკე contract impact analysis-ს მოითხოვს. მათში არსებული საჭირო key/authority/version სვეტების წაშლა ამ გეგმით არ იგულისხმება.

## 9. მინიმალური grammar და invariant-ები

კონტრაქტის მომავალი machine-readable schema უნდა აღწერდეს:

1. profile identity/version და supported capabilities;
2. dataset → immutable structure binding;
3. ordered components: code, role, concept, representation, required;
4. measure references და unit/methodology;
5. attribute attachment და explicit defaults;
6. versioned validation/aggregation/derivation policies;
7. source fields → components mapping და declared constants;
8. UI captions/order/editability, სემანტიკის დუბლირების გარეშე.

Grammar უნდა იყოს closed და schema-validated. გაფართოება ხდება registered namespace/schema/version-ით. უდეკლარაციო field, role, attachment ან rule იწვევს rejection-ს. JSON metadata-ს გამოყენება ნებადართულია აღწერისთვის; მონაცემების უკონტროლო EAV შენახვა ამით არ ლეგიტიმდება.

Numeric ტიპებს აქვთ explicit precision/scale/rounding; IEEE floating point გამოიყენება მხოლოდ declared approximate semantics-ისას. პროცენტები, ინდექსები და საშუალოები sum-default-ს არ იღებს. გადაფარული ჯგუფები და total/subtotal უჯრედები ცალკე წესებს ექვემდებარება. სხვადასხვა grain-ის measure-ები ცალკე dataset-ში რჩება, approved relation-ით.

დეტერმინისტული semantic key მოიცავს dataset scope-ს, resolved structure-სა და canonical dimension tuple-ს. სტრიქონების გადალაგება იდენტობას არ ცვლის. ისტორიული revision/snapshot key-სთან არ ირევა. განმეორებითი load-ის idempotency ცალკე transport invariant-ია.

## 10. მიღება, უსაფრთხოება და აღდგენა

- Preview read-only ამოწმებს shape-ს, references-ს, უფლებებსა და value constraints-ს.
- Schema/contract mismatch ბლოკავს package-ს; row-level შეცდომების quarantine/atomic-reject რეჟიმი კონტრაქტში წინასწარ განისაზღვრება. ჩუმად დაკარგული row დაუშვებელია.
- Idempotency token მოიცავს artifact digest-ს, immutable contract/mapping revision-ს და ოპერაციის scope-ს. concurrent replay ვერ ქმნის დუბლირებულ შედეგს.
- Transient failure-ზე bounded retry/backoff/checkpoint; schema/authorization error retry-ით არ გვერდის ავლითდება. დროის და ზომის ლიმიტები deployment policy-ში განისაზღვრება.
- Delete საჭიროებს explicit operation-ს, უფლებასა და audit-ს; ფაილში row-ის არყოფნა ავტომატური delete არ არის.
- Cross-tenant reference, unauthorized confidentiality downgrade და arbitrary schema/SQL უარყოფილია.
- ფორმის დამალვა/read-only UI უსაფრთხოების საზღვარი არ არის; იგივე წესები server-ზე სრულდება.
- Quality, reconciliation და privacy gates + publication authorization ძალაში რჩება.
- Rollback აბრუნებს წინა approved contract/consumer routing/snapshot-ს; ახალი evidence და immutable artifacts არ იშლება.

## 11. განხორციელების თანმიმდევრობა

ახალი საიტისა და dataset-ის მომხმარებლური timeline, ინიციალიზაციის ობიექტები, compiler pipeline, კავშირები და workflow მდგომარეობები დეტალურად აღწერილია [სტატისტიკური კონტრაქტის lifecycle-ში](STATISTICAL-CONTRACT-LIFECYCLE.md). „სტატისტიკური მონაცემების“ არჩევა ქმნის draft-ს; შესავსები Access ცხრილები გენერირდება სტრუქტურის აღწერისა და კონტრაქტის დამტკიცების შემდეგ.

1. **Baseline:** მიმდინარე ACCDB/schema/code/runtime inventory, sample hashes და dependency graph. არსებული სტატისტიკური rows/values/status/relations-ის machine-readable export.
2. **Contract:** საბოლოო neutral naming, registry reuse, reference serialization, typed schema, compatibility და conformance matrix. გადაწყდეს Data Plane-ის multi-measure representation ყველა მოქმედი migration-ის შემოწმებით.
3. **Core:** generic contract validation/resolution; unit/concept/codelist identity და explicit policies. ახალი site-ისთვის core branch არ ემატება.
4. **Delivery:** contract-driven typed Access template და ორგანიზების გეგმით UI; ერთი ACCDB რჩება. raw/import და manual-authoring განსხვავებული adapter/profile-ებია.
5. **Ingestion/Data:** lossless mapping, exact numerical semantics, measure-specific statuses, lineage, idempotency და recovery.
6. **Compatibility:** ახალი revision; ძველი R8 adapter დროებით რჩება. crosswalk ყველა legacy metric/carrier/input key-ზე. ambiguity ბლოკავს ავტომატურ merge-ს.
7. **Parallel validation:** ერთი legacy aggregate dataset, ერთი სხვა პროექტის multi-dimension/multi-measure dataset და ერთი non-time dataset; semantic diff და round-trip.
8. **Release:** ტესტები/evidence, review და approved deployment. ძველი profile-ის deprecation მხოლოდ consumer inventory/retention/rollback readiness-ის შემდეგ.

შესრულების დაწყებისას baseline commit/hash-ები უნდა დაფიქსირდეს; ამ დოკუმენტის მომზადებისას სამუშაო ხეში სხვა ცვლილებებიც იყო. reproducible source-ის გარეშე release readiness არ გამოცხადდება.

## 12. მიღების checklist და evidence

- [ ] თითოეულ field-ს აქვს purpose, type, nullability, owner, source of truth და validation.
- [ ] საერთო სახელებში project/provider prefix არ არის; ახალი პროექტის onboarding core-code ცვლილების გარეშე მუშაობს.
- [ ] DSD/schema validation unit და negative suite PASS.
- [ ] multi-measure განსხვავებული ერთეულებითა და measure-level სტატუსებით round-trip PASS.
- [ ] non-time, mixed cardinality და განსხვავებული grain-ის უარყოფის შემთხვევები PASS.
- [ ] invalid code/version, duplicate tuple, missing/zero/suppressed განსხვავების ტესტები PASS.
- [ ] decimal boundary/rounding/overflow და lexical preservation PASS.
- [ ] replay, reorder, retry/crash recovery და concurrent ingest არ აორმაგებს დაკვირვებებს.
- [ ] property tests ადასტურებს key stability-ს და encode/decode სემანტიკურ თანხვედრას.
- [ ] legacy↔new reconciliation მოიცავს values, units, dimensions, statuses, relations და lineage-ს; მარტო row count არასაკმარისია.
- [ ] authenticated/authorized/tenant-negative და confidentiality-negative suite PASS.
- [ ] Access-ის მხარდაჭერილ ვერსიაზე ფორმის გახსნა/არჩევა/შენახვა და import integration PASS.
- [ ] declared cardinality/volume-ზე import latency, memory და query-cost ბიუჯეტები დამტკიცებული და გაზომილია.
- [ ] approved rule-ების aggregation/derivation negative suite PASS.
- [ ] exported SDMX ვერსიის conformance და unsupported conversion rejection PASS.
- [ ] immutable artifacts, migration crosswalk, compatibility report, test reports, reconciliation diff, audit trace და rollback rehearsal შენახულია.
- [ ] release invariant-ის სრული მტკიცებულება უკავშირდება deployed revision-ს.

Evidence უნდა დაიგეგმოს `docs/work/evidence/`-ში სამუშაო ბარათის ფარგლებში; დოკუმენტში checkbox შესრულებულად მხოლოდ შესაბამისი reproducible/runtime ჩანაწერის ბმულით მოინიშნება.

## 13. დარჩენილი ზუსტი დიზაინის გადაწყვეტილებებ
კონსოლიდირებული რეესტრი: [გადაწყვეტილებების რეესტრი Q01–Q50](STATISTICAL-CONTRACT-OPEN-QUESTIONS.md). 2026-09-19: 50/50 კითხვას გადაწყვეტილება ჰყავს; 9 default-ს მფლობელის დადასტურება სჭირდება; evidence — 0/50. ქვემოთ ჩამოთვლილ პუნქტების გადაწყვეტილებები: Q29–Q30, Q34–Q35, Q21/Q31/Q38, Q33, Q47, Q44/Q14. §6-ის measure-binding-ის ალტერნატივა დახურულია Q32-ით: wire-ში მხოლოდ `measureRef`. Canonical storage multi-measure-ს schema migration-ის გარეშე ფარავს (Q33); საჭიროა runtime writer-ის დასრულება.

- registry reuse-ის საბოლოო ფიზიკური mapping და reference wire syntax;
- Access-ის exact numeric adapter და provider capability limits;
- attribute attachment/default/override serialization;
- multi-measure canonical storage-ის ცვლილების საჭიროება და migration;
- მხარდაჭერილი SDMX exchange ფორმატები/ვერსიები და მათი conformance scope;
- tenant policy-ის მოქმედ ADR-თან შესაბამისობა და quantitative performance thresholds.

ეს პუნქტები არ ცვლის შეთანხმებულ მიმართულებას. ისინი უნდა გადაიჭრას implementation-ის შესაბამის ეტაპზე evidence-ით; გადაწყვეტილების არარსებობა არ უნდა დაიფაროს hardcoded fallback-ით.
