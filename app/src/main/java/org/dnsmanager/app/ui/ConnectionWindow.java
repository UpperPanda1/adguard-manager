package org.dnsmanager.app.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONObject;
import org.dnsmanager.app.site.Site;
import org.dnsmanager.app.status.ConnectionReport;
import org.dnsmanager.app.status.ConnectionStatus;
import org.dnsmanager.app.status.Latency;
import org.dnsmanager.app.theme.ThemeBridge;
import org.dnsmanager.app.web.DialogWebViewClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Three-dots menu → "Connection details" (and the status dialog's "Details" button): what AdGuard's DNS
 * check saw of this phone (protocol, own or public server, device, location, Android's Private DNS) and
 * how fast AdGuard's DNS servers answer (latency). Replaces NextDNS Manager+'s Ping window, which showed
 * two NextDNS web pages that AdGuard has no counterpart for (research D7, owner Q2).
 *
 * <p>The page is {@code assets/connection.html}, opened like About and Settings: a full-screen
 * {@link Dialog} holding a {@link WebView} with a local page (ui/Windows). Its bridge
 * {@code AndroidConnection} exists on this local page only, and hands out JSON built by
 * status/ConnectionReport (codes and numbers, already validated).</p>
 *
 * <p><b>Threads.</b> The DNS check runs where the status icon's does (status/ConnectionStatus.checkNow),
 * so both always agree. The latency test runs on this window's <b>own</b> thread: it can take a few
 * seconds and must not hold up the app's shared background thread (status checks, cache measuring).
 * Closing the window stops the measuring and ends that thread.</p>
 */
public final class ConnectionWindow {

    private ConnectionWindow() {
    }

    /**
     * Opens the window and starts a fresh check and latency test. Call on the main thread.
     *
     * <p>Lint warns about JavaScript in WebViews because a website could then use the bridge. Here the
     * WebView only ever shows our own {@code connection.html} (links go to the browser, file access is
     * off), so the warning is switched off for this method only, as in AboutWindow.</p>
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static Dialog show(Activity activity, ConnectionStatus status) {
        Dialog dialog = new Dialog(activity);
        Windows.prepare(dialog);

        WebView webView = new WebView(activity);
        webView.setWebViewClient(new DialogWebViewClient(dialog::dismiss));
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);

        ExecutorService latencyThread = Executors.newSingleThreadExecutor();
        // Each "Check again" starts a new run; a run whose number is no longer current stops (and its
        // late answers are dropped). Closing the window makes every run old.
        AtomicInteger run = new AtomicInteger();

        // Runs a line of JavaScript in the page, from any thread, only while the window is open.
        Sender send = script -> activity.runOnUiThread(() -> {
            if (dialog.isShowing()) {
                webView.evaluateJavascript(script, null);
            }
        });
        Runnable sendStatus = () -> send.run("window.onStatus && onStatus(JSON.parse("
                + JSONObject.quote(snapshot(status)) + "))");

        webView.addJavascriptInterface(new Object() {
            /**
             * A fresh DNS check and latency test (the page calls it when it opens, and on "Check again").
             * Answers arrive through {@code onStatus} and {@code onLatency}. The status is read and sent on
             * the main thread only, where ConnectionStatus changes it (this method runs on a WebView thread).
             */
            @JavascriptInterface
            public void refresh() {
                activity.runOnUiThread(() -> {
                    status.checkNow();
                    sendStatus.run(); // also when there is nothing to check yet ("Checking…")
                });
                int mine = run.incrementAndGet();
                List<Latency.Row> measured = new ArrayList<>();
                send.run("window.onLatency && onLatency(JSON.parse("
                        + JSONObject.quote(ConnectionReport.latency(measured, false)) + "))");
                try {
                    latencyThread.execute(() -> {
                        Latency.measure(Site.LATENCY_SERVERS, Latency.SYSTEM, Latency.PORT,
                                () -> mine != run.get(), row -> {
                                    measured.add(row);
                                    sendLatency(send, measured, false, mine, run);
                                });
                        sendLatency(send, measured, true, mine, run);
                    });
                } catch (java.util.concurrent.RejectedExecutionException e) {
                    // The window is closing: nothing to measure for.
                }
            }

            @JavascriptInterface
            public void close() {
                activity.runOnUiThread(dialog::dismiss);
            }
        }, "AndroidConnection");
        // Theme colours for the page (read-only, local page only; see theme/ThemeBridge).
        webView.addJavascriptInterface(new ThemeBridge(activity), "AndroidTheme");
        webView.loadUrl("file:///android_asset/connection.html");

        status.setListener(sendStatus);
        dialog.setContentView(webView);
        dialog.setOnDismissListener(d -> {
            status.removeListener(sendStatus); // only ours: a reopened window may have set its own
            run.incrementAndGet();      // the running measurement stops at its next step
            latencyThread.shutdown();   // and the thread ends once it has
            webView.destroy();
        });
        dialog.show();
        Windows.fill(dialog);
        return dialog;
    }

    /** Runs a line of JavaScript in the page. */
    private interface Sender {
        void run(String script);
    }

    private static void sendLatency(Sender send, List<Latency.Row> measured, boolean done, int mine,
                                    AtomicInteger run) {
        if (mine != run.get()) {
            return; // a newer run (or the closed window) owns the page now
        }
        String json = ConnectionReport.latency(Latency.forDisplay(new ArrayList<>(measured)), done);
        send.run("window.onLatency && onLatency(JSON.parse(" + JSONObject.quote(json) + "))");
    }

    /** Main thread only: ConnectionStatus changes these fields there. */
    private static String snapshot(ConnectionStatus status) {
        return ConnectionReport.status(status.state(), status.description(), status.stateColor(),
                status.isChecking(), status.lastCheck(), status.privateDnsActive(), status.privateDnsServer());
    }
}
