/**
 * Mutator<T> — fluent, generic record-mutation builder.
 *
 * Two modes, same API:
 *   mutate(data).set(...).get() — immediate: data in, transform, data out
 *   mutator().set(...).build() — deferred:  build a reusable RecordTransform
 *
 * Conditional closures receive a fresh Mutator pre-loaded with the current
 * data — so they can freely chain without affecting the parent pipeline.
 */

import type {
  AnyRecord,
  Transform,
  RecordTransform,
  Predicate,
  Op,
  SwitchCases,
  LooseTransform,
} from "./types";

// ── Internal sentinel ─────────────────────────────────────────────────────────

const DEFERRED = Symbol("deferred");
type DeferredMode = typeof DEFERRED;

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Structural interface for anything that produces a transform via `.build()`.
 * Matches FieldBuilder<TIn, TOut> for any TIn/TOut — no `any` needed.
 * (Transform<TIn, TOut> is always assignable to LooseTransform via bivariance.)
 */
type HasBuild = { build(): LooseTransform };

function resolveTransform(fn: LooseTransform | HasBuild): LooseTransform {
  return typeof fn === "function" ? fn : fn.build();
}

// ── Mutator ───────────────────────────────────────────────────────────────────

export class Mutator<T extends object = AnyRecord> {
  private readonly ops: Op[] = [];
  private readonly source: T | DeferredMode;

  constructor(data?: T) {
    this.source = data !== undefined ? data : DEFERRED;
  }

  // ── Internal ────────────────────────────────────────────────────────────────

  private push(op: Op): this {
    this.ops.push(op);
    return this;
  }

  private run(data: AnyRecord): AnyRecord {
    return this.ops.reduce<AnyRecord>((acc, op) => op(acc), data);
  }

  private fresh(data: AnyRecord): Mutator<T> {
    return new Mutator<T>(data as T);
  }

  // ── Single-field ops ────────────────────────────────────────────────────────

  /**
   * Set a field to a static value or a function derived from the whole record.
   * `(d) => d.first + "" + d.last` — full record is typed as T.
   */
  set(key: string, fn: (data: T) => unknown): this;
  set(key: string, value: unknown): this;
  set(key: string, value: unknown | ((data: T) => unknown)): this {
    return this.push((data) => ({
      ...data,
      [key]:
        typeof value === "function"
          ? (value as (d: T) => unknown)(data as T)
          : value,
    }));
  }

  /**
   * Move a field from `from` to `to`, optionally transforming the value.
   * The source field is removed after the move.
   * Skips when the source field is absent or null.
   */
  map(from: string, to: string, fn?: LooseTransform | HasBuild): this {
    const t = fn ? resolveTransform(fn) : undefined;
    return this.push((data) => {
      if (!(from in data) || data[from] == null) return data;
      const { [from]: value, ...rest } = data;
      return { ...rest, [to]: t ? t(value) : value };
    });
  }

  /** Rename a field without transforming its value. */
  rename(from: string, to: string): this {
    return this.map(from, to);
  }

  /**
   * Apply a transform to a field in-place — key stays the same.
   * Null-safe: skips the transform when the field value is null/undefined.
   * Use `set()` if you need to transform null values explicitly.
   */
  transform(key: string, fn: LooseTransform | HasBuild): this {
    const t = resolveTransform(fn);
    return this.push((data) =>
      key in data && data[key] != null
        ? { ...data, [key]: t(data[key]) }
        : data,
    );
  }

  // ── Multi-field ops ─────────────────────────────────────────────────────────

  /**
   * Apply different transforms to multiple fields at once.
   * Keys absent from the record are silently skipped.
   *
   * @example
   * .transformMany({ amount: toNumber, score: maybe(toNumber), isActive: toBoolean })
   */
  transformMany(map: Record<string, LooseTransform | HasBuild>): this {
    return this.push((data) => {
      const r = { ...data };
      for (const [key, fn] of Object.entries(map)) {
        if (key in r && r[key] != null) r[key] = resolveTransform(fn)(r[key]);
      }
      return r;
    });
  }

  /**
   * Move + transform multiple fields in one call.
   *
   * @example
   * .mapMany([
   *   { from: "department", to: "departmentId",   transform: extract("id") },
   *   { from: "department", to: "departmentCode", transform: extract("code") },
   * ])
   */
  mapMany(
    steps: Array<{
      from: string;
      to: string;
      transform?: LooseTransform | HasBuild;
    }>,
  ): this {
    return this.push((data) => {
      let r = { ...data };
      const seen = new Set<string>();
      for (const { from, to, transform: fn } of steps) {
        if (!(from in r) || r[from] == null) continue;
        const t = fn ? resolveTransform(fn) : undefined;
        const value = t ? t(r[from]) : r[from];
        r = { ...r, [to]: value };
        seen.add(from);
      }
      for (const key of seen) {
        if (!steps.some((s) => s.to === key)) delete r[key];
      }
      return r;
    });
  }

  // ── Array-field ops ─────────────────────────────────────────────────────────

  /** Apply a transform to every element of an array field. */
  each(key: string, fn: LooseTransform | HasBuild): this {
    const t = resolveTransform(fn);
    return this.push((data) =>
      key in data && Array.isArray(data[key])
        ? { ...data, [key]: (data[key] as unknown[]).map(t) }
        : data,
    );
  }

  /** Filter elements of an array field by a predicate. */
  filter(key: string, predicate: Predicate): this {
    return this.push((data) =>
      key in data && Array.isArray(data[key])
        ? { ...data, [key]: (data[key] as unknown[]).filter(predicate) }
        : data,
    );
  }

  /** Sort elements of an array field. */
  sort(key: string, compareFn?: (a: unknown, b: unknown) => number): this {
    return this.push((data) =>
      key in data && Array.isArray(data[key])
        ? { ...data, [key]: [...(data[key] as unknown[])].sort(compareFn) }
        : data,
    );
  }

  // ── Whole-object ops ────────────────────────────────────────────────────────

  /**
   * Merge a partial object — or a function that derives one from the record.
   * Merged keys override existing values (unlike `.defaults()`).
   */
  merge(partial: Partial<T> | ((data: T) => Partial<T>)): this {
    return this.push((data) => ({
      ...data,
      ...(typeof partial === "function"
        ? (partial as (d: T) => Partial<T>)(data as T)
        : partial),
    }));
  }

  /** Fill in missing fields — existing values are NOT overwritten. */
  defaults(obj: Partial<T>): this {
    return this.push((data) => ({ ...obj, ...data }));
  }

  /** Remove the specified fields. */
  omit(...keys: string[]): this {
    return this.push((data) => {
      const r = { ...data };
      for (const k of keys) delete r[k];
      return r;
    });
  }

  /** Keep only the specified fields. */
  pick(...keys: string[]): this {
    return this.push((data) =>
      Object.fromEntries(
        keys.filter((k) => k in data).map((k) => [k, data[k]]),
      ),
    );
  }

  /** Side-effect only — inspect the record without mutating it. */
  tap(fn: (data: T) => void): this {
    return this.push((data) => {
      fn(data as T);
      return data;
    });
  }

  // ── Conditional ops ─────────────────────────────────────────────────────────

  /**
   * Apply a mutation only when `predicate(data)` returns true.
   * The closure receives a fresh Mutator preloaded with the current data.
   *
   * @example
   * .when((d) => d.type === "PREMIUM", (m) => m.set("badge", "⭐").omit("trial"))
   */
  when(
    predicate: (data: T) => boolean,
    fn: (m: Mutator<T>) => Mutator<T>,
  ): this {
    return this.push((data) => {
      if (!predicate(data as T)) return data;
      return fn(this.fresh(data)).get();
    });
  }

  /** Inverse of `.when()` — applies only when predicate returns false. */
  unless(
    predicate: (data: T) => boolean,
    fn: (m: Mutator<T>) => Mutator<T>,
  ): this {
    return this.when((data) => !predicate(data), fn);
  }

  /**
   * Full if/else branch — both sides are optional.
   *
   * @example
   * .branch(* (d) => d.isAdmin,
   *   (m) => m.set("role", "admin").omit("restrictedAt"),
   *   (m) => m.set("role", "user"), *
   */
  branch(
    predicate: (data: T) => boolean,
    ifTrue?: (m: Mutator<T>) => Mutator<T>,
    ifFalse?: (m: Mutator<T>) => Mutator<T>,
  ): this {
    return this.push((data) => {
      const branch = this.fresh(data);
      if (predicate(data as T)) {
        return ifTrue ? ifTrue(branch).get() : data;
      }
      return ifFalse ? ifFalse(branch).get() : data;
    });
  }

  /**
   * Pattern-match on a field value or a derived key — like a typed switch statement.
   * The first argument can be a field name (string) or a function that derives the key.
   * `_` is the default branch (optional).
   *
   * @example
   * .switchOn("type", {
   *   PREMIUM: (m) => m.set("badge", "⭐").set("priority", 1),
   *   TRIAL: (m) => m.set("badge", "⚠️").set("priority", 3),
   *   _: (m) => m.set("badge", null).set("priority", 2),
   * })
   *
   * // Function key — derive from multiple fields:
   * .switchOn((d) => `${d.type}:${d.status}`, {
   *   "PREMIUM:ACTIVE": (m) => m.set("featured", true),
   * })
   */
  switchOn(key: string | ((data: T) => string), cases: SwitchCases<T>): this {
    return this.push((data) => {
      const value =
        typeof key === "function" ? key(data as T) : String(data[key] ?? "");
      const handler = cases[value] ?? cases._;
      if (!handler) return data;
      return handler(this.fresh(data)).get();
    });
  }

  // ── Composition ops ─────────────────────────────────────────────────────────

  /**
   * Apply an external RecordTransform or another Mutator directly.
   * No need to call `.build()` when passing a Mutator.
   * Accepts any Mutator<U> regardless of U — composition is type-safe at runtime.
   *
   * @example
   * .use(sharedCleanup) // RecordTransform
   * .use(otherMutator) // Mutator — no .build() needed
   * .use(otherMutator.build()) // also fine
   */
  use<U extends object>(transform: RecordTransform<U> | Mutator<U>): this {
    const fn = transform instanceof Mutator ? transform.build() : transform;
    return this.push(fn as Op);
  }

  /**
   * Inject an external closure `(m) => m.set(...).omit(...)` into the pipeline.
   * Use this for reusable, named mutation logic defined outside the chain.
   *
   * Difference from `.use()`:
   *   .use(transform) — accepts a built RecordTransform or Mutator (data pipeline)
   *   .apply(fn) — accepts a closure that receives and returns a Mutator
   *
   * @example
   * const addBadge = (m: Mutator<Report>) => m.set("badge", "⭐").omit("trial")
   * mutate(report).apply(addBadge).get()
   *
   * // Curried with context:
   * const withRole = (role: string) => (m: Mutator<User>) => m.set("role", role)
   * mutate(user).apply(withRole("admin")).get()
   */
  apply(fn: (m: Mutator<T>) => Mutator<T>): this {
    return this.push((data) => fn(this.fresh(data)).get());
  }

  /**
   * Apply this Mutator as a transform over each element of an array.
   * Returns a plain Transform suitable for `.transform()` or `.each()`.
   *
   * @example
   * const itemTransform = mutator<Item>().omit("__typename").build();
   * mutate(raw).transform("items", mutator<Item>().omit("__typename").asArray())
   */
  asArray(): Transform {
    const fn = this.build();
    return (arr: unknown): AnyRecord[] =>
      Array.isArray(arr) ? arr.map((item) => fn(item as T)) : [];
  }

  // ── Terminal ────────────────────────────────────────────────────────────────

  /**
   * Execute all operations and return the transformed record.
   * Pass the expected output type to avoid manual casting.
   *
   * @example
   * mutate(raw).set(...).get<AppReport>()
   */
  get<TOut = AnyRecord>(): TOut {
    if (this.source === DEFERRED) {
      throw new Error(
        "Mutator.get() called in deferred mode — use mutate(data) or call build() instead.",
      );
    }
    return this.run(this.source as unknown as AnyRecord) as TOut;
  }

  /**
   * Seal all operations into a reusable RecordTransform function.
   * Safe to call in both immediate and deferred mode.
   */
  build(): RecordTransform<T> {
    const ops = [...this.ops];
    return (data: T): AnyRecord =>
      ops.reduce<AnyRecord>((acc, op) => op(acc), data as unknown as AnyRecord);
  }
}

// ── Factories ─────────────────────────────────────────────────────────────────

/**
 * Immediate mode — data is passed upfront.
 * Chain mutations and call `.get<TOut>()` to retrieve the result.
 *
 * @example
 * const result = mutate(rawData)
 *   .map("department", "departmentId", extract("id"))
 *   .when((d) => d.isAdmin, (m) => m.set("badge", "🔑"))
 *   .omit("__typename")
 *   .get<AppUser>();
 */
export const mutate = <T extends object>(data: T): Mutator<T> =>
  new Mutator<T>(data);

/**
 * Deferred mode — no data yet.
 * Chain mutations and call `.build()` to get a reusable RecordTransform.
 *
 * @example
 * const transform = mutator<Employee>()
 *   .map("department", "departmentId", extract("id"))
 *   .omit("__typename")
 *   .build();
 *
 * const result = transform(rawData);
 * a const list = rawList.map(transform);
 */
export const mutator = <T extends object = AnyRecord>(): Mutator<T> =>
  new Mutator<T>();
