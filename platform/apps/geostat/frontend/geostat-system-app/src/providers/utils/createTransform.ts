/**
 * DataProvider transform utilities — bidirectional JPA ↔ app layer.
 *
 * Thin adapter on top of the `mutator/` package.
 * All generic mutation power lives there; this file only adds:
 *
 *   createPipeline()  — ordered field-step runner with pre/post hooks
 *   createTransform() — bidirectional JPA ↔ app shorthand
 *
 * Everything else (mutate, mutator, field, extract, …) is re-exported
 * from `mutator/` so consumers only need one import.
 */

import type { ResourceTransform } from "../createDataProvider";
import type { Transform, RecordTransform, FieldStep } from "./mutator";
import {
  Mutator,
  FieldBuilder,
  mutator,
  extract,
  wrap,
  extractMany,
  wrapMany,
} from "./mutator";

// ── Re-exports ────────────────────────────────────────────────────────────────

export type {
  AnyRecord,
  Transform,
  RecordTransform,
  Predicate,
  LooseTransform,
  HasBuild,
  FieldStep,
} from "./mutator";

export {
  // Entry points
  mutate,
  mutator,
  field,
  FieldBuilder,
  // Value
  identity,
  constant,
  maybe,
  guard,
  when,
  unless,
  // Nested
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
} from "./mutator";

// ── createPipeline ────────────────────────────────────────────────────────────

type Buildable = RecordTransform | Mutator;

export interface PipelineHooks {
  pre?: Buildable;
  post?: Buildable;
}

/**
 * Builds a RecordTransform from ordered field steps + optional hooks.
 * Direction-agnostic — caller decides how to use the result.
 */
export function createPipeline(
  steps: FieldStep[],
  hooks: PipelineHooks = {},
): RecordTransform {
  const m = mutator();
  if (hooks.pre) m.use(hooks.pre);
  for (const { from, to, transform: fn } of steps) m.map(from, to, fn);
  if (hooks.post) m.use(hooks.post);
  return m.build();
}

// ── createTransform ───────────────────────────────────────────────────────────

export interface FieldConfig<TInput = unknown, TOutput = unknown> {
  scalar: string;
  array?: boolean;
  deserialize?: Transform<TInput, TOutput> | FieldBuilder<TInput, TOutput>;
  serialize?: Transform<TOutput, TInput> | FieldBuilder<TOutput, TInput>;
}

export type FieldMapping<TInput = unknown, TOutput = unknown> =
  | string
  | FieldConfig<TInput, TOutput>;

export interface TransformHooks {
  deserialize?: Buildable | Buildable[];
  serialize?: Buildable | Buildable[];
}

function resolveHookList(
  input: Buildable | Buildable[] | undefined,
): RecordTransform | undefined {
  if (!input) return undefined;
  const list = Array.isArray(input) ? input : [input];
  const m = mutator();
  for (const b of list) m.use(b);
  return m.build();
}

function toSteps(
  mappings: Record<string, FieldMapping>,
  direction: "deserialize" | "serialize",
): FieldStep[] {
  return Object.entries(mappings).map(([nested, config]) => {
    const isString = typeof config === "string";
    const scalar = isString ? config : config.scalar;
    const isArray = !isString && !!config.array;

    if (direction === "deserialize") {
      const fn = isString ? undefined : config.deserialize;
      return {
        from: nested,
        to: scalar,
        transform: fn ?? (isArray ? extractMany("id") : extract("id")),
      };
    } else {
      const fn = isString ? undefined : config.serialize;
      return {
        from: scalar,
        to: nested,
        transform: fn ?? (isArray ? wrapMany("id") : wrap("id")),
      };
    }
  });
}

/**
 * Bidirectional JPA ↔ app transform.
 * Pass field mappings; `deserialize` and `serialize` are derived automatically.
 *
 * @example
 * createTransform({ parent: "parentId" })
 * createTransform({
 *   department: { scalar: "departmentId" },
 *   roles:      { scalar: "roleIds", array: true },
 * })
 */
export function createTransform(
  mappings: Record<string, FieldMapping>,
  hooks?: TransformHooks,
): ResourceTransform {
  return {
    deserialize: createPipeline(toSteps(mappings, "deserialize"), {
      post: resolveHookList(hooks?.deserialize),
    }),
    serialize: createPipeline(toSteps(mappings, "serialize"), {
      pre: resolveHookList(hooks?.serialize),
    }),
  };
}
