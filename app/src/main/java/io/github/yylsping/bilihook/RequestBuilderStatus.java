package io.github.yylsping.bilihook;

import java.util.ArrayList;
import java.util.List;

/** Exact symbol inventory for the four 7.42.0 playurl request builders. */
final class RequestBuilderStatus {
    static final class Spec {
        final String id;
        final String className;
        final boolean required;

        Spec(String id, String className, boolean required) {
            this.id = id;
            this.className = className;
            this.required = required;
        }
    }

    static final class Result {
        final Spec spec;
        final boolean available;

        Result(Spec spec, boolean available) {
            this.spec = spec;
            this.available = available;
        }

        String summary() {
            return spec.id + "=" + (available ? "native" : "missing")
                    + (spec.required ? "(required)" : "(optional)");
        }
    }

    private RequestBuilderStatus() {}

    static List<String> missingRequired(List<Result> results) {
        List<String> missing = new ArrayList<>();
        for (Result result : results) {
            if (result.spec.required && !result.available) missing.add(result.spec.id);
        }
        return missing;
    }

    static String summarize(List<Result> results) {
        List<String> values = new ArrayList<>();
        for (Result result : results) values.add(result.summary());
        return String.join(",", values);
    }
}
