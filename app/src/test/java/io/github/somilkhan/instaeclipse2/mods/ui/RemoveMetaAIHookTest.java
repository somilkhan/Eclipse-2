package io.github.somilkhan.instaeclipse2.mods.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RemoveMetaAIHookTest {

    @Test public void metaAiOptionsOn457AreFiltered() {
        // MediaOption$Option constants the 457 reel sheet adds row by row.
        assertTrue(RemoveMetaAIHook.isMetaAiOption("GEN_AI_INFO"));
        assertTrue(RemoveMetaAIHook.isMetaAiOption("GEN_AI"));
        assertTrue(RemoveMetaAIHook.isMetaAiOption("CONTENT_DEEP_DIVE"));
    }

    @Test public void regularRowsAreKept() {
        for (String o : new String[]{"SHARE", "DOWNLOAD", "COPY_LINK", "UNSAVE", "PLAYBACK_CONTROLS",
                "AUTO_SCROLL", "REPORT", "DEEP_LINK_MEDIA_TO_BASEL_TEMPLATE_POSTCAP"}) {
            assertFalse(o, RemoveMetaAIHook.isMetaAiOption(o));
        }
    }
}
