package org.dnsmanager.app.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Pattern;

/** AdGuard's DNS check ({@link DnsCheck}): the address, every kind of answer, and real requests to a local server. */
public class DnsCheckTest {

    /** The answer AdGuard's server writes (AdGuardDNS remotekv.go `info`), for a phone on its own server. */
    private static final String OWN_SERVER = "{\"client_ip\":\"203.0.113.7\",\"device_id\":\"abcd1234\","
            + "\"profile_id\":\"p1x2y3\",\"server_group_name\":\"clients\",\"server_name\":\"default_dns\","
            + "\"server_type\":\"private\",\"node_location\":\"ams\",\"node_name\":\"ams-01\","
            + "\"protocol\":\"dot\",\"tls_curve_id\":\"X25519\"}";

    private ServerSocket server;

    @After
    public void stopServer() throws Exception {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Test
    public void everyCheckGetsAFreshAddressAdGuardAccepts() {
        // https://a<16 hex>-dnscheck.adguard-dns.com/dnscheck/test; the random part is 4–63 of a–z A–Z 0–9 -.
        Pattern shape = Pattern.compile("https://a[0-9a-f]{16}-dnscheck\\.adguard-dns\\.com/dnscheck/test");
        Random random = new Random(1);
        String first = DnsCheck.newAddress(random);
        String second = DnsCheck.newAddress(random);
        assertTrue(first, shape.matcher(first).matches());
        assertTrue(second, shape.matcher(second).matches());
        assertNotEquals("a new name each time, so no DNS cache can answer", first, second);
    }

    @Test
    public void anAnswerFromTheOwnServerIsReadCompletely() {
        DnsCheck.Result r = DnsCheck.parse(200, OWN_SERVER);
        assertEquals(DnsCheck.Outcome.IN_USE, r.outcome);
        assertEquals("dot", r.protocol);
        assertTrue(r.encrypted());
        assertEquals("private", r.serverType);
        assertEquals("abcd1234", r.deviceId);
        assertEquals("ams", r.location);
        assertEquals("ams-01", r.node);
    }

    @Test
    public void protocolsAreComparedWithoutCase() {
        for (String p : new String[]{"doh", "DoH", "DOT", "doq", "DNSCrypt"}) {
            assertTrue(p, DnsCheck.parse(200, "{\"protocol\":\"" + p + "\"}").encrypted());
        }
        assertFalse(DnsCheck.parse(200, "{\"protocol\":\"dns\"}").encrypted());
        assertFalse("an unknown word is not encrypted", DnsCheck.parse(200, "{\"protocol\":\"doh4\"}").encrypted());
        assertFalse(DnsCheck.parse(200, "{}").encrypted());
    }

    @Test
    public void aPublicServerHasNoDevice() {
        DnsCheck.Result r = DnsCheck.parse(200, "{\"server_type\":\"public\",\"device_id\":\"\",\"protocol\":\"doh\"}");
        assertEquals(DnsCheck.Outcome.IN_USE, r.outcome);
        assertEquals("public", r.serverType);
        assertEquals("", r.deviceId);
    }

    @Test
    public void valuesThatLookWrongAreDropped() {
        DnsCheck.Result r = DnsCheck.parse(200, "{\"protocol\":\"<b>dot</b>\",\"server_type\":\"admin\","
                + "\"device_id\":\"x y\",\"node_location\":\"<script>\",\"node_name\":null,\"device_id2\":1}");
        assertEquals(DnsCheck.Outcome.IN_USE, r.outcome);
        assertEquals("", r.protocol);
        assertEquals("", r.serverType);
        assertEquals("", r.deviceId);
        assertEquals("", r.location);
        assertEquals("JSON null is not the text \"null\"", "", r.node);
        assertEquals("", DnsCheck.parse(200, "{\"device_id\":123}").deviceId);
    }

    @Test
    public void onlyA200WithJsonMeansInUse() {
        assertEquals(DnsCheck.Outcome.NOT_IN_USE, DnsCheck.parse(404, "404 page not found").outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.parse(429, "rate limited").outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.parse(500, "").outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.parse(200, "<html>Wi-Fi login</html>").outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.parse(200, "").outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.parse(200, "[]").outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.parse(200, null).outcome);
    }

    @Test
    public void aRealRequestIsAnsweredFromALocalServer() throws Exception {
        String url = serve(200, OWN_SERVER);
        DnsCheck.Result r = DnsCheck.fetch(url, 2000);
        assertEquals(DnsCheck.Outcome.IN_USE, r.outcome);
        assertEquals("abcd1234", r.deviceId);
        assertEquals(DnsCheck.Outcome.NOT_IN_USE, DnsCheck.fetch(serve(404, "404 page not found"), 2000).outcome);
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.fetch(serve(429, "rate limited"), 2000).outcome);
    }

    @Test
    public void aHugeAnswerIsNotRead() throws Exception {
        StringBuilder big = new StringBuilder("{\"protocol\":\"dot\",\"x\":\"");
        while (big.length() <= DnsCheck.MAX_ANSWER_BYTES) {
            big.append("0123456789");
        }
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.fetch(serve(200, big + "\"}"), 2000).outcome);
    }

    @Test
    public void aNameThatDoesNotExistMeansNotInUse() {
        // ".invalid" never resolves (RFC 2606): what a phone on another DNS service sees for a check name.
        assertEquals(DnsCheck.Outcome.NOT_IN_USE,
                DnsCheck.fetch("http://a0123456789abcdef-dnscheck.invalid/dnscheck/test", 2000).outcome);
    }

    @Test
    public void aServerThatHangsUpMeansNoVerdict() throws Exception {
        // A server that accepts and closes without answering. (Not "close the server, then connect": the
        // operating system may give that free port to another program meanwhile; this once failed on CI.)
        stopServer();
        ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        server = socket;
        Thread thread = new Thread(() -> {
            while (!socket.isClosed()) {
                try (Socket client = socket.accept()) {
                    // hang up at once
                } catch (Exception e) {
                    return;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        String url = "http://127.0.0.1:" + socket.getLocalPort() + "/dnscheck/test";
        assertEquals(DnsCheck.Outcome.UNKNOWN, DnsCheck.fetch(url, 2000).outcome);
    }

    /**
     * Starts a tiny local HTTP server (plain sockets: unit tests compile against android.jar, which has no
     * JDK web server) that answers every request with {@code code} and {@code body}; returns its address.
     */
    private String serve(int code, String body) throws Exception {
        stopServer();
        ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        server = socket;
        Thread thread = new Thread(() -> {
            while (!socket.isClosed()) {
                try (Socket client = socket.accept()) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
                    String line;
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        // skip the request line and headers
                    }
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    OutputStream out = client.getOutputStream();
                    out.write(("HTTP/1.1 " + code + " X\r\nContent-Type: application/json\r\nContent-Length: "
                            + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                    out.write(bytes);
                    out.flush();
                } catch (Exception e) {
                    return; // the test closed the server
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return "http://127.0.0.1:" + socket.getLocalPort() + "/dnscheck/test";
    }
}
