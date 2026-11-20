package org.dnsmanager.app.status;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * How fast AdGuard's DNS servers answer this phone (Connection details → Latency). Plain Java (LatencyTest).
 *
 * <p><b>How it measures.</b> A real "ping" sends ICMP packets, which Android apps may not do without root.
 * The usual stand-in is timing a <b>TCP handshake</b>: {@code connect()} returns when the server's answer
 * to the first packet arrives, which is exactly one round trip (phone → server → phone). The connection is
 * closed at once; nothing is sent. Each address is tried {@link #TRIES} times and the <b>best</b> time
 * counts (the others include random delays on the way, as with ping's "min").
 * NextDNS's ping page does the same idea from the other side: each server reports the round trip it measured.</p>
 *
 * <p>Port 443 (HTTPS, DNS-over-HTTPS) because almost every network lets it through; the time to an
 * address doesn't depend on the port. IPv4 and IPv6 are measured separately.</p>
 *
 * <p><b>Network: run {@link #measure} on a background thread only.</b></p>
 */
public final class Latency {

    public static final int PORT = 443;
    public static final int TRIES = 3;
    /** Per attempt, as NextDNS's ping page (2 s). */
    public static final int TIMEOUT_MS = 2000;
    /** "No answer" in {@link Row#micros}. */
    public static final long NO_ANSWER = -1;

    private Latency() {
        // Only static members.
    }

    /** One measured address: a server name (site/Site.LATENCY_SERVERS key) over IPv4 or IPv6. */
    public static final class Row {
        public final String server;
        public final int ipVersion;
        /** Microseconds of the best handshake, or {@link #NO_ANSWER}. */
        public final long micros;

        public Row(String server, int ipVersion, long micros) {
            this.server = server;
            this.ipVersion = ipVersion;
            this.micros = micros;
        }

        public boolean answered() {
            return micros >= 0;
        }
    }

    /** Looks host names up (the phone's DNS). An interface so tests can answer without a network. */
    public interface Resolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /** The phone's own DNS lookup. */
    public static final Resolver SYSTEM = InetAddress::getAllByName;

    /**
     * Measures every server over IPv4 and IPv6, one after another, and hands each finished row to
     * {@code onRow} as it arrives (so the page can fill in while the rest is measured).
     *
     * @param servers   name → host, in the order to measure (site/Site.LATENCY_SERVERS)
     * @param cancelled checked between attempts: true stops the work (the window was closed)
     * @return all rows, in measuring order
     */
    public static List<Row> measure(Map<String, String> servers, Resolver resolver, int port,
                                    BooleanSupplier cancelled, Consumer<Row> onRow) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, String> server : servers.entrySet()) {
            InetAddress[] all;
            try {
                all = resolver.resolve(server.getValue());
            } catch (UnknownHostException | RuntimeException e) {
                all = new InetAddress[0];
            }
            for (int version : new int[]{4, 6}) {
                if (cancelled.getAsBoolean()) {
                    return rows;
                }
                InetAddress address = pick(all, version);
                long[] samples = new long[TRIES];
                Arrays.fill(samples, NO_ANSWER);
                for (int i = 0; i < TRIES && address != null && !cancelled.getAsBoolean(); i++) {
                    samples[i] = handshakeMicros(address, port, TIMEOUT_MS);
                    if (samples[i] == NO_ANSWER) {
                        break; // no answer once: don't wait for the timeout twice more
                    }
                }
                Row row = new Row(server.getKey(), version, best(samples));
                rows.add(row);
                onRow.accept(row);
            }
        }
        return rows;
    }

    /** @return the first address of that IP version, or null if the host has none. */
    static InetAddress pick(InetAddress[] all, int version) {
        for (InetAddress a : all) {
            if (version == 4 ? a instanceof Inet4Address : a instanceof Inet6Address) {
                return a;
            }
        }
        return null;
    }

    /** @return the smallest answered sample (the best round trip), or {@link #NO_ANSWER} if none answered. */
    static long best(long[] samples) {
        long best = NO_ANSWER;
        for (long s : samples) {
            if (s >= 0 && (best < 0 || s < best)) {
                best = s;
            }
        }
        return best;
    }

    /** @return the time of one TCP handshake in microseconds, or {@link #NO_ANSWER} (refused, timeout, no route). */
    static long handshakeMicros(InetAddress address, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            long start = System.nanoTime();
            socket.connect(new InetSocketAddress(address, port), timeoutMs);
            return Math.max(0, (System.nanoTime() - start) / 1000);
        } catch (IOException | RuntimeException e) {
            return NO_ANSWER;
        }
    }

    /**
     * @return what the window lists: fastest first, then the ones without an answer. IPv6 rows only if at
     *         least one IPv6 address answered (otherwise this network simply has no IPv6, which the page says
     *         in one line instead of four "no answer" rows).
     */
    public static List<Row> forDisplay(List<Row> rows) {
        boolean ipv6Works = false;
        for (Row r : rows) {
            ipv6Works |= r.ipVersion == 6 && r.answered();
        }
        List<Row> shown = new ArrayList<>();
        for (Row r : rows) {
            if (r.ipVersion == 4 || ipv6Works) {
                shown.add(r);
            }
        }
        // Stable sort: equal times keep the measuring order.
        Collections.sort(shown, (a, b) -> {
            if (a.answered() != b.answered()) {
                return a.answered() ? -1 : 1;
            }
            return Long.compare(a.answered() ? a.micros : 0, b.answered() ? b.micros : 0);
        });
        return shown;
    }
}
