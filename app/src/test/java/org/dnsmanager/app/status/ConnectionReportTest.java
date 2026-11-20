package org.dnsmanager.app.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** What the Connection details page receives ({@link ConnectionReport}). */
public class ConnectionReportTest {

    @Test
    public void theStatusCarriesTheCheckAsCodes() throws Exception {
        DnsCheck.Result check = DnsCheck.parse(200, "{\"protocol\":\"DoT\",\"server_type\":\"private\","
                + "\"device_id\":\"abcd1234\",\"node_location\":\"ams\",\"node_name\":\"ams-01\"}");
        JSONObject o = new JSONObject(ConnectionReport.status(StatusRules.State.SECURE,
                "This phone uses AdGuard DNS over an encrypted connection.", 0xFF198754, false,
                check, true, "abcd1234.d.adguard-dns.com"));
        assertEquals("SECURE", o.getString("state"));
        assertEquals("#198754", o.getString("color"));
        assertFalse(o.getBoolean("checking"));
        JSONObject c = o.getJSONObject("check");
        assertEquals("IN_USE", c.getString("outcome"));
        assertEquals("dot", c.getString("protocol"));
        assertTrue(c.getBoolean("encrypted"));
        assertEquals("private", c.getString("serverType"));
        assertEquals("abcd1234", c.getString("deviceId"));
        assertEquals("ams", c.getString("location"));
        assertEquals("ams-01", c.getString("node"));
        assertTrue(o.getJSONObject("privateDns").getBoolean("active"));
        assertEquals("abcd1234.d.adguard-dns.com", o.getJSONObject("privateDns").getString("host"));
        assertEquals("dns.adguard-dns.com", o.getJSONObject("servers").getString("default"));
    }

    @Test
    public void beforeAnythingIsKnownTheFieldsAreNull() throws Exception {
        JSONObject o = new JSONObject(ConnectionReport.status(null, "Connection status: checking", 0, true,
                null, false, null));
        assertTrue(o.isNull("state"));
        assertTrue(o.isNull("color"));
        assertTrue(o.isNull("check"));
        assertTrue(o.getBoolean("checking"));
        assertTrue(o.getJSONObject("privateDns").isNull("host"));
    }

    @Test
    public void latencyRowsAreMillisecondsAndNoAnswerIsNull() throws Exception {
        JSONObject o = new JSONObject(ConnectionReport.latency(Arrays.asList(
                new Latency.Row("family", 4, 3_449),
                new Latency.Row("default", 6, 23_400),
                new Latency.Row("personal", 4, Latency.NO_ANSWER)), true));
        assertTrue(o.getBoolean("done"));
        assertTrue(o.getBoolean("ipv6"));
        JSONArray rows = o.getJSONArray("rows");
        assertEquals(3, rows.length());
        assertEquals("family", rows.getJSONObject(0).getString("server"));
        assertEquals(4, rows.getJSONObject(0).getInt("ip"));
        assertEquals(3.4, rows.getJSONObject(0).getDouble("ms"), 0);
        assertEquals(23, rows.getJSONObject(1).getDouble("ms"), 0);
        assertTrue(rows.getJSONObject(2).isNull("ms"));
        assertFalse(new JSONObject(ConnectionReport.latency(Collections.emptyList(), false)).getBoolean("ipv6"));
    }

    @Test
    public void numbersAndColoursAreFormattedForThePage() {
        assertEquals(0.5, ConnectionReport.milliseconds(450), 0);
        assertEquals(9.9, ConnectionReport.milliseconds(9_940), 0);
        assertEquals(12, ConnectionReport.milliseconds(12_499), 0);
        assertEquals(13, ConnectionReport.milliseconds(12_500), 0);
        assertEquals("#dc3545", ConnectionReport.hexColor(0xFFDC3545));
        assertEquals("#00ff00", ConnectionReport.hexColor(0xFF00FF00));
    }
}
