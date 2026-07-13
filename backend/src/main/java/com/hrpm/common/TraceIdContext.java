package com.hrpm.common;

public final class TraceIdContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TraceIdContext() {
    }

    public static String current() {
        return CURRENT.get();
    }

    public static void set(String traceId) {
        CURRENT.set(traceId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
