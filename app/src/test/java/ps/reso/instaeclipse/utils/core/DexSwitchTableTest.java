package ps.reso.instaeclipse.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * Fixture: app/src/test/resources/dex/string_table_fixture.dex, built by d8 (min-api 28) from
 * <pre>
 * package fx;
 * public final class Table {
 *   public static String A00(int i) { switch (i) { case 0: return "alpha"; case 1: return "beta";
 *     case 2: return "high_quality_media_upload"; case 3: return "gamma"; case 4: return "delta";
 *     case 5: return "beta"; default: return null; } }
 *   public static String sparse(int i) { switch (i) { case 7: return "x_one"; case 900: return "x_two";
 *     case 100000: return "high_quality_media_upload"; default: return ""; } }
 *   public static boolean user(int a) { return A00(2).length() > a; }
 * }
 * </pre>
 * d8 emits a packed-switch for A00 (with "beta" hoisted into a shared register) and a
 * sparse-switch for sparse — the same two shapes Instagram's string tables use.
 */
public class DexSwitchTableTest {

    private static byte[] dex() throws Exception {
        try (InputStream in = DexSwitchTableTest.class.getResourceAsStream("/dex/string_table_fixture.dex")) {
            return in.readAllBytes();
        }
    }

    private static final String SIG = "(I)Ljava/lang/String;";

    @Test
    public void packedSwitchCaseIndex() throws Exception {
        assertEquals(Collections.singletonList(2),
                DexSwitchTable.caseKeysForString(dex(), "fx.Table", "A00", SIG, "high_quality_media_upload"));
        assertEquals(Collections.singletonList(0), DexSwitchTable.caseKeysForString(dex(), "fx.Table", "A00", SIG, "alpha"));
        assertEquals(Collections.singletonList(4), DexSwitchTable.caseKeysForString(dex(), "Lfx/Table;", "A00", SIG, "delta"));
    }

    @Test
    public void hoistedSharedRegister() throws Exception {
        assertEquals(Arrays.asList(1, 5), DexSwitchTable.caseKeysForString(dex(), "fx.Table", "A00", SIG, "beta"));
    }

    @Test
    public void sparseSwitchCaseKey() throws Exception {
        assertEquals(Collections.singletonList(100000),
                DexSwitchTable.caseKeysForString(dex(), "fx.Table", "sparse", SIG, "high_quality_media_upload"));
        assertEquals(Collections.singletonList(900), DexSwitchTable.caseKeysForString(dex(), "fx.Table", "sparse", SIG, "x_two"));
    }

    @Test
    public void missingThingsGiveEmpty() throws Exception {
        assertTrue(DexSwitchTable.caseKeysForString(dex(), "fx.Table", "A00", SIG, "not_in_dex").isEmpty());
        assertTrue(DexSwitchTable.caseKeysForString(dex(), "fx.Table", "A00", SIG, "x_one").isEmpty());
        assertTrue(DexSwitchTable.caseKeysForString(dex(), "fx.Nope", "A00", SIG, "alpha").isEmpty());
        assertTrue(DexSwitchTable.caseKeysForString(dex(), "fx.Table", "A01", SIG, "alpha").isEmpty());
    }

    @Test
    public void instructionWidths() {
        assertEquals(1, DexSwitchTable.width(0x12)); // const/4
        assertEquals(2, DexSwitchTable.width(0x13)); // const/16
        assertEquals(3, DexSwitchTable.width(0x1B)); // const-string/jumbo
        assertEquals(5, DexSwitchTable.width(0x18)); // const-wide
        assertEquals(3, DexSwitchTable.width(0x6E)); // invoke-virtual
        assertEquals(4, DexSwitchTable.width(0xFA)); // invoke-polymorphic
    }
}
