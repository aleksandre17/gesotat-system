/**
 * mutator — fluent, generic record-mutation toolkit.
 *
 * Entry points:
 *   mutate(data)  — immediate: data in → chain → .get<TOut>()
 *   mutator()     — deferred:  chain → .build() → reusable RecordTransform
 *   field()       — single value: chain transforms → .build() → Transform
 *
 * @example
 * // Immediate
 * const result = mutate(raw)
 *   .map("dept", "deptId", extract("id"))
 *   .omit("__typename")
 *   .get<AppUser>();
 *
 * // Deferred
 * const transform = mutator<User>()
 *   .map("dept", "deptId", extract("id"))
 *   .omit("__typename")
 *   .build();
 * list.map(transform);
 *
 * // Field
 * field().nullable().default(0).transform(toNumber).build()
 */

// ── Core types ────────────────────────────────────────────────────────────────
export type {
  AnyRecord,
  Transform,
  RecordTransform,
  Predicate,
  Op,
  FieldStep,
  SwitchCases,
  LooseTransform,
  HasBuild,
} from "./types";

// ── Entry points ──────────────────────────────────────────────────────────────
export { Mutator, mutate, mutator } from "./Mutator";
export { FieldBuilder, field } from "./FieldBuilder";

// ── Pure primitive helpers ────────────────────────────────────────────────────
export {
  // Value guards / conditionals
  identity,
  constant,
  maybe,
  guard,
  when,
  unless,

  // Nested object
  extract,
  wrap,

  // Array
  extractMany,
  wrapMany,
  arrayOf,
  compact,

  // Composition
  pipe,
  compose,

  // Record-level
  omit,
  pick,
  defaults,
  tap,

  // Coercions
  toNumber,
  toString,
  toBoolean,
  toDate,
  toISO,
} from "./primitives";
