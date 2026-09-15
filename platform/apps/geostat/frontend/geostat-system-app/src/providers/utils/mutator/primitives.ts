/**
 * Pure, curried primitive functions.
 * No classes, no state — safe to use anywhere.
 */

import type { Transform, AnyRecord, Predicate } from "./types";

// ── Value ─────────────────────────────────────────────────────────────────────

export const identity: Transform = (v) => v;
export const constant =
  (value: unknown): Transform =>
  () =>
    value;
export const maybe =
  (fn: Transform): Transform =>
  (v) =>
    v != null ? fn(v) : null;
export const guard =
  (predicate: Predicate, fallback: unknown = null): Transform =>
  (v) =>
    predicate(v) ? v : fallback;
export const when =
  (predicate: Predicate, fn: Transform): Transform =>
  (v) =>
    predicate(v) ? fn(v) : v;
export const unless =
  (predicate: Predicate, fn: Transform): Transform =>
  (v) =>
    !predicate(v) ? fn(v) : v;

// ── Nested object ─────────────────────────────────────────────────────────────

/** `extract("id")({ id: 5, name: "x" })` → `5` */
export const extract =
  (key: string): Transform =>
  (nested: unknown): unknown =>
    (nested as Record<string, unknown>)?.[key] ?? null;

/** `wrap("id")(5)` → `{ id: 5 }` */
export const wrap =
  (key: string): Transform =>
  (value: unknown): Record<string, unknown> => ({ [key]: value });

// ── Array ─────────────────────────────────────────────────────────────────────

/** `extractMany("id")([{id:1},{id:2}])` → `[1,2]` */
export const extractMany =
  (key: string): Transform =>
  (arr: unknown): unknown[] =>
    Array.isArray(arr)
      ? arr.map((item) => (item as Record<string, unknown>)?.[key] ?? null)
      : [];

/** `wrapMany("id")([1,2])` → `[{id:1},{id:2}]` */
export const wrapMany =
  (key: string): Transform =>
  (arr: unknown): Record<string, unknown>[] =>
    Array.isArray(arr) ? arr.map((v) => ({ [key]: v })) : [];

/** Apply transform to every element. */
export const arrayOf =
  (fn: Transform): Transform =>
  (arr: unknown): unknown[] =>
    Array.isArray(arr) ? arr.map(fn) : [];

/** Remove null/undefined from array. */
export const compact: Transform = (arr: unknown): unknown[] =>
  Array.isArray(arr) ? arr.filter((v) => v != null) : [];

// ── Composition ───────────────────────────────────────────────────────────────

/** Left-to-right: `pipe(extract("value"), toNumber)` */
export const pipe =
  (...fns: Transform[]): Transform =>
  (value: unknown): unknown =>
    fns.reduce((acc, fn) => fn(acc), value);

/** Right-to-left: `compose(toNumber, extract("value"))` */
export const compose =
  (...fns: Transform[]): Transform =>
  (value: unknown): unknown =>
    fns.reduceRight((acc, fn) => fn(acc), value);

// ── Record-level ──────────────────────────────────────────────────────────────

export const omit =
  (...keys: string[]) =>
  (data: AnyRecord): AnyRecord => {
    const r = { ...data };
    for (const k of keys) delete r[k];
    return r;
  };

export const pick =
  (...keys: string[]) =>
  (data: AnyRecord): AnyRecord =>
    Object.fromEntries(keys.filter((k) => k in data).map((k) => [k, data[k]]));

export const defaults =
  (obj: AnyRecord) =>
  (data: AnyRecord): AnyRecord => ({ ...obj, ...data });

export const tap =
  (fn: (data: AnyRecord) => void) =>
  (data: AnyRecord): AnyRecord => {
    fn(data);
    return data;
  };

// ── Coercions ─────────────────────────────────────────────────────────────────

export const toNumber = (v: unknown): number => Number(v);
export const toString = (v: unknown): string => String(v);
export const toBoolean = (v: unknown): boolean => Boolean(v);
export const toDate = (v: unknown): Date => new Date(v as string);
export const toISO = (v: unknown): string =>
  v instanceof Date ? v.toISOString() : String(v);
