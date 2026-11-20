# AdGuardDNS Manager — project guide

The one document that explains **what this app is, how it is being made, how it is built, what every file does, and
how to change each feature**. Start at the top for the big picture or jump to the part you need.

- For Claude sessions: [`CLAUDE.md`](CLAUDE.md) (working rules, commands, verified facts).
- The plan the work follows: [`docs/ADGUARD_PLAN.md`](docs/ADGUARD_PLAN.md) (phases 0–12); the step-by-step guide for
  the remaining phases: [`docs/PHASES.md`](docs/PHASES.md). What the live AdGuard site
  does: [`docs/research/2026-11-19-adguard-live-probe.md`](docs/research/2026-11-19-adguard-live-probe.md).
- What changed and when: [`CHANGES.md`](CHANGES.md). Where the last session stopped: [`handoff.md`](handoff.md).
- Keeping in step with the sibling app: [`UPSTREAM.md`](UPSTREAM.md).

## Contents

1. [The app in one page](#1-the-app-in-one-page)
2. [How it is being made](#2-how-it-is-being-made)
   - [2.1 A copy of NextDNS Manager+, not a fork or a flavor](#21-a-copy-of-nextdns-manager-not-a-fork-or-a-flavor)
   - [2.2 Owner decisions](#22-owner-decisions)
   - [2.3 The phases and where they stand](#23-the-phases-and-where-they-stand)
   - [2.4 Minimum Android and WebView versions](#24-minimum-android-and-webview-versions)
3. [Where the code comes from](#3-where-the-code-comes-from)
4. [How the app is put together](#4-how-the-app-is-put-together)
   - [4.1 The screen](#41-the-screen)
   - [4.2 The parts and who talks to whom](#42-the-parts-and-who-talks-to-whom)
   - [4.3 File map](#43-file-map)
5. [Features, one by one](#5-features-one-by-one)
6. [Cache management](#6-cache-management)
7. [How to change things](#7-how-to-change-things)
8. [Versions and build settings](#8-versions-and-build-settings)
9. [Testing and CI](#9-testing-and-ci)
10. [Known limitations and what is not verified](#10-known-limitations-and-what-is-not-verified)
11. [The AdGuard work, phase by phase](#11-the-adguard-work-phase-by-phase)
12. [Roadmap](#12-roadmap)

---

## 1. The app in one page

**AdGuardDNS Manager** will be an Android app that shows the official **AdGuard DNS dashboard**
(`https://adguard-dns.io/en/dashboard/`) with a native top bar and extras a browser doesn't have:

| Feature | In short | State (2026-11-20) |
|---|---|---|
| Several accounts | Each account is a separate AdGuard login, kept apart in its own WebView "profile" (cookies, storage, cache). Switch instantly from the top bar. | works (shared code) |
| Account manager | One list in Settings: add, rename, drag to re-order, sign out, remove. Each account has a number badge. | works |
| Sign-in with Apple, Google, Facebook | AdGuard's login offers them; they run inside the account's container. | Phase 5 |
| Connection status | A shield/warning icon in the top bar: is this phone using AdGuard DNS, and encrypted? | Phase 6 (today: NextDNS's test) |
| Find in page | Search the dashboard's text (three-dots menu, or Ctrl+F). | works |
| App lock | Fingerprint/face/PIN when the app opens; hidden in screenshots and recent apps. | works |
| Theme | White, Black or Match system; the status bar matches the page. | works; AdGuard colours in Phase 4 |
| Account menu tools | Desktop site, Reload, Hide status bar, Clear cache, unload an account. | works |
| Downloads | Files the dashboard makes itself (exported user rules) saved to Downloads; server files open in the browser. | Phase D (`blob:` capture) |
| Ping test, Private DNS shortcut | From NextDNS Manager+; Ping has no AdGuard page (`docs/PHASES.md`, Phase 7). | shortcut works; Ping open |

**Today the app is named AdGuardDNS Manager, has its own id and icon, but still opens the NextDNS dashboard**: everything
about the DNS service is in one file, `site/Site.java`, which Phase 3 rewrites (§11). That is by design: the plan
changes one thing per step so each change can be reviewed on its own.

Package `org.dnsmanager.app` (code, shared with NextDNS Manager+ on purpose), application id
`org.rafee_m.adguarddnsmanager` (store identity), Java only, no UI library, one library in total (`androidx.webkit`).
Release APK 187,414 bytes (clean build, 2026-11-20).

---

## 2. How it is being made

### 2.1 A copy of NextDNS Manager+, not a fork or a flavor

The owner's plan ([`docs/ADGUARD_PLAN.md`](docs/ADGUARD_PLAN.md) §2) compared three ways to build a second app for
another DNS service from **NextDNS Manager+** (`UpperPanda1/nextdnsmanager-plus`):

| Option | Verdict |
|---|---|
| GitHub "Fork" button | **No.** An account can't fork its own repository, a fork elsewhere stays linked, and for F-Droid it is a separate app anyway. |
| Gradle product flavors (one repository, `nextdns` + `adguard`) | **Not now.** Two apps in one README, release list and F-Droid recipe; AdGuard **diverges** (sign-in, captcha, downloads), so more and more files would need a copy per flavor. Revisit only if keeping two copies in step gets painful. |
| **A new repository, copied once (chosen)** | Each app stands alone (README, releases, F-Droid recipe) and can grow its own features. A fix to shared code is ported by hand and recorded in [`UPSTREAM.md`](UPSTREAM.md). |

How the copy was made (Phase 0, 2026-11-19): the NextDNS Manager+ tree at commit **`eaa1665`** (branch
`claude/nextdns-manager-plus-review-el0q2u`) was imported **unchanged** as one commit (`abb10bf`; git tree
`11ec3ad6` = NextDNS's tree, compared by hash), with no history. Every later commit is one adaptation, so each can be
reviewed, reverted and compared with NextDNS. The **Java package stays `org.dnsmanager.app`**: users never see it,
and keeping it makes a NextDNS fix apply here as a near-identical diff. Only the **applicationId** changed.

### 2.2 Owner decisions

| # | Question | Decision | When |
|---|---|---|---|
| — | Repository | **`UpperPanda1/adguard-manager`** (public) for now; the owner will rename it to **`adguard-dns-manager`** later. Until then `app.repoUrl` keeps the current address (§7 "Change the repository address" lists what to change after the rename) | 2026-11-20 |
| — | App name | **AdGuardDNS Manager** (first "AdGuard Manager", changed the same day; plan §13 had "AdGuard DNS Manager+"; the trademark question is in `fdroid.md` §3.2) | 2026-11-20 |
| — | applicationId | **`org.rafee_m.adguarddnsmanager`** (first `org.rafee_m.adguardmanager`, changed the same day, before any release; frozen after the first public release) | 2026-11-20 |
| Q1 | The dashboard's "Export rules" makes a `blob:` file the copied code can't save | **Port LLM Central's blob capture** (new phase D, §11) | 2026-11-20 |
| Q2 | AdGuard has no ping page: remove the Ping window? | **Open.** The owner asked why remove it; the options are in `docs/PHASES.md` Phase 7 | asked 2026-11-20 |
| Q3 | The dashboard follows the phone's dark mode only if the account's theme is "Automatic" | **Keep as is**: no script forces the page's theme; README/Settings say so | 2026-11-20 |
| Q4 | Account and payment pages | **`adguardaccount.com` inside the account's container, `adguardcheckout.com` in the browser** | 2026-11-20 |
| Q5 | Which sign-in providers | **All three: Apple, Google, Facebook** | 2026-11-20 |
| — | The plan file | added to the repository as `docs/ADGUARD_PLAN.md` (unchanged) | 2026-11-20 |

Hard limits carried over from NextDNS Manager+ (CLAUDE.md §6): Java only, minSdk 29, one library, no UI libraries, no
analytics or crash reporting, no swipe to refresh, no text-size feature, no back button in the top bar, theme exactly
White/Black/Match system, status bar = page background. **One rule is reversed for AdGuard:** NextDNS Manager+ had
"no Google/Apple sign-in code"; AdGuardDNS Manager **needs** Apple, Google and Facebook sign-in (Q5).

### 2.3 The phases and where they stand

From the plan's §6, with the owner's answers applied. Details of each phase: [§11](#11-the-adguard-work-phase-by-phase).

| Phase | What | State |
|---|---|---|
| 0 Set-up | Import NextDNS Manager+ `eaa1665` unchanged; build, tests, page checks, CI green | ✅ `abb10bf` (2026-11-19) |
| 1 Research | Live probe of the dashboard, login, sign-in, DNS check, colours | ✅ `8642642`; [research file](docs/research/2026-11-19-adguard-live-probe.md), `tools/site-probe` |
| 2 Identity | Name, applicationId, repository, About credits, icon, README, docs | ✅ 2026-11-20 (`8d6c321`, `9197934`, `1fa8540`, docs `7ec4967`; renamed AdGuardDNS Manager `d671501`) |
| 3 Site | `site/Site.java` for AdGuard + `SiteTest` | **next** (then 6 right after it: `docs/PHASES.md`) |
| 4 Theme | `theme/Themes.java` from AdGuard's CSS | to do |
| 5 Sign-in | Apple, Google, Facebook inside the container; Google's user agent | to do |
| 6 Status | Status icon from AdGuard's DNS check | to do |
| 7 Ping | Owner decision first (Q2) | waiting |
| D Downloads | `blob:` files saved to Downloads (Q1) | to do |
| 8 Texts | Settings Privacy/Disclaimer, store texts, changelog 1 | to do |
| 9 Workflows | Release workflow names; local release rehearsal | to do |
| 10 Review | Adversarial review of the whole diff against the import | to do |
| 11 Docs | Final docs pass | to do (this guide is kept current meanwhile) |
| 12 Owner | Phone test, key + secrets, dry run, tag, F-Droid | owner |

### 2.4 Minimum Android and WebView versions

**minSdk 29 (Android 10). No WebView version is required**; features that need a newer WebView switch themselves off.

- Private DNS (what the status icon reads) exists since Android 9; the system unlock sheet with PIN fallback since
  Android 10 (`BiometricPrompt` + device credential); current WebView releases are only published for Android 10+.
- **Several accounts** need the WebView feature `MULTI_PROFILE`, which depends on the installed *Android System
  WebView* version, not on Android. Without it the app allows one account and says why (Settings → Accounts). The
  first WebView version with it is **119** (from LLM Central's docs; not re-checked here).

---

## 3. Where the code comes from

| Origin | Files |
|---|---|
| **NextDNS Manager+ `eaa1665`, unchanged** (shared code; fixes are ported both ways, `UPSTREAM.md`) | everything in `app/` except the files below, all of `tools/page-checks/` (except the stand-in app name), `tools/icon/`, `.github/workflows/`, the Gradle files, the session hook |
| **Changed for AdGuard so far** | `app/build.gradle` (applicationId, debug name), `res/values/strings.xml` (`app_name`), `assets/about.html` (credits, note), `artwork/icon.svg` + the generated icon files, `gradle.properties` (`app.repoUrl`), `settings.gradle`, `tools/readme-badges/generate.mjs`, `artwork/readme/`, the page checks' stand-in app name, every document |
| **New in this repository** | `tools/site-probe/` (live probe of AdGuard's site), `docs/ADGUARD_PLAN.md`, `docs/PHASES.md`, `docs/research/`, `docs/superpowers/plans/`, `UPSTREAM.md` |
| **Still NextDNS-specific, to change** | `site/Site.java` (Phase 3), `theme/Themes.java` + `res/values*/colors.xml` (Phase 4), `status/StatusRules` (Phase 6), `ui/PingWindow` (Phase 7), `downloads/Downloads` naming and `blob:` (Phase D), texts in `settings.html` (filled from `Site.NAME`; wording Phase 8), `fastlane/…/changelogs/1.txt` (Phase 8), `release.yml` names (Phase 9), comments naming NextDNS (`Profiles`, `ThemeManager`, `top_bar.xml`, `AndroidManifest.xml`, `zoom_viewport.js`) |

Where NextDNS Manager+ itself came from (its `PROJECT.md` §2–3): **LLM Central** (`UpperPanda1/assist`: accounts,
tabs, settings design; everything AI-related stripped) plus the NextDNS-specific *features* of **NextDNS Manager**
(`doubleangels/nextdnsmanager`: status, ping, Private DNS shortcut, app lock, downloads, links; ideas only, no file of
that app is in the tree). All three are GPL-3.0; About → "Built on" names them.

---

## 4. How the app is put together

### 4.1 The screen

```
┌─────────────────────────────────────────────┐  ← Android status bar (same colour as the bar below)
│ (1) Home ▾                  🛡   ⌂   ⋮      │  ← top bar: account chip · status · home · menu
├─────────────────────────────────────────────┤
│░░░░░░ loading bar (only while loading) ░░░░░│
│                                             │
│        the DNS service's dashboard          │  ← the current account's WebView ("tab")
│   (other loaded accounts: hidden WebViews)  │
│                                             │
├─────────────────────────────────────────────┤
│ 🔍 [search………]   3/12   ↑  ↓  ✕            │  ← find bar (only while searching)
└─────────────────────────────────────────────┘
  + over everything while locked: the lock cover (padlock, "Locked", Unlock)
```

The view tree (`res/layout/activity_main.xml`): `screen` (frame) → `mainRoot` (column: `topBar`, `tabContainer`)
+ the lock cover added at runtime. `tabContainer` holds the tab WebViews, the two black placeholder screens and the
loading bar. Settings, About and Ping test are full-screen dialogs on top (`ui/Windows`).

### 4.2 The parts and who talks to whom

```
                         MainActivity  (wiring only: creates parts, connects buttons, passes lifecycle on)
        ┌──────────────┬───────┴────────┬──────────────┬───────────────┬─────────────┐
   web/TabManager   ui/SwitcherPopup  ui/SettingsWindow  status/       lock/          downloads/
   (tabs)           (account menu)    (Settings page)    ConnectionStatus AppLock     Downloads
     │  └ web/Profiles (containers, cache)   │ bridges:       │              │ LockRules
     │  └ web/TabLru, LoadErrors, Recovery   │ AndroidSettings│ StatusRules  ui/LockScreen
     │  └ web/DesktopMode, PageZoom          │ AndroidAccounts → account/AccountsBridge → AccountStore
     └ site/Site  ← everything about the     │ AndroidStorage → storage/StorageBridge
                    DNS service              │ AndroidTheme   → theme/ThemeBridge ← theme/Themes
```

Rules of the road (also in CLAUDE.md §7):

- **Logic lives in small classes**, mostly plain Java so it can be unit-tested on a computer. `MainActivity` only
  connects them. A new feature gets its own class.
- **Only `site/Site.java` knows the DNS service.** Everything else asks it (addresses, domains, test answer format,
  links). Phase 3 points it at AdGuard; the sign-in hosts (Phase 5) go there too.
- **Only `theme/Themes.java` holds colours.** Native views ask `ThemeManager.scheme()`, HTML pages get the same
  colours as CSS variables (`--md-primary` …) through `AndroidTheme.css()`.
- **Every setting is stored in one file** (`Prefs.FILE` = `app_prefs`); each feature owns its key name.
- **Bridges** (Java objects a page can call) are given only to the app's own local pages, never to the website.

### 4.3 File map

Java: `app/src/main/java/org/dnsmanager/app/` (43 files, 6,128 lines). Tests: `app/src/test/java/org/dnsmanager/app/`
(23 files, 92 tests). The last column says which AdGuard phase changes a file (blank = shared, unchanged).

| File | What it does | Tested by | AdGuard |
|---|---|---|---|
| `MainActivity.java` | The one screen: lifecycle, top bar, three-dots menu, tab set-up (`configureWebView`), page events (`TabClient`, incl. `shouldOverrideUrlLoading`), Settings host, app lock flow | — (wiring) | 5 (sign-in wiring), 7, D |
| `Prefs.java` | The settings file's name and shared keys (`max_live_tabs`, `last_account`) | — | |
| **site/** | | | |
| `Site.java` | Everything about the service: name, home/test/ping addresses, owned domains (`ownsHost`), test answer format, Private DNS marker, legal links, script origin rules | `SiteTest` | **3**, 5, 6 |
| **account/** | | | |
| `Account.java` | One account: id + name; `profileName()` (`acct_<id>`, or the default profile for id `main`) | `AccountStoreTest` | |
| `AccountStore.java` | The account list and all its rules (add, rename, remove, reorder, numbering, limits, safe ids); saved as JSON | `AccountStoreTest` | |
| `AccountsBridge.java` | `AndroidAccounts` for the Settings page; every call answers with the fresh list | page check `settings-accounts` | |
| **web/** | | | |
| `TabManager.java` | One WebView per loaded account, show/unload/switch, retry screen, renderer-crash recovery, `isStartUrl`; no page while the account's container is being emptied (`loadAfterWipe`) | `TabManagerTest` | 3 (test hosts) |
| `WipeGate.java` | Which containers are being emptied, which account to load when done, `Join` | `WipeGateTest` | |
| `TabLru.java` | Which loaded tab closes first when over the limit | `TabLruTest` | |
| `Profiles.java` | Account containers (WebView profiles), cache cap, Clear cache for all, sign-out/remove wipe (`WipeGate`), start-up clean-up | — (Android-only; rules in [§6](#6-cache-management)) | |
| `LoadErrors.java` | When the "Tap to retry" screen comes and goes | `LoadErrorsTest` | |
| `WebViewRecovery.java` | Replace/discard a WebView whose renderer died | — | |
| `DesktopMode.java` | Desktop site per account (user agent, client hints, viewport script) | `DesktopModeTest` | 5 (must not undo the sign-in user agent) |
| `PageZoom.java` | Pinch to zoom (lifts the page's zoom ban at document start) | page check `zoom-viewport` | |
| `FindInPage.java` | Ctrl+F detection, highlight CSS, "3/12" text | `FindInPageTest` | |
| `DialogWebViewClient.java` | Client for the app's own pages: links to the browser, renderer death closes the window | `DialogWebViewClientTest` | |
| `ExternalLinks.java` | The one place that hands a link to another app; only https/http/mailto/tel pass | `ExternalLinksTest` | |
| `Scripts.java` | Loads `assets/js/*.js` | `ScriptsTest` | D (blob script) |
| *(Phase 5)* `SignInUserAgent.java` | Google's sign-in page only: desktop-Linux user agent (ported from LLM Central) | 4 tests to port | 5 |
| **theme/** | | | |
| `Themes.java` | The two colour schemes (all colours of the app) | `ContrastTest` | **4** |
| `Scheme.java` | A scheme object + its CSS | `SchemeTest` | |
| `ThemeManager.java` | White/Black/Match system setting, night override, window/status-bar colours, Hide status bar, insets | — | |
| `ThemeRebuild.java` | When a theme change rebuilds the screen | `ThemeRebuildTest` | |
| `ThemeBridge.java` | `AndroidTheme.css()` for local pages | page check `settings-dark` | |
| `ContentInsets.java` | Padding rule with/without the status bar | `ContentInsetsTest` | |
| **ui/** | | | |
| `SwitcherPopup.java` | The account menu | — (view code) | |
| `AccountBadge.java` | Numbered badge | `AccountBadgeTest` | |
| `NativeTheme.java` | Colours top bar, toggles, loading bar | — | |
| `SettingsWindow.java` | Opens Settings, `AndroidSettings` bridge (sends `service` = `Site.NAME`, `app_name` = launcher label), Back handling | page checks `settings-*` | |
| `SettingsRestore.java` | Reopen Settings after a rebuild; safe section names | `SettingsRestoreTest` | |
| `AboutWindow.java` | Opens About with name (launcher label), version, repository/author (`BuildConfig`), icon (the launcher icon drawn to a PNG) | page check `about` | |
| `PingWindow.java` | Ping test window (two service pages) | — | **7** |
| `LockScreen.java` | The lock cover (also hides what's under it from TalkBack/keyboard) | — | |
| `FindBar.java` | The find bar | — | |
| `UnloadedPlaceholder.java` | Black "Tap to load" / "Tap to retry" screens | — | |
| `Windows.java` | Makes a dialog full-screen; `setSecure` for the app lock | — | |
| **lock/** | | | |
| `AppLock.java` | Setting + Android's unlock sheet ("Unlock AdGuardDNS Manager") | — | |
| `LockRules.java` | When to lock (fresh start, away > 1 minute) | `LockRulesTest` | |
| **status/** | | | |
| `ConnectionStatus.java` | Listens to the network, runs the test request, paints the icon | — | 6 (random test name) |
| `StatusRules.java` | Private DNS + test answer → state and colour | `StatusRulesTest` | **6** |
| **downloads/** | | | |
| `Downloads.java` | `data:` files saved via MediaStore; http(s) files handed to the browser; `blob:` refused today | `DownloadsTest` | **D** |
| `DataUrl.java` | Decodes `data:` addresses | `DataUrlTest` | |
| **storage/** | | | |
| `StorageBridge.java` | `AndroidStorage` for Settings: measure / clear cache | page check `settings-options` | |
| `AppCache.java` | Cache folder size | `AppCacheTest` | |

Other files:

| File | What it is |
|---|---|
| `app/src/main/assets/settings.html` | The Settings screen (HTML + CSS + JS in one file); the service's name fills every `.svc` element from `Site.NAME` |
| `app/src/main/assets/about.html` | The About screen ("Built on": NextDNS Manager+, LLM Central, NextDNS Manager) |
| `app/src/main/assets/js/zoom_viewport.js`, `desktop_viewport.js`, `find_highlight.js` | Scripts added to the dashboard |
| `app/src/main/assets/lib/reorder.js` | Drag-to-reorder for the account list |
| `app/src/main/res/layout/activity_main.xml`, `top_bar.xml` | The screen and the top bar |
| `app/src/main/res/values/strings.xml` | Texts of the native parts; `app_name` = "AdGuardDNS Manager" |
| `app/src/main/res/drawable/ic_*.xml` | Icons (Material Icons, vector) |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml` + `drawable/ic_launcher_*.xml` | Launcher icon (own design since 2026-11-20: two white server units on `#1A6BF4`, AdGuard dashboard blue; vector adaptive icon, no PNGs needed at minSdk 29). The two `drawable` files are **generated** from `artwork/icon.svg` |
| `app/src/main/res/xml/*backup*.xml` | Backup: settings yes, logins/site data never |
| `app/lint.xml` | The lint warnings accepted on purpose, with reasons |
| `artwork/icon.svg` | **The app icon's source**: edit this to change the icon everywhere ([§7](#7-how-to-change-things)) |
| `tools/icon/generate.mjs` | Makes the launcher icon files and the store icon from `artwork/icon.svg` |
| `fastlane/metadata/android/en-US/images/icon.png` | 512 × 512 store icon (F-Droid), generated |
| `tools/page-checks/` | Browser checks of the HTML pages (README inside) |
| `tools/site-probe/` | Live probe of the AdGuard site: sign-in chain, page colours, DNS check (README inside) |
| `tools/readme-badges/` → `artwork/readme/` | README banner + badges (light/dark) in the icon's blue `#1A6BF4` (`BRAND`), banner drawing read from `artwork/icon.svg`; ported from LLM Central. Run after a version change (needs a release build for the APK size) |
| `.github/workflows/ci.yml` | CI |
| `.github/workflows/icon.yml` | "App icon" workflow: regenerates the icon files from the SVG (by hand) or checks them (on push) |
| `.github/workflows/release.yml` | Release: on a `v*` tag builds unsigned, signs with apksigner 34 (v1 off), runs F-Droid's `apksigcopier compare`, publishes; dry runs by hand or on PRs (`RELEASE.md`). **Still names the APK `NextDNS-Manager-Plus-…` until Phase 9** |
| `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` | Release notes / F-Droid "What's new" (≤ 500 characters); `1.txt` still describes NextDNS (Phase 8) |
| `BUILDING.md`, `RELEASE.md`, `fdroid.md`, `re-sign.md` | How to build; how to release and sign; the F-Droid submission; re-signing commits with GPG |
| `docs/ADGUARD_PLAN.md`, `docs/PHASES.md`, `docs/research/`, `docs/superpowers/plans/` | The owner's plan; **the step-by-step guide for every remaining phase**; research results; records of finished phases |
| `UPSTREAM.md` | Port list with NextDNS Manager+ |
| `.claude/hooks/session-start.sh` | Sets up the Android SDK in Claude's cloud sandbox |

---

## 5. Features, one by one

Each part says what the user sees, how it works, where to change it, and what changes for AdGuard.

### 5.1 Top bar and three-dots menu

- **Sees:** chip `(n) Name ▾` on the left; status icon, home, three dots on the right. No back button: Android's Back
  goes back inside the page (find bar first, then page history, then leaves the app).
- **Three dots:** Find in page · Ping test · Private DNS settings · Settings. Refresh is not here (it is Reload in the
  account menu).
- **Code:** layout `res/layout/top_bar.xml`; buttons in `MainActivity.setupTopBar`; menu in
  `MainActivity.showOverflowMenu` (each item's id is its string resource); colours `ui/NativeTheme.styleTopBar`.
- **Fits 360 dp:** chip name max 120 dp + three 48 dp buttons (comment in `top_bar.xml`).
- **AdGuard:** the Ping item depends on Q2 (Phase 7).

### 5.2 Accounts and their containers

- **Sees:** Settings → Accounts is the whole manager: rows `[n] Name  ⋮  ═`. Tap = open it. ⋮ = Open, Rename,
  Sign out, Remove (greyed out for the last account). ═ = drag (or arrow keys) to re-order. "Add account" below.
- **Rules** (`account/AccountStore`): at least one account; at most 20; the list order = menu order = badge numbers;
  names cleaned (spaces, 40 characters, whole emoji); ids are safe profile names (`main` or `a-xxxxxxxx`).
- **Containers** (`web/Profiles`): account `main` uses WebView's default profile; others `acct_<id>`. A profile holds
  that account's cookies (login), storage and cache, apart from all others.
- **Sign out** deletes the account's login and site data (the account stays). **Remove** also deletes the account and,
  at the next start, its folder. Both close the account's tab first and wait until the deletion has finished before
  the container is used again (`web/WipeGate`).
- **Old WebView** (no `MULTI_PROFILE`): only one account; "Add account" is hidden and a hint explains why.
- **Flow of a change:** page → `AndroidAccounts.x()` (`AccountsBridge`) → `AccountStore` (rules) → saved → main
  thread: `TabManager.updateAccounts` (close removed tabs, new names, top bar) → the page redraws from the returned list.
- **AdGuard:** the login cookies live on `auth.adguardaccount.com` (`JSESSIONID`, host-only), `adguardaccount.com`
  (`user`) and `adguard-dns.io` (`adga`, `lang`) [research F3]; all inside the account's profile. Social sign-in pages
  (Apple, Google, Facebook) also store their cookies in that profile (Phase 5), so each account can use a different
  Google login.

### 5.3 Tabs: loading, switching, unloading

- Each loaded account = one WebView in `tabContainer`; only the current one is visible; hidden ones are paused
  (`onPause()`: the page is marked hidden, its timers are slowed down).
- **Accounts kept loaded** (Settings → Behaviour, 1–10, default 3): over the limit, the account used longest ago is
  closed (`TabLru`). A closed account keeps its login; picking it loads it again (from the cache, see §6).
- **Unload** (✕ on a loaded row in the account menu) frees one account's memory now; if it was on screen, the black
  "Unloaded. Tap to load" screen shows.
- **Start:** the account used last (`Prefs.LAST_ACCOUNT`); after a screen rebuild, the same account **and page**
  (`STATE_URL`, accepted only for `https://` pages of the service: `TabManager.isStartUrl` → `Site.ownsHost`).
- **Low memory** (`onTrimMemory`): hidden tabs close.

### 5.4 Account menu

Opened from the chip (`ui/SwitcherPopup`):

1. One row per account: badge with its number, name, ✕ if loaded. The current one is highlighted.
2. Icon row: **Desktop site** (this account; reloads), **Reload page**, **Hide status bar** (toggle; swipe down to peek),
   **Clear cache** (all accounts; logins stay; same action as in Settings).
3. **Manage accounts** → Settings scrolled to Accounts.

What each tap does is in `MainActivity.switcherHost`.

### 5.5 Settings

`assets/settings.html`, opened by `ui/SettingsWindow` as a full-screen window. Sections:

| Section | Rows |
|---|---|
| Accounts | the account manager (§5.2) |
| Appearance | Theme: Match system / White / Black |
| Behaviour | Pinch to zoom · Accounts kept loaded (1–10) |
| Security | App lock |
| Data | Clear cache (shows the current size) |
| About & legal | About AdGuardDNS Manager · Privacy (what the app stores/sends) · *service* privacy policy · *service* user agreement · Permissions · Disclaimer |

- *service* = `Site.NAME`, filled into every `.svc` element: today "NextDNS", "AdGuard DNS" from Phase 3. The Privacy
  and Disclaimer texts are rewritten for AdGuard in Phase 8 (sign-in pages, hCaptcha, AdGuard's own telemetry).
- Privacy, Permissions and Disclaimer are sub-screens inside the page (`openSubView('name')`); Back returns.
- Links to the service's pages are sent **by name** (`openLink('service_privacy')`) and looked up in `Site.LINKS`, so
  the page can't open arbitrary addresses. AdGuard's: privacy `https://adguard-dns.io/en/privacy.html`, terms (DNS EULA)
  `https://adguard-dns.io/en/eula.html` [research L1].
- **Back:** closes an open dialog → cancels a drag → leaves a sub-screen → closes Settings (`window.handleBack`).
- Rebuilt screen: Settings reopens at the same section (`ui/SettingsRestore`).

### 5.6 Theme and status bar colour

- **Setting:** White, Black, Match system (`ThemeManager.MODE_LIGHT/DARK/SYSTEM`, key `theme_mode`).
- **Colours:** `theme/Themes.java`. Today NextDNS's: light white, dark `#212529`, accent `#0056B3`. **Phase 4** sets
  AdGuard's, measured in Chromium from the dashboard's real CSS [research T1]: light page `#ffffff` (text `#333e4c`),
  dark page **`#242d38`** (text white), accent `#3c81f6` light / `#639af8` dark; the exact accent shade is chosen by
  `ContrastTest` (WCAG 4.5:1 for text). Top bar, window and Android's status/navigation bars all use the scheme's
  `surface`, so the status bar always matches the page (`ThemeManager.styleWindow`).
- **How the website follows:** WebView answers the page's `prefers-color-scheme` from the **app's theme**: the app
  forces night mode on its own screen (`ThemeManager.nightOverride` in `attachBaseContext`) and the manifest theme
  `Theme.DeviceDefault.DayNight` follows it. Nothing is injected into the page. **AdGuard's dashboard** uses
  `prefers-color-scheme` only when the account's theme setting is **Automatic**; otherwise the account's Light/Dark
  choice wins, and with no setting it is Light [research T2]. **Owner (Q3): keep it that way**; the user switches the
  dashboard to Automatic once if they want it to follow.
- **When the screen rebuilds** (`theme/ThemeRebuild`): the theme is fixed when the screen is built, so a change
  (Settings, or the phone switching day/night under Match system) rebuilds it, never under open Settings. The
  current account and page come back.

### 5.7 App lock

- **Sees:** with App lock on, opening the app shows a cover and Android's unlock sheet ("Unlock AdGuardDNS Manager":
  fingerprint, face, or the phone's PIN/pattern/password). Cancel leaves the cover with an Unlock button; Back leaves the
  app. The app also becomes "secure": blank in recent apps, no screenshots. Every dialog is its own window and gets the
  mark too (`ui/Windows.setSecure`), and `applySecureFlag` updates open dialogs when the lock is switched. While the
  cover shows, the content under it is also hidden from accessibility services and keyboard focus
  (`ui/LockScreen.covers`), and `openSettings` refuses to open.
- **When** (`lock/LockRules`): at a fresh start, and after more than **1 minute** out of sight.
- **Switching it on or off** needs the phone's unlock first; the switch in Settings only moves after Android confirmed
  (`window.onAppLock`).
- **No screen lock on the phone:** it can't be switched on (message).
- **Code:** `lock/AppLock` (framework `BiometricPrompt`, no library), `ui/LockScreen`, `MainActivity.lock/unlock/
  promptUnlock/changeAppLock`.

### 5.8 Connection status icon

| Icon | Meaning |
|---|---|
| green shield ✓ | this phone uses the service, encrypted (DoH/DoT/DoQ) |
| orange ⚠ | uses the service, unencrypted |
| yellow ⚠ | Private DNS is on, but another service |
| red ⚠ | not using the service |
| grey ⚠ | checking (first moment after start) |

- **How today** (`status/ConnectionStatus` + `status/StatusRules`): Android's Private DNS state at once, then a request
  to `Site.TEST_URL` (NextDNS: `test.nextdns.io`, JSON `status`/`protocol`). A positive test answer wins, so router/VPN
  set-ups show correctly. Runs only while the app is visible; a newer network change discards an older answer.
- **AdGuard (Phase 6)** [research D1–D6]: `https://a<random>-dnscheck.adguard-dns.com/dnscheck/test`, a **new random
  part each time**. The name exists only in AdGuard DNS (AdGuard's resolver answers it, Google's and Cloudflare's say
  NXDOMAIN; tested live), so any JSON answer = in use, a failed lookup = not in use. The JSON has `protocol` in **lower
  case** (`dns`/`doh`/`dot`/`doq`/`dnscrypt`) and `server_type` `private` (the user's own server) or `public`.
  Private DNS marker: a host ending in `.adguard-dns.com` (personal servers `<device id>.d.adguard-dns.com`; public
  `dns.`, `family.`, `unfiltered.adguard-dns.com`).
- **Tap** = explanation + "Private DNS settings" button.

### 5.9 Find in page

Three-dots → Find in page, or Ctrl+F on a keyboard. The bar at the bottom uses WebView's own find (Chromium's engine);
matches are coloured with the accent where the WebView supports `::search-text` (WebView 144+). Back closes the bar.
Code: `ui/FindBar`, `web/FindInPage`, `assets/js/find_highlight.js`.

### 5.10 Ping test and Private DNS shortcut

- **Ping test** (`ui/PingWindow`): two of the service's web pages in a full-screen window, default profile, no login:
  `Site.PING_URL` (NextDNS: `ping.nextdns.io`, how fast each server location answers) above `Site.TEST_URL` (what the
  service sees of this phone). **AdGuard has neither page** [research D7]; see `docs/PHASES.md` Phase 7 for the options (Q2).
- **Private DNS settings:** Android has no direct screen for it; the app opens the network settings
  (`Settings.ACTION_WIRELESS_SETTINGS`) where it sits.

### 5.11 Downloads

WebView doesn't download anything by itself: without `downloads/Downloads` a download button does nothing.

| What | Today | AdGuard |
|---|---|---|
| `data:` link (the file is inside the link) | decoded by `downloads/DataUrl`, saved to Downloads via MediaStore as "*Site.NAME* download.*ext*" | the dashboard makes none [research W3] |
| `blob:` link (a file the page made in memory) | **refused** ("This file can't be downloaded"): nothing outside the page can read it | the dashboard's **"Export rules"** makes `rules.txt` this way [W1] → **Phase D** ports LLM Central's blob capture (owner Q1) |
| A file on a server (`https://…`) | opened in the phone's **browser**, which downloads it | DNS server settings export, Apple profile (`adguard-dns.mobileconfig`), query log [W2]: the links come from an authenticated API call; whether the browser can fetch them without the page's login is **unverified** (phone check P4) |

No storage permission is needed on Android 10+.

### 5.12 Desktop site and Pinch to zoom

- **Desktop site** (per account, account menu): computer user agent and client hints, page width 1024 px. The tab
  reloads. `web/DesktopMode`. Phase 5 must make sure it doesn't undo Google's sign-in user agent.
- **Pinch to zoom** (Settings, all accounts, off by default): the dashboard forbids zooming (AdGuard's website template:
  `maximum-scale=1, user-scalable=0` [F8]); `assets/js/zoom_viewport.js` lifts that at document start; open tabs reload
  once. `web/PageZoom`. There is no text size setting (removed on purpose).

### 5.13 Errors, crashes and links

- **Page can't load** (offline, server down): black "Couldn't load this page. Tap to retry" screen until a page is
  really drawn (`web/LoadErrors`).
- **Renderer crash** (WebView's page process died): a hidden tab closes; the visible one is rebuilt on the same page;
  three crashes within a minute → closed with a message (`TabManager.onRendererGone`). Every WebView of the app
  handles this, or Android would end the whole app.
- **Links** (`MainActivity.TabClient.shouldOverrideUrlLoading`): `https` pages of the service's domains
  (`Site.ownsHost`: the domain or a real subdomain) stay in the app; everything else opens in the phone's apps
  (`web/ExternalLinks`: https/http/mailto/tel only). Frames inside the page are never redirected (only main-frame
  navigations are checked), so hCaptcha's frames will load. Third-party cookies are off in tabs.
- **AdGuard (Phases 3 + 5):** domains `adguard-dns.io` and `adguardaccount.com` (login `auth.`, account pages; owner
  Q4) load inside; `adguardcheckout.com` (payment) opens in the browser (Q4); Google's, Apple's and Facebook's sign-in
  hosts load inside **for main-frame navigations only** (research S4–S8), never for anything else.

---

## 6. Cache management

The requirements (from NextDNS Manager+, unchanged): Clear cache empties every account's cache but keeps all logins;
the cache must not grow without bound; reloading must reuse the cache instead of storing copies.

**Where things live** (per account profile, inside `app_webview/` and `cache/WebView/`):

| Data | Cleared by "Clear cache"? | Cleared by "Sign out" / "Remove"? |
|---|---|---|
| Cookies (= the login) | **no** | yes |
| Site storage (localStorage, IndexedDB) | no | yes |
| HTTP cache (the site's scripts, styles, fonts, images) | **yes** | yes |
| Compiled-code cache | yes (part of the cache) | yes |

**The rules and where they are enforced:**

1. **One cache per account, kept for the account's lifetime.** Each profile has one cache folder; unloading a tab,
   reloading it or restarting the app reuses the same folder. The app never clears the cache by itself. Tabs use
   `WebSettings.LOAD_DEFAULT` = normal browser caching (`MainActivity.configureWebView`).
2. **Reuse is real for NextDNS** (checked 2026-11-15: hashed file names, one-year `Cache-Control`). **AdGuard: to
   check in Phase 3.** Its files live in a build-named folder (`st2.adguardcdn.com/dns-website-<build>/…`), which
   suggests the same pattern; the headers have not been read yet.
3. **A cap per account:** `Profiles.MAX_CACHE_BYTES` = 20 MB HTTP cache (WebView deletes the oldest entries itself);
   the code caches are half that each, so one account uses at most about 40 MB. AdGuard's dashboard is bigger than
   NextDNS's (`dashboard.min.js` 1.4 MB + `dashboard-vendor.min.js` 2.3 MB + CSS 0.3 MB, uncompressed [research]), still
   well under the cap. Set in `Profiles.attach` for every profile (needs WebView feature `HTTP_CACHE_MANAGER`).
4. **Clear cache clears all accounts** (`Profiles.clearAllCaches`): open tabs via `clearCache(true)` (cache only, that
   WebView's profile); every other profile through a short-lived invisible WebView put into it.
5. **Removed accounts leave nothing:** the profile is deleted (or wiped if in use, then deleted at the next start by
   `Profiles.cleanUp`). No page loads into a container while it is being emptied (`web/WipeGate`, 10 s limit).
6. The Settings row shows the total cache size (`storage/AppCache.sizeBytes`, the same figure as Android's App info).

**Not verified on a phone:** the effect of the cap and the clearing.

---

## 7. How to change things

**Change a text:** native parts → `app/src/main/res/values/strings.xml`; Settings → `assets/settings.html`; About →
`assets/about.html`. The service's name in pages comes from `Site.NAME` (elements with class `svc`); the app's name
from `app_name` (the launcher label).

**Change the app's name or id:** `app_name` in `strings.xml`; the debug name and `applicationId` in `app/build.gradle`
(**the id must never change after the first public release**: Android and F-Droid treat a new id as a different app,
users lose their accounts); the page checks' stand-in name (`tools/page-checks/common.mjs`, `about.mjs`,
`settings-options.mjs`); `title.txt` once it exists; README; the F-Droid recipe. Check with `aapt dump badging` on the
built APKs (CHANGES.md 2026-11-20).

**Change the app icon** (launcher, themed icon, About screen, store icon and README banner all follow):

1. Edit `artwork/icon.svg`. Keep its rules, written at the top of the file: a 108 × 108 canvas, `<rect
   id="background">` with one colour, `<g id="foreground">` with `<path>`s only (optionally
   `transform="translate(x y) scale(s)"`), everything important within the middle 66 × 66 (21–87).
2. Generate the files, either way:
   - **GitHub:** Actions → **App icon** → Run workflow → pick your branch. It generates the files, commits them and
     starts CI. The run's artifact `icon-files` has `preview.png` (circle, rounded square, themed).
   - **Locally:** `cd tools/icon && npm install && npx playwright install chromium && npm run generate`, then look at
     `tools/icon/preview.png` and commit the changed files.
3. Re-run `tools/readme-badges` for the banner. A push that changes the SVG without the generated vector files fails
   the App icon check.

**Change a colour:** `theme/Themes.java` (both schemes), then the fallback values at the top of `settings.html` and
`about.html` (used only outside the app), then `res/values*/colors.xml` (start-up only). Run the tests:
`ContrastTest` fails if text gets unreadable. `node tools/site-probe/probe.mjs colours` shows the dashboard's current
colours.

**Point the app at the service** (Phase 3): `site/Site.java` only (`NAME`, `HOME_URL`, `DOMAINS`, `TEST_URL`,
`PING_URL`, `PRIVATE_DNS_MARKER`, test fields, `LINKS`), then `SiteTest` and the hosts in `TabManagerTest` /
`LoadErrorsTest`; mutation-check `ownsHost`.

**Add a simple switch to Settings** (example: "Keep screen on"):

1. `settings.html`: copy the "Pinch to zoom" row, new input id `keep_screen_on`.
2. Same file, `loadSettings()`: `document.getElementById('keep_screen_on').checked = !!settings.keep_screen_on;` and
   under `DOMContentLoaded`: on `change` call `S.setKeepScreenOn(e.target.checked)`.
3. `ui/SettingsWindow.Bridge.getSettingsJson`: `o.put("keep_screen_on", prefs.getBoolean("keep_screen_on", false));`
   and a `@JavascriptInterface public void setKeepScreenOn(boolean on)` that saves it and calls a new `Host` method.
4. `MainActivity.settingsHost`: implement that method (apply the setting).
5. Add the row to a page check (`tools/page-checks/settings-options.mjs`), run tests and checks.

**Add a three-dots menu item:** a string in `strings.xml`, add it to the array in `MainActivity.showOverflowMenu`,
handle it in the same method.

**Add an account-menu icon:** a vector icon in `res/drawable/`, a method in `SwitcherPopup.Host`, a button in
`SwitcherPopup.show` (`addEvenly(icons, iconButton(...))`), implement it in `MainActivity.switcherHost`.

**Add a Settings sub-screen:** copy the `<div id="disclaimerView" hidden>` block with a new id `xyzView`, add a row
with `onclick="openSubView('xyz')"`.

**Add a permission:** add it to `AndroidManifest.xml` **and** a row to Settings → Permissions
(`#permissionList`, `data-permission="…"`); the page check fails if the two differ.

**Change the repository address or author** (About links, repository name, "Developed by"): `app.repoUrl` /
`app.author` in `gradle.properties` (the file has Windows line endings: keep them), then commit and tag. Read at build
time (`app/build.gradle` → `BuildConfig` → `ui/AboutWindow` → `about.html`); the build fails on a malformed value;
GitHub addresses only. `-PrepoUrl=…` is for test builds only, **never** a release (`fdroid.md` §4). Also change the
static mentions in `README.md` and the F-Droid recipe.

**After the planned rename to `adguard-dns-manager`** (owner, 2026-11-20): GitHub redirects the old address, so nothing
breaks at once; then, in one commit: `app.repoUrl` in `gradle.properties` (keep the CRLF line ending) → check with
`grep REPO_URL app/build/generated/source/buildConfig/release/org/dnsmanager/app/BuildConfig.java`; the Obtainium link
in `README.md`; the five addresses of the recipe draft in `fdroid.md` §3.5 and the table in §4; the repository line in
`CLAUDE.md` §2, `handoff.md` §1 and `RELEASE.md` §1; `git remote set-url origin
https://github.com/UpperPanda1/adguard-dns-manager.git`. Find them all with
`grep -rn "UpperPanda1/adguard-manager" --exclude-dir=.git --exclude-dir=build .` (historical `CHANGES.md` entries
stay as they are). `release.yml` needs nothing: it compares `app.repoUrl` with the repository it runs in.

**Change the README banner or badges:** `tools/readme-badges/generate.mjs` (`BRAND` = one hex re-colours everything;
`WEBVIEW_FOR_ACCOUNTS`; texts of the banner in `banner()`); then `cd tools/readme-badges && npm ci && npm run generate`
after a release build, and look at the README in light and dark. Never edit `artwork/readme/*.svg` by hand.

**Re-check a fact about AdGuard's website:** `tools/site-probe` (`signin <Provider> [--disguised]`, `colours`,
`dnscheck`); write the result and the date into the research file.

**Change the cache cap:** `Profiles.MAX_CACHE_BYTES`. **Change the lock grace time:** `LockRules.GRACE_MS`.
**Change the account limit:** `AccountStore.MAX_ACCOUNTS`.

**Add a script for the dashboard:** a file in `assets/js/`, a constant + `ALL` entry in `web/Scripts.java`
(`ScriptsTest` checks both), add it where needed (document-start scripts use `Site.originRules()`).

**Port a fix from or to NextDNS Manager+:** `UPSTREAM.md` (which folders are shared, `git format-patch` / `git am -3`,
add the row in both repositories).

---

## 8. Versions and build settings

| Setting | Value | Why |
|---|---|---|
| minSdk | 29 (Android 10) | §2.4 |
| targetSdk | 35 | raising it changes Android behaviour; test on a phone first |
| compileSdk | 36 | |
| Android Gradle Plugin / Gradle | 8.13.2 / 8.14.5 (checksum pinned) | same as NextDNS Manager+ and LLM Central |
| Java | source/target 11; builds with JDK 17 or 21 | |
| Libraries | `androidx.webkit:webkit:1.17.1`; tests: JUnit 4.13.2, `org.json` | |
| Release | R8 shrinking + resource shrinking, no dependency metadata (reproducible) | |
| applicationId | `org.rafee_m.adguarddnsmanager` (debug: `.debug` suffix, "AdGuardDNS Manager (Debug)") | owner, 2026-11-20; never change after the first public release |
| Version | `versionName` 0.1.0, `versionCode` 1 | first release planned as 0.1.0 (plan §6 Phase 12) |
| Lint | every warning fails the build; accepted ones in `app/lint.xml` with reasons | |
| Repository address, author | `gradle.properties` `app.repoUrl` = `https://github.com/UpperPanda1/adguard-manager`, `app.author` = `Rafee-M` → `BuildConfig.REPO_URL` / `AUTHOR` | committed, not `git remote`: same APK from every clone (§7) |
| Reproducible | same SHA-256 (`bb0699c7…0c1c5a`) from two fresh clones of `1fa8540` at different paths, JDK 21 (2026-11-20, `fdroid.md` §5); JDK 17 vs 21 was proven for NextDNS Manager+ and is to repeat here before the release | F-Droid |

---

## 9. Testing and CI

| Layer | What | Command |
|---|---|---|
| Unit tests | 92 JUnit tests of the plain-Java rules (accounts, site, status, lock, colours/contrast, theme rebuild, downloads/data URLs, tab LRU, start URLs, load errors, desktop mode, find, scripts, cache size, insets, badge, external links, wipe gate). Phases 3–6 change `SiteTest`, `StatusRulesTest`, `ContrastTest` and add the sign-in tests | `./gradlew test` |
| Lint | Android lint, warnings = errors | `./gradlew lint` |
| Builds | debug + release (R8) | `./gradlew assembleDebug assembleRelease` |
| Page checks | 6 Chromium checks of Settings, About and the zoom script with stand-in bridges (accounts flow, options, permissions = manifest, dark colours = `Themes.java`, 360 px width, About links) | `tools/page-checks` (README) |
| Live probes | `tools/site-probe`: AdGuard's sign-in chain, colours, DNS check against the live site (development only, not in CI) | README in the folder |
| CI | `.github/workflows/ci.yml`: job `android` (all of the above + APK artifact + size, `ubuntu-latest`) and job `pages` (page checks + screenshots, `ubuntu-24.04`: Playwright 1.56.1 does not support 26.04) on every push and PR | GitHub Actions |
| Icon check | `.github/workflows/icon.yml`: on a push touching the icon, regenerates the vector files from `artwork/icon.svg` and fails if they differ | GitHub Actions |
| Release | `.github/workflows/release.yml`: checks, tests, lint, unsigned build, signing, F-Droid signature check; publishes only for a tag. Not yet run in this repository | GitHub Actions |

Proven to catch mistakes (mutation checks, NextDNS Manager+ 2026-11-15, same tests): loosening the domain rule fails
`SiteTest` and `TabManagerTest`; an undeclared permission fails `settings-permissions`; a renamed theme option fails
`settings-options`. Phase 3 repeats the `ownsHost` mutation check with AdGuard's domains.

**What no automated test covers:** anything on a real phone (WebView profiles, unlock sheet, saving to Downloads,
status bar colours, cache size, sign-in). See the phone checklists in `CHANGES.md`.

---

## 10. Known limitations and what is not verified

- **Not tested on a phone or emulator** (none available to Claude). Everything Android-side is verified only by
  compiling, lint, unit tests, page checks and reading the Android/Chromium behaviour it relies on.
- **The app still opens NextDNS** (Phase 3 next) and its texts name NextDNS where they come from `Site.NAME`.
- **Not seen live** (no AdGuard account signed in): the signed-in dashboard's HTML (`BACKEND_BASE_URI`,
  `DNS_SERVER_DOMAIN`, `LOGOUT_PATH`), the DNS check's JSON from a phone that uses AdGuard DNS (format read in
  AdGuard's open-source server), hCaptcha with third-party cookies off, Google's `403 disallowed_useragent` in a real
  WebView, whether server download links work without login. Research file §4 (P1–P6).
- **Name/trademark:** "AdGuard" is AdGuard's trademark and "AdGuardDNS Manager" could be read as an official AdGuard app;
  F-Droid reviewers (or AdGuard) may ask. `fdroid.md` §3.2.
- **`data:` downloads** are named "*Site.NAME* download.txt", not the page's name (WebView doesn't pass it).
- **Translations:** English only.
- **`MULTI_PROFILE` minimum WebView version** 119 per LLM Central's docs (not re-checked here); the app checks at runtime.
- **The website following a forced White/Black** relies on WebView answering `prefers-color-scheme` from the app's
  theme, and for AdGuard on the account's theme being Automatic (§5.6).
- **Inherited review items** (open in NextDNS Manager+ too): `REVIEW.md`.

---

## 11. The AdGuard work, phase by phase

**The detailed guide is [`docs/PHASES.md`](docs/PHASES.md)**: for every remaining phase the goal, the owner's decisions,
the files, the steps, the tests and mutation checks, how to verify, "done when", the phone tests to write, the tools
and packages it needs, and the LLM Central code to port. Summary:

| Phase | Changes | Rests on | Owner input |
|---|---|---|---|
| **3 Site** (next) | `site/Site.java`: name "AdGuard DNS", home `adguard-dns.io/en/dashboard/`, domains `adguard-dns.io` + `adguardaccount.com`, marker `adguard-dns.com`, AdGuard's legal links; tests `SiteTest`, `TabManagerTest`, `DownloadsTest`, `StatusRulesTest` | F1–F7, D1–D2, L1 | Q4 ✅ |
| **6 Status** (right after 3) | `status/StatusRules` + `ConnectionStatus`: AdGuard's DNS check (`a<random>-dnscheck.adguard-dns.com`), lower-case protocols, marker as a host suffix | D3–D6 | — |
| **4 Theme** | `theme/Themes.java` etc.: `#ffffff`/`#242d38`, AdGuard blue accent chosen by `ContrastTest` | T1–T2 | Q3 ✅ |
| **5 Sign-in** | sign-in host list in `Site` (main frame only), `web/SignInUserAgent` from LLM Central, Desktop site interplay | S1–S10 | Q5 ✅ |
| **D Downloads** | `blob:` capture script + origin-restricted message listener + MediaStore save; R6 and F2 fixed on the way | W1–W3 | Q1 ✅ |
| **7 Ping** | remove / "Connection details" (recommended) / latency test | D7 | **Q2 open** |
| **8 Texts** | Settings Privacy/Disclaimer, store texts, changelog 1, README | F9, S9 | — |
| **9 Workflows** | `release.yml` names (`AdGuardDNS-Manager-<version>.apk`), local release rehearsal with apksigner 34 + apksigcopier | — | — |
| **10 Review** | adversarial + security review of `abb10bf..HEAD` (REVIEW.md §4) | — | — |
| **11 Docs**, **12 Owner** | final docs pass; phone tests, name answer, key + secrets, dry run, `v0.1.0`, F-Droid | — | owner |

---

## 12. Roadmap

1. Owner: answer Q2 (Ping, `docs/PHASES.md` Phase 7). Phone test of the identity (CHANGES.md).
2. Phase 3 (Site) → 6 (Status) → 4 (Theme) → 5 (Sign-in) → D (Downloads) → 7 (Ping) → 8 → 9 → 10 → 11
   (`docs/PHASES.md`).
3. Owner: phone tests along the way, repository rename (§7), release key + secrets, first `v0.1.0`, F-Droid.
4. Later: translations; inherited review items (REVIEW.md); keep `UPSTREAM.md` current.
