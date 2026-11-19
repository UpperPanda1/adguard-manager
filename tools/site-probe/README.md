# Site probe

Development-time check of the **live** AdGuard DNS website. The app's rules about the site (which hosts the sign-in
walks through, the page colours, how AdGuard's DNS check works) come from these facts, and websites change, so they
are re-checked here instead of trusted from an old note. Results and their dates:
`docs/research/2026-11-19-adguard-live-probe.md`. Not part of the app; not run by CI (it needs the internet and
the live site).

| Command | What it shows | Used by |
|---|---|---|
| `node probe.mjs signin Google` | every main-frame navigation from the dashboard to Google's sign-in page (values in addresses replaced by `…`), pop-ups, all hosts | Phase 5: sign-in hosts |
| `node probe.mjs signin Google --disguised` | the same with LLM Central's desktop-Linux user agent (`web/SignInUserAgent`) | Phase 5 |
| `node probe.mjs signin Apple` / `Facebook` | the same for the other providers | Phase 5 |
| `node probe.mjs colours` | page background, text and accent in light and dark, computed by Chromium from the dashboard's real stylesheet | Phase 4: `theme/Themes.java` |
| `node probe.mjs dnscheck` | a fresh random `…-dnscheck.adguard-dns.com` asked from AdGuard's and Google's resolvers (only AdGuard knows it) | Phase 6: status check |

Limits: headless Chromium is not Android WebView. It doesn't send WebView's `X-Requested-With` header, so Google may
show its page here where a phone gets `403 disallowed_useragent`. Nothing signs in; no passwords are typed.

Run (any computer):

```bash
cd tools/site-probe && npm install && npx playwright install chromium && node probe.mjs colours
```

In Claude's cloud sandbox (Chromium and Playwright are pre-installed):

```bash
cd tools/site-probe && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node probe.mjs colours; rm node_modules
```
