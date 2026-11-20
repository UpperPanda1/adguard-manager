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
 * <p><b>Still NextDNS on purpose</b> (replaced by Phase 7, see docs/PHASES.md): {@link #PING_URL},
 * {@link #PING_TEST_URL} and {@link #PING_DOMAINS}.</p>
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
     * The speed test page (three-dots menu → Ping test). <b>Still NextDNS's until Phase 7</b>:
     * AdGuard has no such page (research D7); the owner decides what the window becomes (Q2).
     */
    public static final String PING_URL = "https://ping.nextdns.io/";

    /** The Ping window's lower page (NextDNS's connection test). <b>Removed in Phase 7.</b> */
    public static final String PING_TEST_URL = "https://test.nextdns.io/";

    /**
     * Extra domains whose pages may stay inside the Ping window (and only there; never in an
     * account tab). Needed while that window still shows NextDNS's two pages: {@link #PING_TEST_URL}
     * redirects to a random subdomain ({@code <random>.test.nextdns.io}, checked 2026-11-20), and
     * without this the redirect would be sent to the browser. <b>Removed in Phase 7.</b>
     */
    public static final List<String> PING_DOMAINS = Collections.singletonList("nextdns.io");

    /**
     * Android's Private DNS host names that belong to the service: this domain or any name under it.
     * Personal servers are {@code <device id>.d.adguard-dns.com}, public ones {@code dns.}, {@code
     * family.}, {@code unfiltered.adguard-dns.com} (research D1, D2). Checked by {@link #isPrivateDnsHost}.
     */
    public static final String PRIVATE_DNS_DOMAIN = "adguard-dns.com";

    /**
     * AdGuard's "am I using AdGuard DNS?" check (research D3–D5), the same one the dashboard runs. The
     * address is {@code https://a<random>-}{@value #DNS_CHECK_DOMAIN}{@value #DNS_CHECK_PATH}, with a new
     * random part every time. <b>Only AdGuard's DNS servers know such a name</b>: if the phone's DNS
     * goes through AdGuard, the name resolves and the page answers with what AdGuard saw (as JSON);
     * otherwise the name can't even be looked up. Built and read by status/DnsCheck.
     */
    public static final String DNS_CHECK_DOMAIN = "dnscheck.adguard-dns.com";
    public static final String DNS_CHECK_PATH = "/dnscheck/test";

    /**
     * Values of the check's {@code protocol} field that are encrypted. AdGuard's server writes
     * {@code dns}, {@code doh}, {@code dot}, {@code doq} or {@code dnscrypt} (AdGuardDNS
     * {@code dnsserver/protocol.go}); compared without case. DNSCrypt counts: it encrypts and signs
     * every query (only an app can use it on Android). Plain {@code dns}, or a word not listed here,
     * is "not encrypted", so a new protocol name can never turn the icon green by accident.
     */
    public static final List<String> SECURE_PROTOCOLS = Collections.unmodifiableList(
            Arrays.asList("doh", "dot", "doq", "dnscrypt"));

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
     * @return true if Android's Private DNS host name {@code host} is one of the service's servers.
     *         A suffix rule, not "contains": "abc.d.adguard-dns.com" yes, "adguard-dns.com.evil.com"
     *         and "evil-adguard-dns.com" no (REVIEW.md F3).
     */
    public static boolean isPrivateDnsHost(String host) {
        return matches(host, Collections.singletonList(PRIVATE_DNS_DOMAIN));
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
