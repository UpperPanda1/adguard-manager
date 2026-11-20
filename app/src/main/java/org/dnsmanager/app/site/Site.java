package org.dnsmanager.app.site;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Everything the app knows about the DNS service it manages: <b>AdGuard DNS</b>. This is the one
 * file that names the service; the rest of the code asks this class (CLAUDE.md §6). The facts below
 * were checked on the live website; each names its row in
 * {@code docs/research/2026-11-19-adguard-live-probe.md} (re-checked 2026-11-20).
 *
 * <p>Plain Java (no Android classes), so SiteTest checks the rules on the computer.</p>
 *
 * <p><b>Still NextDNS on purpose</b> (each replaced by a later phase, see docs/PHASES.md):
 * {@link #TEST_URL} and the {@code TEST_*}/{@link #SECURE_PROTOCOLS} fields (Phase 6, AdGuard's DNS
 * check), {@link #PING_URL} and {@link #PING_DOMAINS} (Phase 7, owner question Q2).</p>
 */
public final class Site {

    /** The service's name as shown to the user ("This phone uses AdGuard DNS…"). */
    public static final String NAME = "AdGuard DNS";

    /**
     * The dashboard: the page every account opens, and where the home button goes. Signed out, it
     * sends the page on to AdGuard's login (research F1:
     * {@code adguard-dns.io/en/dashboard/ → auth.adguardaccount.com/oauth/authorize → /login.html}),
     * which comes back to {@code adguard-dns.io/oauth/callback.html} after the login.
     */
    public static final String HOME_URL = "https://adguard-dns.io/en/dashboard/";

    /**
     * Domains whose pages open inside the app (each one including all its subdomains). Any other
     * link is handed to the phone's browser, so no other website runs in an account's container.
     * <ul>
     *   <li>{@code adguard-dns.io}: the dashboard itself (and AdGuard DNS's public pages).</li>
     *   <li>{@code adguardaccount.com}: AdGuard's login ({@code auth.adguardaccount.com}, research
     *       F1/F2) and the "Manage account" pages (F7). The login must stay inside the account's
     *       container, or its cookies would land in the browser and the app would never be signed in.</li>
     * </ul>
     * Deliberately <b>not</b> here: {@code adguardcheckout.com} (payment, owner Q4: the phone's
     * browser handles money, not the app), {@code adguard.com} (AdGuard's other products; the login
     * page's "Terms and conditions" link goes there) and {@code adguard-dns.com} (the DNS servers'
     * names, not a website). Apple, Google and Facebook sign-in pages come in Phase 5, as a separate
     * exact-host list that only main-frame navigations may use.
     */
    public static final List<String> DOMAINS = Collections.unmodifiableList(
            Arrays.asList("adguard-dns.io", "adguardaccount.com"));

    /**
     * Answers with a small JSON text that says whether this phone's DNS goes through the service.
     * <b>Still NextDNS's test until Phase 6</b> (AdGuard's own check works differently: research D4,
     * D5). Meanwhile it answers "unconfigured" for a phone on AdGuard DNS, which the app reads as "no
     * answer", so the icon goes by the Private DNS setting and {@link #PRIVATE_DNS_MARKER} alone.
     */
    public static final String TEST_URL = "https://test.nextdns.io/";

    /**
     * The speed test page (three-dots menu → Ping test). <b>Still NextDNS's until Phase 7</b>:
     * AdGuard has no such page (research D7); the owner decides what the window becomes (Q2).
     */
    public static final String PING_URL = "https://ping.nextdns.io/";

    /**
     * Extra domains whose pages may stay inside the Ping window (and only there; never in an
     * account tab). Needed while that window still shows NextDNS's two pages: {@link #TEST_URL}
     * redirects to a random subdomain ({@code <random>.test.nextdns.io}, checked 2026-11-20), and
     * without this the redirect would be sent to the browser. <b>Removed in Phase 7.</b>
     */
    public static final List<String> PING_DOMAINS = Collections.singletonList("nextdns.io");

    /**
     * A Private DNS host name containing this text belongs to the service: personal servers are
     * {@code <device id>.d.adguard-dns.com}, public ones {@code dns.}/{@code family.}/{@code
     * unfiltered.adguard-dns.com} (research D1, D2). Phase 6 turns the "contains" into a proper
     * suffix rule (REVIEW.md F3).
     */
    public static final String PRIVATE_DNS_MARKER = "adguard-dns.com";

    /** TEST_URL's JSON: field that says whether the service is in use, and the value that means yes. (Phase 6) */
    public static final String TEST_STATUS_FIELD = "status";
    public static final String TEST_STATUS_IN_USE = "ok";

    /** TEST_URL's JSON: field with the DNS protocol, and the values that are encrypted. (Phase 6) */
    public static final String TEST_PROTOCOL_FIELD = "protocol";
    public static final List<String> SECURE_PROTOCOLS = Arrays.asList("DOH", "DOT", "DOQ", "DOH3");

    /**
     * Web pages the Settings screen may open in the browser, by name. The settings page only ever
     * sends the name, never an address, so it can't be used to open something else. Both are the
     * addresses AdGuard DNS's own website footer links (research L1).
     */
    public static final Map<String, String> LINKS;

    static {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("service_privacy", "https://adguard-dns.io/en/privacy.html");
        links.put("service_terms", "https://adguard-dns.io/en/eula.html");
        LINKS = Collections.unmodifiableMap(links);
    }

    private Site() {
        // Only static members.
    }

    /**
     * @return true if a page on {@code host} belongs to the service and may load inside the app.
     *         Exact match or a real subdomain: "auth.adguardaccount.com" yes,
     *         "evil-adguard-dns.io" and "adguard-dns.io.evil.com" no.
     */
    public static boolean ownsHost(String host) {
        return matches(host, DOMAINS);
    }

    /**
     * @return true if a page on {@code host} may stay inside the Ping window: the service's own
     *         pages, or the temporary {@link #PING_DOMAINS}. Phase 7 removes this.
     */
    public static boolean pingWindowAllows(String host) {
        return ownsHost(host) || matches(host, PING_DOMAINS);
    }

    /**
     * @return the "origin rules" for scripts the app adds to the service's pages: each domain and
     *         its subdomains, https only. Format: androidx.webkit's addDocumentStartJavaScript.
     */
    public static Set<String> originRules() {
        Set<String> rules = new LinkedHashSet<>();
        for (String domain : DOMAINS) {
            rules.add("https://" + domain);
            rules.add("https://*." + domain);
        }
        return rules;
    }

    /**
     * The one host rule: {@code host} is one of {@code domains} or a real subdomain of one. Written
     * as {@code equals(d) || endsWith("." + d)}, never {@code contains}/{@code endsWith(d)}, so a
     * lookalike such as "evil-adguard-dns.io" can't pass (CLAUDE.md §7).
     */
    private static boolean matches(String host, List<String> domains) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        for (String domain : domains) {
            if (h.equals(domain) || h.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }
}
