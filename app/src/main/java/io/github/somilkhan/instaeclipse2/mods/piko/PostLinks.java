package io.github.somilkhan.instaeclipse2.mods.piko;

import java.math.BigInteger;

/** Instagram post permalinks (pure, unit tested). */
public final class PostLinks {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final BigInteger SIXTY_FOUR = BigInteger.valueOf(64);

    private PostLinks() {}

    /** Post shortcode from its media pk or "pk_ownerId" id (base64url of the number), "" if invalid. */
    public static String shortcode(String mediaId) {
        if (mediaId == null) return "";
        try {
            String pk = mediaId.contains("_") ? mediaId.substring(0, mediaId.indexOf('_')) : mediaId;
            BigInteger n = new BigInteger(pk.trim());
            if (n.signum() <= 0) return "";
            StringBuilder b = new StringBuilder();
            while (n.signum() > 0) {
                BigInteger[] qr = n.divideAndRemainder(SIXTY_FOUR);
                b.append(ALPHABET.charAt(qr[1].intValue()));
                n = qr[0];
            }
            return b.reverse().toString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    /**
     * https://www.instagram.com/{reel|p}/CODE/ — reel for product_type "clips". Uses the code when
     * known, else derives it from the id; null when neither works.
     */
    public static String permalink(String code, String productType, String mediaId) {
        String c = code != null && !code.trim().isEmpty() ? code.trim() : shortcode(mediaId);
        if (c.isEmpty()) return null;
        return "https://www.instagram.com/" + ("clips".equals(productType) ? "reel/" : "p/") + c + "/";
    }
}
