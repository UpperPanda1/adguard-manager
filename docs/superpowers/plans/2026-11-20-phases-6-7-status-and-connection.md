# Phases 6 and 7 — Status icon from AdGuard's DNS check, and "Connection details" (plan and result)

**Goal** (`docs/PHASES.md` Phases 6 and 7): the top-bar icon tells correctly whether this phone uses AdGuard DNS and
whether the connection is encrypted; the three-dots item "Ping test" (two NextDNS web pages) becomes a native-looking
**Connection details** window that shows what AdGuard's DNS check saw and a **latency test** of AdGuard's servers.

**Owner input (2026-11-20):** do 6 and 7 together; Q2 = the recommended option ("Connection details") **plus** a
latency test like `ping.nextdns.io`'s; the UI well designed and consistent with the rest of the app.

## 1. Facts this rests on (checked 2026-11-20)

| Fact | How checked |
|---|---|
| The dashboard's check: `https://a<random>-dnscheck.${DNS_SERVER_DOMAIN}/dnscheck/test`, 10 s timeout, JSON answer = in use, fetch failure = not in use (D3) | `dashboard.min.js` of build `dns-website-0334…`, `checkDnsUsage` |
| The dashboard calls a device "connected" when the answer has a `device_id`, or `server_group_name == "clients"` | same bundle, the protection banner |
| Random part: 4–63 of `a–z A–Z 0–9 -`; the host is `<id>-dnscheck.adguard-dns.com`; JSON fields `client_ip, device_id, profile_id, server_group_name, server_name, server_type (private|public), node_location, node_name, protocol, tls_curve_id`; `protocol` = `dns`, `doh`, `doq`, `dot`, `dnscrypt`; `404` = id unknown, `429` = rate-limited, `500` = server error; `node_location` is a short code (example `ams`) | AdGuardDNS source `af31bb8`: `internal/dnscheck/dnscheck.go`, `remotekv.go`, `internal/dnsserver/protocol.go`, `config.dist.yaml` |
| Only AdGuard DNS resolves a fresh check name (D4) | `tools/site-probe dnscheck`: AdGuard `NOERROR`, Google `NXDOMAIN` |
| `ping.nextdns.io` = list of nearby **NextDNS** edge servers (`dns.nextdns.io/servers`), each asked `https://ipv4-<host>/info`, which answers with its own measured `rtt` (µs). Host names start with the **hosting company** (`vultr-dal-1.edge.nextdns.io`, `anexia-…`, `zepto-…`): NextDNS servers in other companies' data centres | the page's script; `curl dns.nextdns.io/servers`, `/info` |
| AdGuard publishes **no** per-location list; its public hosts are **anycast** addresses (`dns.` `94.140.14.14/15.15`, `unfiltered.` `…14.140/141`, `family.` `…14.15/15.16`, `d.` `…14.49`; IPv6 `2a10:50c0::…`): each address is answered by the nearest AdGuard location | DoH lookups of the four names |
| The sandbox can't fetch the check's JSON (D6) nor open raw TCP connections (`connection refused`) | curl; bash `/dev/tcp` |

## 2. Decisions

1. **Three outcomes of a check**: *in use* (HTTP 200 + a JSON object), *not in use* (the name doesn't resolve —
   `UnknownHostException` — or `404`), *unknown* (`429`, `5xx`, timeout, no network, unreadable answer). Only
   *unknown* leaves the Private DNS state on screen; *not in use* now overrides a green Private DNS state (fixes
   REVIEW.md F3), except that "Private DNS points elsewhere" (yellow) is kept, as it says more.
2. **Encrypted** = `doh`, `dot`, `doq`, **`dnscrypt`** (DNSCrypt encrypts and authenticates every query; only an app
   can use it on Android). `dns` = not encrypted. Anything else = not encrypted (never green by accident).
3. **Private DNS marker as a suffix rule**: `adguard-dns.com` or `*.adguard-dns.com` (fixes F3's "contains").
4. **Latency = time of a TCP handshake** (one round trip; Android apps can't send ICMP pings without root) to port
   **443** of each address, **best of 3**, 2 s timeout each (as `ping.nextdns.io`). Servers: AdGuard's four public
   host names, IPv4 and IPv6. IPv6 rows only when IPv6 works on this network. Results sorted fastest first (as
   `ping.nextdns.io`). Port 443 because it is open on almost every network; latency to an anycast address doesn't
   depend on the port.
5. **The window is a local HTML page** (`assets/connection.html`) in a full-screen dialog, like About and Settings
   (CLAUDE.md §6: screens with many rows are local pages). Bridge `AndroidConnection` on the local page only; values
   from AdGuard are validated in Java (known protocol words, `[A-Za-z0-9-]` codes and ids) before the page sees them;
   the page inserts text with `textContent`.
6. **The status icon and the window share one check** (`ConnectionStatus`): opening the window or pressing *Check
   again* runs a fresh check and updates the icon too.
7. The status explanation (tap the icon) gets a **Details** button that opens the window.
8. The temporary Phase 3 rule (`Site.PING_DOMAINS`, `pingWindowAllows`) and `PING_URL`, `TEST_URL` and the NextDNS
   test fields go away; `ui/PingWindow` is replaced by `ui/ConnectionWindow`.

## 3. UI checklist (ui-consistency: finding-patterns)

Family: the app's local pages, **2 members** (`about.html`, `settings.html`). Reference: **About** (nearest in kind:
read-only rows in a full-screen window; chosen, level 7). Where both agree, taken as is:

| Role | Written as | Settled by |
|---|---|---|
| Window | `Dialog` + `Windows.prepare/fill` (secure mark with the app lock), `DialogWebViewClient`, file access off, `ThemeBridge` as `AndroidTheme`, `webView.destroy()` on dismiss | both members (2/2) |
| App bar | `<header class="appbar">`, back `.icon-btn` with `aria-label="Back"`, `<h1>` 22 px; a second `.icon-btn` on the right for *Check again* (Settings has right-side icon buttons) | 2/2 |
| Colours | `--md-*` variables with the page's own fallbacks for light/dark + `<style id="app-theme">` from `AndroidTheme.css()` last in `<head>`; no fixed colours except the status colour, which comes from `StatusRules.color` through the bridge (one source with the icon) | 2/2; status colour: StatusRules |
| Groups and rows | `.section-label` (14 px, 500, primary) above a `.group` of `.row`s (`surface-container`, 24 px outer corners, 2 px gaps) with leading 24 px icon, `.text > .title + .sub` | 2/2 |
| Notes | `.hint` (14 px, on-surface-variant) under a group | Settings |
| Loading | the value's `.sub` says "Checking…" / "Measuring…" (Settings' cache row says "Measuring…") | Settings |
| Touch feedback, focus | state layer `::after` opacity .1 on `:active`; `:focus-visible` 2 px primary outline | 2/2 |
| Motion | `@media (prefers-reduced-motion: reduce)` turns animations off | Settings |
| Async answers | bridge returns at once; Java answers `window.onX && onX(JSON.parse(<quoted>))`, only while the dialog is showing | Settings (`StorageBridge.onCacheSize`) |
| Width | max 640 px column, nothing wider than 360 px phones | 2/2 (page check `settings-dark`) |
| Checks | `tools/page-checks/connection.mjs`: stand-in bridges, light + dark screenshots, 360 px width, no script errors | `about.mjs` |

New words (for the owner to read): menu item "Connection details", section labels "This connection", "Latency",
row titles, the latency note.

## 4. Steps (one commit each; each passes `test lint assembleDebug assembleRelease`, page checks when HTML changes)

| # | Commit | Code | Tests / checks |
|---|---|---|---|
| 1 | Status icon from AdGuard's DNS check (Phase 6) | `Site` (check address parts, protocols, marker rule; NextDNS test fields out), new `status/DnsCheck` (address, fetch, parse, validation), `StatusRules` (suffix marker, `fromCheck`, new `combine`), `ConnectionStatus` (uses `DnsCheck`, 10 s, keeps the last answer) | `DnsCheckTest` (address format and randomness; every answer: DoH/DoT/DoQ/DNSCrypt any case, plain DNS, unknown word, malformed, empty, 404, 429, 500; fetch against a local HTTP server; unknown host), `StatusRulesTest`; mutations: marker `contains`, dnscrypt/`dns` swapped, 404 as unknown |
| 2 | Latency test of AdGuard's servers | `Site.LATENCY_SERVERS`, new `status/Latency` (resolve, connect timing, best of 3, sort) | `LatencyTest` against a local `ServerSocket` (answers → a time; closed port → no time; sort order; IPv4/IPv6 pick); mutation: max instead of min |
| 3 | Connection details window (Phase 7) | new `assets/connection.html`, `ui/ConnectionWindow`; `ConnectionStatus.checkNow`; menu item + status dialog button; `PingWindow`, `PING_*`, strings out; Settings → Privacy sentence about the latency test | page check `connection.mjs` (+ screenshots light/dark/360 px), `settings-permissions` unchanged; lint (no unused resources); dex: no `nextdns` left |

## 5. Result

(filled in when done)
