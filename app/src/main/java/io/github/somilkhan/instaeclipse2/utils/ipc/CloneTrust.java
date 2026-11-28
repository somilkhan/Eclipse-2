package io.github.somilkhan.instaeclipse2.utils.ipc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.somilkhan.instaeclipse2.BuildConfig;
import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;

/**
 * Clone apps (Instafel, InstaAlpha, …) are re-signed by their makers, so the official Instagram
 * signer pin cannot cover them. The user approves each clone once in the companion; we then pin
 * the signing key the clone had at that moment. A different app later installed under the same
 * package name (or a re-signed update) no longer matches and is denied until approved again.
 *
 * <p>Stored in the companion's private prefs, never in the synced / world-readable settings
 * cache and never in backups. Only meaningful inside the companion process.
 */
public final class CloneTrust {
    private static final String PREFS = "eclipse2_clone_trust";

    private CloneTrust() { }

    /**
     * Installed supported packages whose signer the user has to approve: every clone, plus
     * com.instagram.android when it is not signed by Meta (LSPatch-patched or unclone builds).
     */
    public static List<String> installedClones(Context context) {
        List<String> out = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try { pm.getPackageInfo(pkg, 0); } catch (Exception ignored) { continue; }
            if (isPinnable(context, pkg)) out.add(pkg);
        }
        return out;
    }

    public static boolean isTrusted(Context context, String pkg) {
        if (!isPinnable(context, pkg) || !inCompanion(context)) return false;
        String pinned = prefs(context).getString(pkg, null);
        if (pinned == null || pinned.length() != 64) return false;
        try {
            return context.getPackageManager().hasSigningCertificate(pkg, hex(pinned), PackageManager.CERT_INPUT_SHA256);
        } catch (RuntimeException e) { return false; }
    }

    /** Pins the clone's current signer. False when it has none or several signers. */
    public static boolean trust(Context context, String pkg) {
        if (!isPinnable(context, pkg) || !inCompanion(context)) return false;
        String sha = signerSha256(context, pkg);
        if (sha == null) return false;
        return prefs(context).edit().putString(pkg, sha).commit();
    }

    public static void untrust(Context context, String pkg) {
        if (inCompanion(context)) prefs(context).edit().remove(pkg).commit();
    }

    /** Lower-case hex SHA-256 of the single current signing certificate, or null. */
    public static String signerSha256(Context context, String pkg) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
            if (pi.signingInfo == null || pi.signingInfo.hasMultipleSigners()) return null;
            Signature[] signers = pi.signingInfo.getApkContentsSigners();
            if (signers == null || signers.length != 1) return null;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    /** "AB:CD:…" for display. */
    public static String pretty(String sha) {
        if (sha == null) return "?";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < sha.length(); i += 2) {
            if (i > 0) sb.append(':');
            sb.append(sha.substring(i, i + 2).toUpperCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /**
     * A supported package whose key can be pinned: any clone, and official Instagram only while it
     * isn't Meta-signed (a Meta-signed install is trusted by its certificate and needs no pin).
     */
    static boolean isPinnable(Context context, String pkg) {
        if (pkg == null || !CommonUtils.SUPPORTED_PACKAGES.contains(pkg)) return false;
        return !BrokerPayload.HOST.equals(pkg) || !CompanionBridge.isOfficialHostSigner(context);
    }

    private static boolean inCompanion(Context context) {
        return context != null && BuildConfig.APPLICATION_ID.equals(context.getPackageName());
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return out;
    }
}
