/**
 * Closure × whole-object mutation
 *
 * პრინციპი: closure-ი გარედანაა — იღებს Mutator<T>-ს, ხედავს მთელ
 * ობიექტს სრული ტიპებით, გადაწყვეტს რა გაუკეთოს, აბრუნებს Mutator-ს.
 *
 * Key API:
 *   .apply(fn)  — inject external (m: Mutator<T>) => Mutator closure
 *   .use(m)     — compose with built RecordTransform or another Mutator
 */

import { mutate, mutator } from "./index";
import type { Mutator } from "./Mutator";

// ── Shapes ────────────────────────────────────────────────────────────────────

type Contract = {
  id: number;
  type: "EMPLOYMENT" | "FREELANCE" | "INTERNSHIP";
  status: "PENDING" | "ACTIVE" | "SUSPENDED" | "EXPIRED";
  employee: { id: number; name: string; level: string };
  department: { id: number; code: string; budget: number };
  salaryBase: number;
  salaryBonus: number | null;
  hoursPerWeek: number;
  startDate: string;
  endDate: string | null;
  permissions: { id: number; code: string }[];
  flags: {
    isSenior: boolean;
    isRemote: boolean;
    isConfidential: boolean;
    needsApproval: boolean;
  };
};

type AppContract = {
  id: number;
  type: Contract["type"];
  status: Contract["status"];
  contractLabel: string;
  salaryTotal: number | string; // "***" when redacted
  employeeId: number;
  employeeName: string;
  permissionIds: number[];
  permissionCodes: string[];
  isSenior: boolean;
  isRemote: boolean;
  needsApproval: boolean;
  uiColor: string;
  uiBadge: string;
  isEditable: boolean;
};

// ── Closure type alias ────────────────────────────────────────────────────────

// T = Contract — no & AnyRecord needed.
// d.flags, d.salaryBase etc. are fully typed inside all closures.
type M = Mutator<Contract>;

// ── Closure library ───────────────────────────────────────────────────────────

/**
 * Employment rules — curried: budget comes from outside via closure.
 * d.flags, d.salaryBase, d.hoursPerWeek — all typed, no casts.
 */
const applyEmploymentRules =
  (departmentBudgetCap: number) =>
  (m: M): M =>
    m
      .set("contractLabel", "სამსახურის ხელშეკრულება")
      .set("salaryTotal", (d) => d.salaryBase + (d.salaryBonus ?? 0))
      .set("salaryPercentage", (d) =>
        Math.round(
          ((d.salaryBase + (d.salaryBonus ?? 0)) / departmentBudgetCap) * 100,
        ),
      )
      .set("leaveEntitlement", (d) => (d.flags.isSenior ? 28 : 21))
      .set("overtimeEligible", (d) => d.hoursPerWeek > 40)
      .when(
        (d) => d.flags.isSenior,
        (m2) =>
          m2
            .set("mentorshipRole", true)
            .set("techReviewAccess", true)
            .set("salaryBand", "SENIOR"),
      )
      .when(
        (d) => d.flags.isRemote,
        (m2) =>
          m2
            .set("workLocation", "remote")
            .set("homeOfficeGrant", 500)
            .set("timezone", "UTC+4"),
      );

/**
 * Freelance rules — ADMIN permission removed, endDate enforced.
 * .set("permissions", fn) gives typed d.permissions — no cast needed.
 */
const applyFreelanceRules = (m: M): M =>
  m
    .set("contractLabel", "მომსახურების ხელშეკრულება")
    .set("invoicingEnabled", true)
    .set("vatRequired", (d) => d.salaryBase > 1000)
    .set("salaryTotal", (d) => d.salaryBase)
    .set("permissions", (d) => d.permissions.filter((p) => p.code !== "ADMIN"))
    .omit("salaryBonus")
    .when(
      (d) => d.endDate === null,
      (m2) =>
        m2
          .set("endDate", "2025-12-31")
          .set("endDateWarning", "⚠️ endDate დაყენდა ავტომატურად"),
    );

/**
 * Internship rules — salary zeroed, hours capped, READ-only permissions.
 */
const applyInternshipRules = (m: M): M =>
  m
    .set("contractLabel", "სტაჟირების ხელშეკრულება")
    .set("salaryTotal", 0)
    .set("salaryBonus", null)
    .set("hoursPerWeek", (d) => Math.min(d.hoursPerWeek, 20))
    .set("permissions", [{ id: 1, code: "READ" }])
    .set("overtimeEligible", false)
    .when(
      (d) => d.endDate === null,
      (m2) => m2.set("endDate", "2025-08-31"),
    );

// ── Status overlay — curried: approvedBy from auth context ───────────────────

const applyStatusOverlay =
  (approvedBy?: string) =>
  (m: M): M =>
    m.switchOn("status", {
      PENDING: (m2) =>
        m2
          .set("uiColor", "#f59e0b")
          .set("uiBadge", "⏳ განხილვაში")
          .set("isEditable", true)
          .set("canActivate", !!approvedBy)
          .set("approvedBy", approvedBy ?? null),

      ACTIVE: (m2) =>
        m2
          .set("uiColor", "#10b981")
          .set("uiBadge", "✅ აქტიური")
          .set("isEditable", false)
          .set("canActivate", false),

      SUSPENDED: (m2) =>
        m2
          .set("uiColor", "#ef4444")
          .set("uiBadge", "🚫 შეჩერებული")
          .set("isEditable", false)
          .set("canReactivate", true)
          .set("suspendedAt", new Date().toISOString()),

      EXPIRED: (m2) =>
        m2
          .set("uiColor", "#6b7280")
          .set("uiBadge", "📦 ვადაგასული")
          .set("isEditable", false)
          .set("archived", true),
    });

// ── Confidentiality overlay — curried: viewer roles from auth context ─────────

const applyConfidentiality =
  (viewerRoles: string[]) =>
  (m: M): M =>
    m.when(
      (d) => d.flags.isConfidential && !viewerRoles.includes("HR"),
      (m2) =>
        m2
          .set("salaryBase", "***")
          .set("salaryBonus", "***")
          .set("salaryTotal", "***")
          .set("employee", (d) => ({ id: d.employee.id, name: "Confidential" }))
          .set("_redacted", true),
    );

// ── Raw data ──────────────────────────────────────────────────────────────────

const raw: Contract = {
  id: 101,
  type: "EMPLOYMENT",
  status: "ACTIVE",
  employee: { id: 7, name: "Ana Beridze", level: "SENIOR" },
  department: { id: 3, code: "TECH", budget: 50000 },
  salaryBase: 3200,
  salaryBonus: 800,
  hoursPerWeek: 40,
  startDate: "2023-01-15",
  endDate: null,
  permissions: [
    { id: 1, code: "READ" },
    { id: 2, code: "WRITE" },
    { id: 5, code: "ADMIN" },
  ],
  flags: {
    isSenior: true,
    isRemote: true,
    isConfidential: false,
    needsApproval: false,
  },
};

const DEPT_BUDGET = raw.department.budget;
const VIEWER_ROLES = ["MANAGER"]; // no HR — confidential fields stay masked

// ── Final pipeline ────────────────────────────────────────────────────────────

const result = mutate(raw)
  // 1. type-based: budget injected via currying
  .switchOn("type", {
    EMPLOYMENT: applyEmploymentRules(DEPT_BUDGET),
    FREELANCE: applyFreelanceRules,
    INTERNSHIP: applyInternshipRules,
  })

  // 2. .apply() — correct entry point for external (m) => m.set(...) closures
  //    .use()   — for RecordTransform / built Mutators (data pipeline composition)
  .apply(applyStatusOverlay("Giorgi Maisuradze"))
  .apply(applyConfidentiality(VIEWER_ROLES))

  // 3. Flatten remaining nested fields — d.employee, d.permissions fully typed
  .set("employeeId", (d) => d.employee.id)
  .set("employeeName", (d) => d.employee.name)
  .set("permissionIds", (d) => d.permissions.map((p) => p.id))
  .set("permissionCodes", (d) => d.permissions.map((p) => p.code))

  // 4. Hoist flags to root, drop objects
  .set("isSenior", (d) => d.flags.isSenior)
  .set("isRemote", (d) => d.flags.isRemote)
  .set("needsApproval", (d) => d.flags.needsApproval)
  .omit("flags", "employee", "department")

  .get<AppContract>();

// ── Deferred: the same pipeline, context injected from outside ───────────────────

const buildContractTransform = (
  deptBudget: number,
  approvedBy: string | undefined,
  viewerRoles: string[],
) =>
  mutator<Contract>()
    .switchOn("type", {
      EMPLOYMENT: applyEmploymentRules(deptBudget),
      FREELANCE: applyFreelanceRules,
      INTERNSHIP: applyInternshipRules,
    })
    .apply(applyStatusOverlay(approvedBy))
    .apply(applyConfidentiality(viewerRoles))
    .set("permissionIds", (d) => d.permissions.map((p) => p.id))
    .set("permissionCodes", (d) => d.permissions.map((p) => p.code))
    .omit("flags")
    .build();

// Three different views of the same data — only injected context differs
const hrTransform = buildContractTransform(50000, "Ana", ["HR", "MANAGER"]);
const managerTransform = buildContractTransform(50000, undefined, ["MANAGER"]);
const publicTransform = buildContractTransform(50000, undefined, []);

const contracts: Contract[] = [raw];

const hrView = contracts.map(hrTransform);
const managerView = contracts.map(managerTransform);
const publicView = contracts.map(publicTransform);

export { result, hrView, managerView, publicView };
