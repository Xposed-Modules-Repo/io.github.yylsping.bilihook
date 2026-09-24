package io.github.yylsping.bilihook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class RequestBuilderStatusTest {
    @Test
    public void reportsEveryBuilderIndividually() {
        RequestBuilderStatus.Spec required =
                new RequestBuilderStatus.Spec("ugc-v1", "ugc.Builder", true);
        RequestBuilderStatus.Spec optional =
                new RequestBuilderStatus.Spec("pgc-v1", "pgc.Builder", false);
        List<RequestBuilderStatus.Result> results = Arrays.asList(
                new RequestBuilderStatus.Result(required, true),
                new RequestBuilderStatus.Result(optional, false));

        assertEquals("ugc-v1=native(required),pgc-v1=missing(optional)",
                RequestBuilderStatus.summarize(results));
    }

    @Test
    public void onlyMissingRequiredBuildersAreFatal() {
        RequestBuilderStatus.Spec required =
                new RequestBuilderStatus.Spec("ugc-v1", "ugc.Builder", true);
        RequestBuilderStatus.Spec optional =
                new RequestBuilderStatus.Spec("pgc-v1", "pgc.Builder", false);
        List<RequestBuilderStatus.Result> results = Arrays.asList(
                new RequestBuilderStatus.Result(required, false),
                new RequestBuilderStatus.Result(optional, false));

        assertEquals(Arrays.asList("ugc-v1"), RequestBuilderStatus.missingRequired(results));
        assertTrue(RequestBuilderStatus.missingRequired(Arrays.asList(
                new RequestBuilderStatus.Result(required, true),
                new RequestBuilderStatus.Result(optional, false))).isEmpty());
    }
}
