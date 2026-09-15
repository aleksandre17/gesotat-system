/**
 * mutator — full capability showcase
 *
 * Raw object: Spring/JPA response with nested entities, arrays, mixed types.
 * Goal: transform it into a clean, UI-ready record in one fluent pipeline.
 */

import {
  mutate,
  mutator,
  field,
  extract,
  extractMany,
  toNumber,
  toBoolean,
  toDate,
  toISO,
} from "./index";

// ── Shapes ────────────────────────────────────────────────────────────────────

type RawReport = {
  id: number;
  title: string;
  status: string;
  priority: string; // "1" | "2" | "3" as string from DB
  isPublic: string; // "true" / "false" mis-typed
  score: string | null;
  createdAt: string;
  author: { id: number; name: string };
  category: { id: number; code: string } | null;
  tags: { id: number; label: string }[];
  viewers: { id: number }[];
  attachments: { id: number; name: string; size: number; mimeType: string }[];
  meta: {
    views: string;
    featured: string;
    region: { id: number; code: string };
  };
  _links: unknown;
  __typename: string;
};

type AppReport = {
  id: number;
  title: string;
  status: "draft" | "published" | "archived";
  priority: number;
  isPublic: boolean;
  score: number | null;
  createdAt: Date;
  authorId: number;
  authorName: string;
  categoryId: number | null;
  tagIds: number[];
  tagLabels: string[];
  viewerIds: number[];
  attachments: {
    id: number;
    name: string;
    size: number;
    mimeType: string;
    isImage: boolean;
  }[];
  hasImages: boolean;
  totalAttachmentSize: number;
  metaViews: number;
  metaFeatured: boolean;
  metaRegionId: number;
  metaRegionCode: string;
  badge: string;
  isEditable: boolean;
};

// ── Reusable sub-transforms ───────────────────────────────────────────────────

type Attachment = RawReport["attachments"][number];

/**
 * Enrich each attachment: add isImage flag.
 * mutator<T>().asArray() — applies this mutator to every array element.
 */
const enrichAttachments = mutator<Attachment>()
  .set("isImage", (d) => d.mimeType.startsWith("image/"))
  .asArray();

/**
 * Flatten meta.* onto the root — stored as Mutator, passed to .use() directly.
 * No .build() needed: .use() accepts a Mutator instance.
 */
const flattenMeta = mutator<RawReport>()
  .set("metaViews", (d) => toNumber(d.meta.views))
  .set("metaFeatured", (d) => toBoolean(d.meta.featured))
  .set("metaRegionId", (d) => extract("id")(d.meta.region))
  .set("metaRegionCode", (d) => extract("code")(d.meta.region))
  .omit("meta");

/**
 * Priority coercion with validation — field() builder.
 * Converts "1"–"3" string to number, clamps to valid range.
 */
const priorityField = field<string>()
  .transform(toNumber)
  .when(
    (v) => v < 1,
    () => 1,
  )
  .when(
    (v) => v > 3,
    () => 3,
  )
  .build();

// ── Raw data ──────────────────────────────────────────────────────────────────

const raw: RawReport = {
  id: 42,
  title: "Q1 Statistical Report",
  status: "DRAFT",
  priority: "1",
  isPublic: "false",
  score: "88.5",
  createdAt: "2025-03-01T10:00:00Z",
  author: { id: 7, name: "Ana Beridze" },
  category: { id: 3, code: "ECON" },
  tags: [
    { id: 10, label: "finance" },
    { id: 11, label: "q1" },
  ],
  viewers: [{ id: 1 }, { id: 2 }, { id: 5 }],
  attachments: [
    { id: 1, name: "chart.png", size: 204800, mimeType: "image/png" },
    {
      id: 2,
      name: "data.xlsx",
      size: 512000,
      mimeType: "application/vnd.ms-excel",
    },
    { id: 3, name: "cover.jpg", size: 102400, mimeType: "image/jpeg" },
  ],
  meta: { views: "1340", featured: "true", region: { id: 2, code: "KA" } },
  _links: { self: "/api/reports/42" },
  __typename: "Report",
};

const currentUserId = 7;

// ── Main pipeline (immediate mode) ───────────────────────────────────────────

const result = mutate(raw)
  // Coerce primitive fields — transformMany accepts field() builders directly
  .transformMany({
    priority: priorityField,
    isPublic: toBoolean,
    createdAt: toDate,
  })

  // null-safe built-in: score stays null if null, no maybe() wrapper needed
  .transform("score", toNumber)

  // Derive isEditable BEFORE flattening author — access d.author.id cleanly
  .set(
    "isEditable",
    (d) => d.status === "DRAFT" && d.author.id === currentUserId,
  )

  // Flatten JPA entity refs → scalar fields
  .set("authorId", (d) => extract("id")(d.author))
  .set("authorName", (d) => extract("name")(d.author))
  .omit("author")

  // Nullable category
  .set("categoryId", (d) =>
    d.category !== null ? extract("id")(d.category) : null,
  )
  .omit("category")

  // tags → two parallel arrays in one call
  .mapMany([
    { from: "tags", to: "tagIds", transform: extractMany("id") },
    { from: "tags", to: "tagLabels", transform: extractMany("label") },
  ])

  // viewers → flat id array
  .transform("viewers", extractMany("id"))
  .rename("viewers", "viewerIds")

  // Enrich attachments, then derive aggregate fields
  .transform("attachments", enrichAttachments)
  .set("hasImages", (d) =>
    (d.attachments as AppReport["attachments"]).some((a) => a.isImage),
  )
  .set("totalAttachmentSize", (d) =>
    (d.attachments as AppReport["attachments"]).reduce((s, a) => s + a.size, 0),
  )

  // Delegate meta flattening to a reusable Mutator — no .build() needed
  .use(flattenMeta)

  // Normalise status + derive badge in one switchOn using function key
  // No separate .transform("status", ...) step needed
  .switchOn((d) => String(d.status).toLowerCase(), {
    draft: (m) => m.set("status", "draft").set("badge", "✏️ Draft"),
    published: (m) => m.set("status", "published").set("badge", "✅ Published"),
    archived: (m) => m.set("status", "archived").set("badge", "📦 Archived"),
    _: (m) => m.set("badge", "❓ Unknown"),
  })

  .when(
    (d) =>
      d.status === "published" && (d as unknown as AppReport).priority === 1,
    (m) => m.set("pinned", true),
  )

  .branch(
    (d) => (d as unknown as AppReport).isPublic,
    (m) =>
      m.set("visibility", "public").set("shareUrl", `/reports/${raw.id}/share`),
    (m) => m.set("visibility", "private"),
  )

  .omit("_links", "__typename")

  .tap((d) => {
    if (process.env.NODE_ENV === "development") console.debug("[mutator]", d);
  })

  // Typed output — no `as AppReport` cast needed
  .get<AppReport>();

// ── Deferred: reusable list transform ────────────────────────────────────────

const reportListTransform = mutator<RawReport>()
  .pick("id", "title", "status", "priority", "createdAt", "author")
  .transformMany({ priority: toNumber, createdAt: toISO })
  .switchOn((d) => String(d.status).toLowerCase(), {
    draft: (m) => m.set("status", "draft"),
    published: (m) => m.set("status", "published"),
    archived: (m) => m.set("status", "archived"),
  })
  .set("authorId", (d) => extract("id")(d.author))
  .set("authorName", (d) => extract("name")(d.author))
  .omit("author")
  .build();

const rawList: RawReport[] = [raw, { ...raw, id: 43, status: "PUBLISHED" }];
const appList = rawList.map(reportListTransform);

// ── Compose: two Mutators, no .build() between them ──────────────────────────

// Stored as Mutator — .use() accepts it directly
const addAuditStamp = mutator()
  .set("_transformedAt", new Date().toISOString())
  .set("_transformedBy", "reportListTransform");

const auditedTransform = mutator<RawReport>()
  .use(reportListTransform) // RecordTransform
  .use(addAuditStamp) // Mutator — no .build() needed
  .build();

const auditedList = rawList.map(auditedTransform);

export { result, appList, auditedList };
