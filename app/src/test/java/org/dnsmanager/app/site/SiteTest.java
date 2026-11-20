package org.dnsmanager.app.site;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

/** Which pages belong to the service ({@link Site}): AdGuard DNS. */
public class SiteTest {

    @Test
    public void theDashboardAndTheLoginAreOwned() {
        assertTrue(Site.ownsHost("adguard-dns.io"));
        assertTrue(Site.ownsHost("ADGUARD-DNS.IO"));            // host names ignore case
        assertTrue(Site.ownsHost("auth.adguardaccount.com"));   // the login (research F1/F2)
        assertTrue(Site.ownsHost("adguardaccount.com"));        // "Manage account" (F7)
    }

    @Test
    public void lookalikesAndOtherAdGuardSitesAreNot() {
        assertFalse(Site.ownsHost("evil-adguard-dns.io"));          // no "." before the domain
        assertFalse(Site.ownsHost("eviladguard-dns.io"));
        assertFalse(Site.ownsHost("adguard-dns.io.evil.com"));      // the domain is not the end
        assertFalse(Site.ownsHost("adguardaccount.com.evil.com"));
        assertFalse(Site.ownsHost("adguardcheckout.com"));          // payment opens the browser (owner Q4)
        assertFalse(Site.ownsHost("adguard.com"));                  // AdGuard's other products
        assertFalse(Site.ownsHost("adguard-dns.com"));              // DNS server names, not a website
        assertFalse(Site.ownsHost("adguard-dns.io."));              // trailing dot: not accepted
        assertFalse(Site.ownsHost("my.nextdns.io"));                // the old service
        assertFalse(Site.ownsHost(""));
        assertFalse(Site.ownsHost(null));
    }

    @Test
    public void theDashboardAndTheLegalPagesAreHttpsOnTheServicesOwnSite() {
        assertTrue(Site.HOME_URL.startsWith("https://"));
        assertTrue(Site.ownsHost(hostOf(Site.HOME_URL)));
        assertEquals(2, Site.LINKS.size());
        for (String url : Site.LINKS.values()) {
            assertTrue(url, url.startsWith("https://"));
            assertEquals(url, "adguard-dns.io", hostOf(url));
        }
    }

    @Test
    public void privateDnsHostsAreTheServicesDomainAndItsSubdomainsOnly() {
        assertTrue(Site.isPrivateDnsHost("abc123.d.adguard-dns.com"));
        assertTrue(Site.isPrivateDnsHost("DNS.ADGUARD-DNS.COM"));
        assertTrue(Site.isPrivateDnsHost("adguard-dns.com"));
        assertFalse(Site.isPrivateDnsHost("adguard-dns.com.evil.com"));
        assertFalse(Site.isPrivateDnsHost("evil-adguard-dns.com"));
        assertFalse(Site.isPrivateDnsHost("dns.google"));
        assertFalse(Site.isPrivateDnsHost(null));
    }

    @Test
    public void theLatencyServersAreAdGuardDnsHosts() {
        assertEquals(4, Site.LATENCY_SERVERS.size());
        for (String host : Site.LATENCY_SERVERS.values()) {
            assertTrue(host, Site.isPrivateDnsHost(host));
        }
    }

    @Test
    public void scriptRulesCoverBothDomainsAndTheirSubdomainsOverHttpsOnly() {
        assertEquals(new LinkedHashSet<>(Arrays.asList(
                        "https://adguard-dns.io", "https://*.adguard-dns.io",
                        "https://adguardaccount.com", "https://*.adguardaccount.com")),
                Site.originRules());
    }

    /** "https://host/path" → "host" (the fixed addresses all have a path). */
    private static String hostOf(String url) {
        return url.substring(8, url.indexOf('/', 8));
    }
}
