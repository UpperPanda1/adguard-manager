package org.dnsmanager.app.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.dnsmanager.app.theme.ContrastTest;
import org.dnsmanager.app.theme.Scheme;
import org.dnsmanager.app.theme.Themes;
import org.junit.Test;

/** What the status icon shows ({@link StatusRules}). */
public class StatusRulesTest {

    @Test
    public void privateDnsOfTheServiceIsSecure() {
        // AdGuard DNS: personal <device id>.d.adguard-dns.com, public dns.adguard-dns.com (research D1, D2).
        assertEquals(StatusRules.State.SECURE, StatusRules.fromPrivateDns(true, "abc123.d.adguard-dns.com"));
        assertEquals(StatusRules.State.SECURE, StatusRules.fromPrivateDns(true, "ABC123.D.ADGUARD-DNS.COM"));
        assertEquals(StatusRules.State.SECURE, StatusRules.fromPrivateDns(true, "dns.adguard-dns.com"));
    }

    @Test
    public void otherPrivateDnsAndOffAreTold() {
        assertEquals(StatusRules.State.OTHER_DNS, StatusRules.fromPrivateDns(true, "dns.google"));
        assertEquals(StatusRules.State.OTHER_DNS, StatusRules.fromPrivateDns(true, "abc123.dns.nextdns.io"));
        assertEquals(StatusRules.State.OTHER_DNS, StatusRules.fromPrivateDns(true, null)); // "automatic"
        assertEquals(StatusRules.State.NOT_PROTECTED, StatusRules.fromPrivateDns(false, "abc.d.adguard-dns.com"));
    }

    @Test
    public void thePrivateDnsMarkerIsASuffixNotAPartOfTheName() {
        // REVIEW.md F3: "contains" let lookalikes count as the service.
        assertEquals(StatusRules.State.OTHER_DNS, StatusRules.fromPrivateDns(true, "adguard-dns.com.evil.com"));
        assertEquals(StatusRules.State.OTHER_DNS, StatusRules.fromPrivateDns(true, "evil-adguard-dns.com"));
        assertEquals(StatusRules.State.OTHER_DNS, StatusRules.fromPrivateDns(true, "adguard-dns.com.example"));
        assertEquals(StatusRules.State.SECURE, StatusRules.fromPrivateDns(true, "adguard-dns.com"));
    }

    @Test
    public void theChecksAnswerSaysInUseAndEncrypted() {
        assertEquals(StatusRules.State.SECURE, StatusRules.fromCheck(inUse("dot")));
        assertEquals(StatusRules.State.SECURE, StatusRules.fromCheck(inUse("doh")));
        assertEquals(StatusRules.State.SECURE, StatusRules.fromCheck(inUse("doq")));
        assertEquals(StatusRules.State.SECURE, StatusRules.fromCheck(inUse("dnscrypt")));
        assertEquals(StatusRules.State.INSECURE, StatusRules.fromCheck(inUse("dns")));
        assertEquals(StatusRules.State.INSECURE, StatusRules.fromCheck(inUse("")));      // protocol missing
        assertEquals(StatusRules.State.NOT_PROTECTED,
                StatusRules.fromCheck(DnsCheck.Result.of(DnsCheck.Outcome.NOT_IN_USE)));
        assertNull(StatusRules.fromCheck(DnsCheck.Result.of(DnsCheck.Outcome.UNKNOWN)));
        assertNull(StatusRules.fromCheck(null));
    }

    @Test
    public void aClearCheckWinsOverPrivateDns() {
        // AdGuard DNS set up on the router: Android sees no Private DNS, the check sees AdGuard.
        assertEquals(StatusRules.State.INSECURE,
                StatusRules.combine(StatusRules.State.NOT_PROTECTED, inUse("dns")));
        assertEquals(StatusRules.State.SECURE,
                StatusRules.combine(StatusRules.State.OTHER_DNS, inUse("doh")));       // e.g. "automatic" mode
        // A green Private DNS state no longer survives a clear "not in use" (REVIEW.md F3).
        assertEquals(StatusRules.State.NOT_PROTECTED,
                StatusRules.combine(StatusRules.State.SECURE, DnsCheck.Result.of(DnsCheck.Outcome.NOT_IN_USE)));
        // ...but "another DNS service" says more than "not protected", so it stays.
        assertEquals(StatusRules.State.OTHER_DNS,
                StatusRules.combine(StatusRules.State.OTHER_DNS, DnsCheck.Result.of(DnsCheck.Outcome.NOT_IN_USE)));
    }

    @Test
    public void withoutAVerdictPrivateDnsStands() {
        for (StatusRules.State privateDns : StatusRules.State.values()) {
            assertEquals(privateDns, StatusRules.combine(privateDns, DnsCheck.Result.of(DnsCheck.Outcome.UNKNOWN)));
            assertEquals(privateDns, StatusRules.combine(privateDns, null));
        }
    }

    private static DnsCheck.Result inUse(String protocol) {
        return new DnsCheck.Result(DnsCheck.Outcome.IN_USE, protocol, "private", "", "", "");
    }

    @Test
    public void iconColoursAreReadableOnTheTopBar() {
        for (boolean dark : new boolean[]{false, true}) {
            int bar = (dark ? Themes.DARK : Themes.LIGHT).get(Scheme.SURFACE);
            for (StatusRules.State state : StatusRules.State.values()) {
                double ratio = ContrastTest.ratio(StatusRules.color(state, dark), bar);
                // 3:1 is WCAG's minimum for icons and other non-text graphics.
                assertTrue(state + (dark ? " dark " : " light ") + ratio, ratio >= 3.0);
            }
        }
    }
}
