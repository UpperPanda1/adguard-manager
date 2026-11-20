package org.dnsmanager.app.status;

import org.dnsmanager.app.site.Site;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * AdGuard's "am I using AdGuard DNS?" check, done the way the AdGuard dashboard does it (research D3–D5;
 * AdGuardDNS source {@code internal/dnscheck}). Plain Java (DnsCheckTest), no Android classes.
 *
 * <p><b>How it works.</b> The app makes up a brand-new host name such as
 * {@code a3f9c1…-dnscheck.adguard-dns.com} and asks for {@code https://<that name>/dnscheck/test}:</p>
 * <ol>
 *   <li>To connect, the phone must first look the name up in DNS. <b>Only AdGuard's DNS servers know
 *       these names</b>, and when one is asked, it notes down what it saw (protocol, own or public server,
 *       location) under the random part.</li>
 *   <li>If the lookup went through AdGuard, the request arrives and the page answers with that note as
 *       JSON → {@link Outcome#IN_USE}. If the phone uses another DNS service, the name doesn't exist →
 *       {@link UnknownHostException} → {@link Outcome#NOT_IN_USE}.</li>
 * </ol>
 * <p>A <b>new random part every time</b> means no DNS cache along the way can answer from an old lookup.</p>
 *
 * <p>Everything taken from the answer is checked against a narrow pattern before anyone sees it: the
 * page that shows it (ui/ConnectionWindow) must never receive a surprise from the network.</p>
 */
public final class DnsCheck {

    /** What a check found out. */
    public enum Outcome {
        /** The lookup went through AdGuard DNS (the page answered with JSON). */
        IN_USE,
        /** It didn't: the name couldn't be looked up, or AdGuard never saw it (HTTP 404). */
        NOT_IN_USE,
        /** No verdict: timeout, no network, rate limit (429), server error, or an unreadable answer. */
        UNKNOWN
    }

    /** Same limit as the dashboard's check (it gives up after 10 s). */
    public static final int TIMEOUT_MS = 10_000;
    /** A real answer is about 300 bytes; anything far bigger is not one. */
    static final int MAX_ANSWER_BYTES = 16 * 1024;

    // What a value from the answer may look like. Anything else is dropped (shown as "unknown").
    private static final Pattern WORD = Pattern.compile("[a-z0-9_]{1,16}");          // protocol
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9-]{1,64}");         // device / location
    private static final Pattern NODE = Pattern.compile("[A-Za-z0-9._-]{1,64}");     // server node name

    private static final Random RANDOM = new SecureRandom();

    private DnsCheck() {
        // Only static members.
    }

    /** The result of one check. Every text field is "" when the answer didn't have it (or it looked wrong). */
    public static final class Result {
        public final Outcome outcome;
        /** Lower case: {@code dns}, {@code doh}, {@code dot}, {@code doq}, {@code dnscrypt} (or another word). */
        public final String protocol;
        /** {@code private} = the user's own AdGuard DNS server (account), {@code public} = AdGuard's public one. */
        public final String serverType;
        /** The device the account knows this phone as (own server only). */
        public final String deviceId;
        /** Short code of the AdGuard location that answered (e.g. {@code ams}). */
        public final String location;
        /** Name of the AdGuard server node that answered. */
        public final String node;

        Result(Outcome outcome, String protocol, String serverType, String deviceId, String location, String node) {
            this.outcome = outcome;
            this.protocol = protocol;
            this.serverType = serverType;
            this.deviceId = deviceId;
            this.location = location;
            this.node = node;
        }

        static Result of(Outcome outcome) {
            return new Result(outcome, "", "", "", "", "");
        }

        /** @return true if the protocol AdGuard saw is an encrypted one (site/Site.SECURE_PROTOCOLS). */
        public boolean encrypted() {
            return Site.SECURE_PROTOCOLS.contains(protocol);
        }
    }

    /** Runs one check. <b>Network: call on a background thread only.</b> */
    public static Result run() {
        return fetch(newAddress(RANDOM), TIMEOUT_MS);
    }

    /**
     * @return a fresh check address: {@code https://a<16 hex digits>-dnscheck.adguard-dns.com/dnscheck/test}.
     *         The leading "a" and the length follow the dashboard; AdGuard accepts 4–63 characters of
     *         {@code a–z A–Z 0–9 -} (AdGuardDNS {@code validateRandomID}).
     */
    static String newAddress(Random random) {
        return "https://a" + String.format(Locale.ROOT, "%016x", random.nextLong())
                + "-" + Site.DNS_CHECK_DOMAIN + Site.DNS_CHECK_PATH;
    }

    /** Asks {@code address} and reads the answer. Package-private so tests can use a local server. */
    static Result fetch(String address, int timeoutMs) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false); // AdGuard answers directly; a redirect isn't the check
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return parse(code, null);
            }
            try (InputStream in = connection.getInputStream()) {
                return parse(code, read(in));
            }
        } catch (UnknownHostException e) {
            return Result.of(Outcome.NOT_IN_USE); // only AdGuard DNS knows the name: the phone isn't using it
        } catch (IOException | RuntimeException e) {
            return Result.of(Outcome.UNKNOWN);    // timeout, no route, TLS trouble…: no verdict
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads AdGuard's answer (AdGuardDNS {@code remotekv.go}): 200 + a JSON object = in use; 404 = AdGuard
     * never saw this name; 429 (rate limit), 5xx and anything unreadable = no verdict.
     */
    static Result parse(int httpCode, String body) {
        if (httpCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return Result.of(Outcome.NOT_IN_USE);
        }
        if (httpCode != HttpURLConnection.HTTP_OK || body == null) {
            return Result.of(Outcome.UNKNOWN);
        }
        JSONObject o;
        try {
            o = new JSONObject(body);
        } catch (Exception e) {
            return Result.of(Outcome.UNKNOWN); // e.g. a Wi-Fi login page that answers every address
        }
        String type = text(o, "server_type").toLowerCase(Locale.ROOT);
        return new Result(Outcome.IN_USE,
                clean(text(o, "protocol").toLowerCase(Locale.ROOT), WORD),
                "private".equals(type) || "public".equals(type) ? type : "",
                clean(text(o, "device_id"), ID),
                clean(text(o, "node_location"), ID),
                clean(text(o, "node_name"), NODE));
    }

    /**
     * A text field, or "" when it is missing, JSON {@code null} or not a string. (Android's own
     * {@code optString} would turn {@code null} into the four letters "null", which would pass the id
     * pattern; the computer's org.json used by the tests wouldn't. Checking the type works on both.)
     */
    private static String text(JSONObject o, String key) {
        Object value = o.opt(key);
        return value instanceof String ? (String) value : "";
    }

    private static String clean(String value, Pattern allowed) {
        return allowed.matcher(value).matches() ? value : "";
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
            if (out.size() > MAX_ANSWER_BYTES) {
                return null; // far too big for a real answer
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
