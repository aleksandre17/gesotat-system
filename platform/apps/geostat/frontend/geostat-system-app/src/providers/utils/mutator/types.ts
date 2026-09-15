/** Core types for the mutator toolkit. Zero external dependencies. */

export type AnyRecord = Record<string, unknown>;

/** `T extends object` — intentionally wider than `AnyRecord`.
 *  Allows plain typed interfaces (e.g. `Contract`, `User`) to be used
 *  directly without `& AnyRecord` intersection. */
export type Transform<TIn = unknown, TOut = unknown> = (value: TIn) => TOut;
export type RecordTransform<T extends object = AnyRecord> = (
  data: T,
) => AnyRecord;
export type Predicate<T = unknown> = (value: T) => boolean;

/** Internal queued operation — one step in the pipeline. */
export type Op = (data: AnyRecord) => AnyRecord;

/** A field step: move value from `from` to `to` with optional transform. */
export interface FieldStep {
  from: string;
  to: string;
  transform?: LooseTransform | HasBuild;
}

/**
 * Bivariant transform — any function signature is assignable to this type.
 *
 * Unlike `Transform<unknown, unknown>`, this accepts functions with specific
 * input types (e.g. `(v: string) => number`) without casts or `any`.
 *
 * TypeScript method signatures are bivariant even under `strictFunctionTypes`,
 * so extracting a method type produces a bivariant function type.
 */
export type LooseTransform = { _(value: unknown): unknown }["_"];

/**
 * Structural interface for anything that produces a LooseTransform via `.build()`.
 * Matches `FieldBuilder<TIn, TOut>` for any TIn/TOut — no generics or `any` needed.
 * (`Transform<TIn, TOut>` is always assignable to `LooseTransform` via bivariance.)
 */
export type HasBuild = { build(): LooseTransform };

/**
 * Switch-case map for `.switchOn()`. `_` is the default branch.
 *
 * The closure receives a `Mutator<T>` and returns `Mutator<T>` — all branches
 * operate on the same typed record shape. No `any` needed.
 */
export type SwitchCases<T extends object> = {
  [key: string]: (
    m: import("./Mutator").Mutator<T>,
  ) => import("./Mutator").Mutator<T>;
} & {
  _?: (m: import("./Mutator").Mutator<T>) => import("./Mutator").Mutator<T>;
};
