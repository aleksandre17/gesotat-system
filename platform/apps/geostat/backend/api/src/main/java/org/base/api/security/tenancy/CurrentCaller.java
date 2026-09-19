package org.base.api.security.tenancy;

import java.util.function.Supplier;

/**
 * The one injectable port through which governed services learn who is calling. Services take this
 * port instead of an {@code Authentication} parameter, so their public signatures stay stable and
 * they remain unit-testable.
 *
 * <p>A scheduler, worker or audit job has no request caller. It must state that explicitly with
 * {@link #asSystem}; outside such a scope a caller-less thread is {@link Caller.Kind#ANONYMOUS} and
 * is denied, so a missing caller can never become an accidental bypass.
 */
public interface CurrentCaller {

    Caller current();

    /** Runs {@code action} as the explicit SYSTEM caller. {@code reason} names the job, for audit. */
    <T> T asSystem(String reason, Supplier<T> action);

    default void asSystem(String reason, Runnable action) {
        asSystem(reason, () -> {
            action.run();
            return null;
        });
    }
}
