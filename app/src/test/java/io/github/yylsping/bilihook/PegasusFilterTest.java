package io.github.yylsping.bilihook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public final class PegasusFilterTest {
    @Test
    public void recognizesFastJsonBooleanSemantics() {
        assertRawAd("is_ad", true);
        assertRawAd("is_ad", 1L);
        assertRawAd("is_ad", "TRUE");
        assertRawAd("is_ad", "1");

        Map<String, Object> item = new HashMap<>();
        item.put("is_ad", 2);
        assertFalse(BiliHook.isPegasusRawJsonAd(item, HashMap.class));
    }

    @Test
    public void recognizesAdInfoAndCardTypeMarkers() {
        assertRawAd("ad_info", new HashMap<>());
        assertRawAd("card_type", "AD");
        assertRawAd("card_type", "Ad_Banner");
        assertRawAd("card_type", "large_AD_card");

        Map<String, Object> item = new HashMap<>();
        item.put("card_type", "thread");
        assertFalse(BiliHook.isPegasusRawJsonAd(item, HashMap.class));
    }

    @Test
    public void returnsNullWhenNoFilteringIsNeeded() {
        List<String> source = Arrays.asList("first", "second");
        assertNull(BiliHook.filterList(source, item -> false));
    }

    @Test
    public void jsonReplacementPreservesOrderAndConcreteArrayType() throws Exception {
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> ad = new HashMap<>();
        ad.put("is_ad", true);
        FakeJsonArray source = new FakeJsonArray();
        source.add(content);
        source.add(ad);
        source.add("tail");
        Constructor<FakeJsonArray> constructor = FakeJsonArray.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        Object result = BiliHook.filterPegasusJsonInput(
                source, constructor,
                item -> BiliHook.isPegasusRawJsonAd(item, HashMap.class));

        assertTrue(result instanceof FakeJsonArray);
        assertEquals(Arrays.asList(content, "tail"), result);
    }

    @Test
    public void recognizesOnlySearchCmCards() throws Exception {
        Method getCardItemCase = FakeSearchItem.class.getDeclaredMethod("getCardItemCase");
        getCardItemCase.setAccessible(true);
        assertTrue(BiliHook.isSearchAd(new FakeSearchItem(SearchCardCase.CM), getCardItemCase));
        assertFalse(BiliHook.isSearchAd(new FakeSearchItem(SearchCardCase.AV), getCardItemCase));
    }

    private static void assertRawAd(String key, Object value) {
        Map<String, Object> item = new HashMap<>();
        item.put(key, value);
        assertTrue(BiliHook.isPegasusRawJsonAd(item, HashMap.class));
    }

    private static final class FakeJsonArray extends ArrayList<Object> {
    }

    private enum SearchCardCase {
        AV,
        CM
    }

    private static final class FakeSearchItem {
        private final SearchCardCase cardCase;

        FakeSearchItem(SearchCardCase cardCase) {
            this.cardCase = cardCase;
        }

        SearchCardCase getCardItemCase() {
            return cardCase;
        }
    }
}
