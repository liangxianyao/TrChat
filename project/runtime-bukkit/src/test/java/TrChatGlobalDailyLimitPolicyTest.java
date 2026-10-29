import me.arasple.mc.trchat.module.display.channel.obj.DailyLimitSettings;
import org.junit.Test;

import java.time.ZoneId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TrChatGlobalDailyLimitPolicyTest {
    private DailyLimitSettings settings() {
        Map<String, Integer> groups = new HashMap<>();
        groups.put("vip1", 20);
        groups.put("vip2", 30);
        groups.put("vip3", 40);
        groups.put("moderator", -1);
        groups.put("admin", -1);
        return new DailyLimitSettings(10, groups, ZoneId.of("Asia/Shanghai"));
    }

    @Test public void resolvesNormalAndVipGroups() {
        assertEquals(10, settings().limitFor("default"));
        assertEquals(20, settings().limitFor("VIP1"));
        assertEquals(30, settings().limitFor("vip2"));
        assertEquals(40, settings().limitFor("vip3"));
    }

    @Test public void unlimitedAndMissingGroups() {
        assertEquals(-1, settings().limitFor("admin"));
        assertEquals(-1, settings().limitFor("moderator"));
        assertEquals(10, settings().limitFor(null));
        assertEquals(10, settings().limitFor("unknown"));
    }

    @Test public void resetsAtShanghaiMidnight() {
        Clock before = Clock.fixed(Instant.parse("2026-10-29T15:59:59Z"), ZoneId.of("UTC"));
        Clock after = Clock.fixed(Instant.parse("2026-10-29T16:00:00Z"), ZoneId.of("UTC"));
        assertEquals(LocalDate.of(2026, 10, 29), settings().date(before));
        assertEquals(LocalDate.of(2026, 10, 30), settings().date(after));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidDefault() {
        new DailyLimitSettings(-2, new HashMap<>(), ZoneId.of("Asia/Shanghai"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidGroupLimit() {
        Map<String, Integer> groups = new HashMap<>();
        groups.put("vip1", -2);
        new DailyLimitSettings(10, groups, ZoneId.of("Asia/Shanghai"));
    }
}
