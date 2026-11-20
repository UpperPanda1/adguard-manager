package org.dnsmanager.app.downloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Which addresses go to the browser, and data: file names ({@link Downloads}). */
public class DownloadsTest {

    @Test
    public void onlyHttpAndHttpsGoToTheBrowser() {
        assertTrue(Downloads.isWebAddress("https://adguard-dns.io/en/dashboard/"));
        assertTrue(Downloads.isWebAddress(" HTTP://example.com/file "));
        assertFalse(Downloads.isWebAddress("blob:https://adguard-dns.io/123"));
        assertFalse(Downloads.isWebAddress("data:,x"));
        assertFalse(Downloads.isWebAddress("file:///sdcard/x"));
        assertFalse(Downloads.isWebAddress(null));
    }

    @Test
    public void dataFilesGetTheServicesName() {
        assertEquals("AdGuard DNS download.txt", Downloads.fileName("text/plain"));
    }
}
