/**
 * FieldBuilder<TIn, TOut> — fluent single-value transform builder.
 *
 * Use when you need nullable handling, defaults, or chained value transforms.
 * For simple cases, a plain function is enough.
 *
 * @example
 * field().nullable().default(0).transform(toNumber).build()
 * field<string>().when(v => v === "", () => null).nullable().build()
 */

import type { Transform, Predicate } from "./types";

export class FieldBuilder<TIn = unknown, TOut = TIn> {
  private fns: Transform[] = [];
  private _nullable = false;
  private _defaultValue: unknown = undefined;
  private _hasDefault = false;

  /** Apply a transform — narrows the output type. */
  transform<TNext>(fn: Transform<TOut, TNext>): FieldBuilder<TIn, TNext> {
    const next = new FieldBuilder<TIn, TNext>();
    next.fns = [...this.fns, fn as Transform];
    next._nullable = this._nullable;
    next._defaultValue = this._defaultValue;
    next._hasDefault = this._hasDefault;
    return next;
  }

  /** Left-to-right chain of transforms. */
  pipe(...fns: Transform[]): this {
    this.fns.push(...fns);
    return this;
  }

  /** Apply `fn` only when predicate passes. */
  when(predicate: Predicate<TOut>, fn: Transform<TOut>): this {
    this.fns.push((v) => (predicate(v as TOut) ? fn(v as TOut) : v));
    return this;
  }

  /** Apply `fn` only when predicate fails. */
  unless(predicate: Predicate<TOut>, fn: Transform<TOut>): this {
    return this.when((v) => !predicate(v), fn);
  }

  /** Pass null/undefined through without running transforms. */
  nullable(): this {
    this._nullable = true;
    return this;
  }

  /** Substitute `value` when input is null/undefined. */
  default(value: TOut): this {
    this._defaultValue = value;
    this._hasDefault = true;
    return this;
  }

  /** Seal into a plain Transform function. */
  build(): Transform<TIn, TOut> {
    const fns = [...this.fns];
    const run = (v: unknown): unknown => fns.reduce((acc, fn) => fn(acc), v);
    const nullable = this._nullable;
    const hasDefault = this._hasDefault;
    const defaultValue = this._defaultValue;

    return (v: TIn): TOut => {
      if (v == null) {
        if (hasDefault) return defaultValue as TOut;
        if (nullable) return null as TOut;
      }
      return run(v) as TOut;
    };
  }
}

/** Create a new FieldBuilder. */
export const field = <TIn = unknown>(): FieldBuilder<TIn> =>
  new FieldBuilder<TIn>();
