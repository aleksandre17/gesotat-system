export const throttleWithTrailing = <
  T extends (...args: Parameters<T>) => void,
>(
  func: T,
  limit: number,
): T => {
  let lastCallTime = 0;
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  let lastArgs: Parameters<T> | null = null;
  let lastContext: unknown = null;

  return function (this: unknown, ...args: Parameters<T>) {
    const now = Date.now();
    const remaining = limit - (now - lastCallTime);
    lastArgs = args;
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    lastContext = this;

    if (remaining <= 0) {
      if (timeoutId) {
        clearTimeout(timeoutId);
        timeoutId = null;
      }
      lastCallTime = now;
      func.apply(lastContext, args);
    } else if (!timeoutId) {
      timeoutId = setTimeout(() => {
        timeoutId = null;
        lastCallTime = Date.now();
        if (lastArgs) func.apply(lastContext, lastArgs);
        lastArgs = null;
        lastContext = null;
      }, remaining);
    }
  } as T;
};