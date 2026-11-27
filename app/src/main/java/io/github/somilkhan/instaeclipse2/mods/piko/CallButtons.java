package io.github.somilkhan.instaeclipse2.mods.piko;

/** Which DM thread header buttons are call buttons (pure, unit tested). */
public final class CallButtons {

    private CallButtons() {}

    /**
     * 458.0.0.0.57 header button kinds that start or show a call: AUDIO_CALL, VIDEO_CALL,
     * COMBINED_CALL_V1..V4, COLLAPSED_CALL, ONGOING_AUDIO/VIDEO_CALL, AI_AUDIO/VIDEO_CALL,
     * WHATSAPP_AUDIO/VIDEO_CALL. Others (INFO, BLEND, SPEAKER, MORE, ...) are left alone.
     */
    public static boolean isCall(String kind) {
        if (kind == null) return false;
        return kind.endsWith("_CALL") || kind.startsWith("COMBINED_CALL") || kind.equals("COLLAPSED_CALL");
    }
}
