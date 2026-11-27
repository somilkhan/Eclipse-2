package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.instagram.feed.media.Media;

import org.junit.Test;

public class ReelDownloadHookTest {

    /** 457 clips item (X.075A): Media sits in an obfuscated field. */
    static class ClipsItem { Object other = "x"; Media media; }
    static class SubItem extends ClipsItem { }

    @Test public void mediaItselfIsReturned() {
        Media m = new Media();
        assertSame(m, ReelDownloadHook.findMedia(m));
    }

    @Test public void mediaFieldOfModelIsFound() {
        ClipsItem item = new ClipsItem();
        item.media = new Media();
        assertSame(item.media, ReelDownloadHook.findMedia(item));
    }

    @Test public void inheritedMediaFieldIsFound() {
        SubItem item = new SubItem();
        item.media = new Media();
        assertSame(item.media, ReelDownloadHook.findMedia(item));
    }

    @Test public void nullOrNoMedia() {
        assertNull(ReelDownloadHook.findMedia(null));
        assertNull(ReelDownloadHook.findMedia(new ClipsItem()));
        assertNull(ReelDownloadHook.findMedia("not a model"));
    }
}
