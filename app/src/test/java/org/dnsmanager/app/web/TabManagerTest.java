package org.dnsmanager.app.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Which saved page a rebuilt screen may reopen ({@link TabManager#isStartUrl}). */
public class TabManagerTest {

    @Test
    public void theServicesHttpsPagesAreAccepted() {
        assertTrue(TabManager.isStartUrl("https://adguard-dns.io/en/dashboard/"));
        assertTrue(TabManager.isStartUrl("https://adguard-dns.io"));
        assertTrue(TabManager.isStartUrl("https://adguard-dns.io?x=1#top"));
        assertTrue(TabManager.isStartUrl("https://auth.adguardaccount.com/login.html")); // the login page
        assertTrue(TabManager.isStartUrl("https://adguardaccount.com/en/account/"));
    }

    @Test
    public void everythingElseFallsBackToTheStartPage() {
        assertFalse(TabManager.isStartUrl(null));
        assertFalse(TabManager.isStartUrl("http://adguard-dns.io/en/dashboard/"));     // not https
        assertFalse(TabManager.isStartUrl("https://example.com/"));
        assertFalse(TabManager.isStartUrl("https://evil-adguard-dns.io/"));
        assertFalse(TabManager.isStartUrl("https://adguard-dns.io.evil.com/"));
        assertFalse(TabManager.isStartUrl("https://adguardcheckout.com/en/welcome.html")); // payment: browser
        assertFalse(TabManager.isStartUrl("https://my.nextdns.io/"));                  // the old service
        assertFalse(TabManager.isStartUrl("https://adguard-dns.io@evil.com/"));       // user-info trick
        assertFalse(TabManager.isStartUrl("https://user:pw@adguard-dns.io/"));
        assertFalse(TabManager.isStartUrl("https://adguard-dns.io:8443/"));            // another port
        assertFalse(TabManager.isStartUrl("javascript:alert(1)"));
        assertFalse(TabManager.isStartUrl("about:blank"));
    }
}
