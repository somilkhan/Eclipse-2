package ps.reso.instaeclipse.utils.core;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class CommonUtilsVariantLabelTest {

    @Test public void labelsKnownVariants() {
        assertEquals("Official", CommonUtils.getVariantLabel("com.instagram.android"));
        assertEquals("Instaflow", CommonUtils.getVariantLabel("com.instaflow.android"));
        assertEquals("Honista", CommonUtils.getVariantLabel("cc.honista.app"));
    }

    @Test public void turkishLocaleDoesNotProduceDotlessI() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("Instander", CommonUtils.getVariantLabel("com.Instander.android"));
            assertEquals("Instagold", CommonUtils.getVariantLabel("com.INSTAGOLD.android"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void malformedNamesDoNotThrow() {
        assertEquals("B", CommonUtils.getVariantLabel("a..b"));
        assertEquals("a.b..c", CommonUtils.getVariantLabel("a.b..c")); // used to throw StringIndexOutOfBounds
        assertEquals("", CommonUtils.getVariantLabel(""));
        assertEquals("", CommonUtils.getVariantLabel(null));
        assertEquals("Single", CommonUtils.getVariantLabel("single"));
    }
}
