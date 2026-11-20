package org.dnsmanager.app.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The latency test ({@link Latency}): best-of rule, address choice, real handshakes with a local server, display order. */
public class LatencyTest {

    private ServerSocket server;

    @After
    public void stopServer() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Test
    public void theBestAnsweredTryCounts() {
        assertEquals(3, Latency.best(new long[]{5, 3, 9}));
        assertEquals(4, Latency.best(new long[]{Latency.NO_ANSWER, 4, Latency.NO_ANSWER}));
        assertEquals(0, Latency.best(new long[]{0, 7}));
        assertEquals(Latency.NO_ANSWER, Latency.best(new long[]{Latency.NO_ANSWER, Latency.NO_ANSWER}));
    }

    @Test
    public void theAddressOfTheRightIpVersionIsPicked() throws Exception {
        InetAddress v4 = InetAddress.getByName("94.140.14.14");
        InetAddress v6 = InetAddress.getByName("2a10:50c0::ad1:ff");
        InetAddress[] both = {v6, v4};
        assertSame(v4, Latency.pick(both, 4));
        assertSame(v6, Latency.pick(both, 6));
        assertNull(Latency.pick(new InetAddress[]{v4}, 6));
        assertNull(Latency.pick(new InetAddress[0], 4));
    }

    @Test
    public void aRealHandshakeIsTimedAndARefusedOneIsNot() throws Exception {
        int port = listen();
        long micros = Latency.handshakeMicros(InetAddress.getLoopbackAddress(), port, 2000);
        assertTrue("answered: " + micros, micros >= 0 && micros < 2_000_000);
        // Port 0 can't be listened on, so the handshake is always refused. (Not "close the server, then
        // connect": the operating system may give that free port to another program meanwhile.)
        assertEquals(Latency.NO_ANSWER, Latency.handshakeMicros(InetAddress.getLoopbackAddress(), 0, 2000));
    }

    @Test
    public void everyServerIsMeasuredOverBothIpVersionsAndReportedAsItArrives() throws Exception {
        int port = listen();
        Map<String, String> servers = new LinkedHashMap<>();
        servers.put("default", "here.test");
        servers.put("family", "nowhere.test");
        Latency.Resolver resolver = host -> {
            if (host.equals("here.test")) {
                return new InetAddress[]{InetAddress.getByName("127.0.0.1")}; // IPv4 only
            }
            throw new UnknownHostException(host);
        };
        List<Latency.Row> seen = new ArrayList<>();
        List<Latency.Row> rows = Latency.measure(servers, resolver, port, () -> false, seen::add);
        assertEquals(4, rows.size());
        assertEquals(rows, seen);
        assertEquals("default", rows.get(0).server);
        assertEquals(4, rows.get(0).ipVersion);
        assertTrue(rows.get(0).answered());
        assertFalse("no IPv6 address", rows.get(1).answered());
        assertFalse("no address at all", rows.get(2).answered());
        assertFalse(rows.get(3).answered());
    }

    @Test
    public void closingTheWindowStopsTheMeasuring() throws Exception {
        int port = listen();
        Map<String, String> servers = new LinkedHashMap<>();
        servers.put("default", "127.0.0.1");
        servers.put("family", "127.0.0.1");
        List<Latency.Row> rows = Latency.measure(servers, Latency.SYSTEM, port, () -> true, r -> { });
        assertTrue(rows.isEmpty());
    }

    @Test
    public void theListShowsFastestFirstAndNoAnswerLast() {
        List<Latency.Row> rows = Arrays.asList(
                new Latency.Row("default", 4, 30_000),
                new Latency.Row("default", 6, Latency.NO_ANSWER),
                new Latency.Row("family", 4, 12_000),
                new Latency.Row("family", 6, 25_000),
                new Latency.Row("personal", 4, Latency.NO_ANSWER));
        List<Latency.Row> shown = Latency.forDisplay(rows);
        assertEquals(5, shown.size());
        assertEquals(12_000, shown.get(0).micros);
        assertEquals(25_000, shown.get(1).micros);
        assertEquals(30_000, shown.get(2).micros);
        assertFalse(shown.get(3).answered());
        assertEquals("equal places keep the measuring order", "default", shown.get(3).server);
        assertEquals("personal", shown.get(4).server);
    }

    @Test
    public void withoutAnyIpv6AnswerTheIpv6RowsAreLeftOut() {
        List<Latency.Row> rows = Arrays.asList(
                new Latency.Row("default", 4, 30_000),
                new Latency.Row("default", 6, Latency.NO_ANSWER),
                new Latency.Row("family", 6, Latency.NO_ANSWER));
        List<Latency.Row> shown = Latency.forDisplay(rows);
        assertEquals(1, shown.size());
        assertEquals(4, shown.get(0).ipVersion);
    }

    /** A local server that accepts connections (the operating system answers the handshake by itself). */
    private int listen() throws Exception {
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        return server.getLocalPort();
    }
}
