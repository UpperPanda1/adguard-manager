# Changes

What changed in **AdGuardDNS Manager** (called *AdGuard Manager* in the entries of 2026-11-20 before the rename),
newest first. Each entry: what changed, why, how it was verified, sizes, and a
numbered "What to test on your phone" list written for a beginner.

The app started on 2026-11-19 as an unchanged copy of NextDNS Manager+ (bottom entry). Changes made **before** that, to
the code both apps share (accounts, tabs, app lock, settings, downloads…), are described in NextDNS Manager+'s own
`CHANGES.md` (`UpperPanda1/nextdnsmanager-plus`, commit `eaa1665`); its phone-test lists apply to the shared parts here
too.

---

## 2026-11-20 — Phase 3: the app opens the AdGuard DNS dashboard

**What.** The app now shows **AdGuard DNS** instead of NextDNS. All of it is one file, `site/Site.java` (the only
place in the code that knows which DNS service the app is for):

| | Before | Now |
|---|---|---|
| Name shown in the app (`Site.NAME`) | NextDNS | **AdGuard DNS** (status messages, Settings, download names) |
| Start page (`HOME_URL`) | `my.nextdns.io` | **`https://adguard-dns.io/en/dashboard/`** |
| Sites that open inside the app (`DOMAINS`) | `nextdns.io` | **`adguard-dns.io`** (dashboard) + **`adguardaccount.com`** (login, "Manage account") |
| Private DNS name that counts as the service | contains `nextdns` | contains **`adguard-dns.com`** (`<device id>.d.adguard-dns.com`, `dns.adguard-dns.com`…) |
| Settings → About & legal links | NextDNS's | **`adguard-dns.io/en/privacy.html`**, **`adguard-dns.io/en/eula.html`** |

Everything else (`adguardcheckout.com` = payment, `adguard.com`, Apple/Google/Facebook…) opens in the phone's browser.

**Why the login must stay inside the app.** Opening the dashboard while signed out sends you on to
`auth.adguardaccount.com/login.html` and back after the login. If that page opened in the browser, the login cookie
would be saved in the *browser*, and the app's account would never be signed in. That is why `adguardaccount.com` is
in the list. Payment (`adguardcheckout.com`) is deliberately left out: money is handled by your browser, not by the
app (owner decision Q4).

**One extra fix the plan didn't foresee — the Ping window.** Until Phase 7 the Ping window still shows NextDNS's two
test pages. One of them, `test.nextdns.io`, immediately *redirects* to a random address like
`jhdnjx….test.nextdns.io`. A *redirect* is when a server answers "go to this other address instead". Android's
`WebView.loadUrl()` does not ask the app about the first address, but it **does** ask about a redirect
(`shouldOverrideUrlLoading`). The window only kept pages that `Site.ownsHost` accepts, and `nextdns.io` is no longer
one of them, so the lower half would have been thrown out to your browser each time you opened Ping. Now the window
asks `Site.pingWindowAllows` (AdGuard's domains + a temporary `Site.PING_DOMAINS` = `nextdns.io`). The **account tabs
still use `Site.ownsHost`**, so NextDNS can never load inside an account. Phase 7 removes this again.

**Still NextDNS on purpose (each has its own phase):**

- **Status icon (Phase 6, next).** Its Private DNS part now knows AdGuard's names, but the online test still asks
  `test.nextdns.io`. For a phone on AdGuard DNS that test answers "unconfigured", which the app reads as "no answer", so
  the icon follows your Private DNS setting — correct for AdGuard users. Only a phone that really uses **NextDNS**
  would wrongly show green "uses AdGuard DNS". Settings → Privacy already says the test goes to "the AdGuard DNS test
  address"; that becomes true in Phase 6.
- **Ping window (Phase 7, waits for your answer to Q2).**
- **Colours (Phase 4):** still NextDNS's blue and dark grey.
- **Comments in the code naming NextDNS:** the next, separate commit.

**Facts re-checked on the live site first** (`docs/research/…` rows F1, F2, F7, F8, L1: unchanged). New: **F10**
the dashboard's files are cached and reused (`ETag` → `304 Not Modified`; they have no `Cache-Control` header, so
the browser decides how long a copy is fresh and otherwise asks with a tiny request — PROJECT.md §6 rule 2) and **F11**
the login page's only link, "Terms and conditions", goes to `adguard.com` (opens in the browser).

**Changed files.** `site/Site.java` (+ teacher-style comments: why each domain, why not checkout; the host rule moved
into one private `matches` method used by both `ownsHost` and `pingWindowAllows`), `ui/PingWindow.java` (one rule),
tests `SiteTest` (rewritten for AdGuard: 5 tests), `TabManagerTest`, `DownloadsTest`, `StatusRulesTest`;
`assets/settings.html` (the page's own stand-in name "AdGuard DNS", shown only before the app fills in `Site.NAME`);
page checks `common.mjs`, `settings-options.mjs` (expects "AdGuard DNS privacy policy"…), `zoom-viewport.mjs` (now
tests AdGuard's real viewport tag `maximum-scale=1, user-scalable=0` instead of NextDNS's); docs.

**Verified.**
- `./gradlew --no-daemon test lint assembleDebug assembleRelease`: **93 unit tests** (was 92; `SiteTest` +1), **0 lint
  issues**. Page checks **6 of 6**; Settings screenshots looked at (says "AdGuard DNS" everywhere).
- **Mutation checks** (break the rule on purpose, a test must fail, then restore): `ownsHost` with `endsWith(domain)`
  → 3 tests fail; with `contains(domain)` → 3 fail; `adguardcheckout.com` added to `DOMAINS` → 3 fail; NextDNS let
  into the account tabs → 3 fail; Ping window without its redirect rule → 1 fails. All restored.
- `aapt`: package `org.rafee_m.adguarddnsmanager`, label "AdGuardDNS Manager" (unchanged). The release `classes.dex`
  contains the AdGuard addresses and, as planned, only `test.nextdns.io`, `ping.nextdns.io`, `nextdns.io` of NextDNS.
- **Not verified:** anything on a phone (login, dashboard, checkout hand-off); the signed-in dashboard was never seen
  (no AdGuard account here). Found on the way, not caused by this change: the page check's `settings-privacy.png`
  screenshot shows the text very faint — identical on the code before this change (it seems to be taken while the
  sub-screen fades in).

**Size.** Release APK (clean build) **187,626 bytes** (+212 bytes: the longer comments don't count, the new addresses
and the extra method do).

**What to test on your phone**

1. Install this commit's debug APK (**AdGuardDNS Manager (Debug)**). Open it: the **AdGuard login page** appears
   *inside* the app (not in Chrome).
2. Log in with your AdGuard e-mail and password. The **AdGuard DNS dashboard** loads inside the app.
3. Close the app completely and open it again: you are **still signed in**.
4. Add a second account (⋮ → Settings → Add account) and log in with a different AdGuard account: both stay signed in;
   switching between them with the number chip shows each one's own dashboard.
5. In the dashboard, tap a link to **upgrade / buy a plan**: the payment page (`adguardcheckout.com`) opens in your
   **browser**, not in the app. Tap "Manage account" (if shown): it opens **inside** the app.
6. On the login page (signed out), tap **"Terms and conditions"**: it opens in your browser.
7. ⋮ → Settings → About & legal → **"AdGuard DNS privacy policy"** and **"AdGuard DNS user agreement"** open AdGuard's
   pages in your browser.
8. Status icon: with Android's Private DNS set to your AdGuard address (`<device id>.d.adguard-dns.com`) it is
   **green**; with Private DNS off it is **red**. (Phase 6 makes this exact.)
9. ⋮ → **Ping test**: both halves load inside the window (still NextDNS's pages until Phase 7); nothing opens the
   browser by itself.
10. Pinch to zoom (Settings → Behaviour) on the dashboard: zooming works.

---

## 2026-11-20 — Handoff and a step-by-step guide for every remaining phase (docs only)

**What.** So that a new session can continue without this one:

- **`docs/PHASES.md` (new):** for every remaining phase (3 Site, 6 Status, 4 Theme, 5 Sign-in, D Downloads, 7 Ping,
  8 Texts, 9 Workflows, 10 Review, 11 Docs, 12 Owner): the goal, the owner's decisions, the files, the steps, the tests
  and mutation checks, how to verify, "done when", the phone tests to write; plus the rules for every phase, the tools
  and packages each phase needs (and which are not installed in the sandbox: apksigner 34, apksigcopier, JDK 17), and
  the LLM Central files to port with their sizes and test counts.
- **`handoff.md`** rewritten: state (incl. the rename and CI runs 1–4), Phase 3 in one paragraph, documents, a map of
  where things are, tools and packages, set-up/build/verify commands and the routine every commit follows, rules, open
  tasks and owner questions, limits, and a **structured prompt** for the next session (what to read in which order,
  how to set up and check the baseline, how to do and prove a phase).
- `PROJECT.md` §11 is now a summary table pointing to `docs/PHASES.md` (no duplicate); suggested order **3 → 6 → 4 →
  5 → D → 7…** (Phase 6 right after 3, because Phase 3 leaves the status icon half NextDNS); `CLAUDE.md` §1/§8/§11
  point to the guide.

**Found while writing it (checked in the code / repositories):** the LLM Central code to port is on its **`master`**
branch (`1102f48`; the old note named another branch); Phase 3 breaks exactly `SiteTest`, `TabManagerTest`,
`DownloadsTest` line 24 and `StatusRulesTest` lines 17–18 (`LoadErrorsTest` and `DataUrlTest` only use NextDNS
addresses as sample text).

**Verified.** Links and anchors of every document checked by script; build unchanged (`test lint assembleDebug
assembleRelease`: 92 tests, 0 lint issues). **Size.** App unchanged.

**What to test on your phone**

Nothing new. Answer **Q2** (Ping window) when you can: `docs/PHASES.md` → Phase 7.

---

## 2026-11-20 — Renamed to "AdGuardDNS Manager" (`org.rafee_m.adguarddnsmanager`)

**What.** The owner changed the identity chosen earlier the same day:

| | Before (this morning) | Now |
|---|---|---|
| Name (launcher, About, Settings, unlock prompt, README banner) | AdGuard Manager | **AdGuardDNS Manager** |
| Debug build's name | AdGuard Manager (Debug) | AdGuardDNS Manager (Debug) |
| applicationId | `org.rafee_m.adguardmanager` | **`org.rafee_m.adguarddnsmanager`** (`.debug` for debug builds) |
| Repository | `UpperPanda1/adguard-manager` | **unchanged for now**; the owner will rename it `adguard-dns-manager` later (PROJECT.md §7 lists what to change then) |

Also renamed: the Gradle project (`AdGuardDNSManager`), the tool package names, the release key file/alias in
`RELEASE.md` (`adguarddnsmanager`), the future APK name (`AdGuardDNS-Manager-<version>.apk`) and the F-Droid recipe
draft (`metadata/org.rafee_m.adguarddnsmanager.yml`). Earlier entries below keep the names they had at the time.

**Why it is safe now:** the applicationId may change freely **before the first public release**; nobody has the app
installed from a release. After the first release it is frozen.

**Verified.** Clean `test lint assembleDebug assembleRelease`: 92 tests, 0 lint issues. `aapt dump badging`: release
`org.rafee_m.adguarddnsmanager` / "AdGuardDNS Manager", debug `….debug` / "AdGuardDNS Manager (Debug)";
`BuildConfig.REPO_URL` still `https://github.com/UpperPanda1/adguard-manager`. Page checks 6 of 6. Banner regenerated
and looked at (the longer title fits). No old name or id left outside historical records (grep). **Not verified:** on a
phone.

**Size.** Release APK (clean build) **187,414 bytes** (unchanged).

**What to test on your phone**

1. If you installed an earlier debug build ("AdGuard Manager (Debug)"), uninstall it: the new id makes this a
   **different app**, so it would stay next to the new one.
2. Install this commit's debug APK: it is called **AdGuardDNS Manager (Debug)**; ⋮ → Settings → **About AdGuardDNS
   Manager** shows the name, and Source code still opens `UpperPanda1/adguard-manager`.

---

## 2026-11-20 — Documentation rewritten for AdGuard Manager (Phase 2, docs only)

**What.** Every document now describes **AdGuard Manager** instead of NextDNS Manager+, with the same depth, and
nothing of the NextDNS-only content is kept (owner's request):

| File | Now |
|---|---|
| `PROJECT.md` | The project guide: what the app is, the copy-not-fork decision, **all owner decisions** (name, id, repository, Q1–Q5), a **phase table** with status, where every file comes from, the file map with an "AdGuard phase" column, each feature with what changes for AdGuard, cache, how to change things (new: name/id, pointing at the service, re-checking site facts, porting fixes), versions, tests, limits, and **§11: what each remaining phase changes**, file by file, from the research |
| `CLAUDE.md` | Working rules (unchanged), build commands for this folder (the old file pointed at the NextDNS folder), hard constraints with the **sign-in exception** (Apple, Google, Facebook required; user-agent change only on Google's page), a "where to find what" table, verified facts (AdGuard site, this repository, shared code), TODO |
| `CHANGES.md` | This file, restarted for AdGuard Manager (NextDNS history: see the note above) |
| `handoff.md` | State, documents, where things are, rules, open tasks, the prompt for the next session |
| `REVIEW.md` | No AdGuard review yet (Phase 10); the review findings inherited from NextDNS Manager+ that still apply to the shared code |
| `BUILDING.md`, `RELEASE.md`, `fdroid.md`, `re-sign.md` | AdGuard Manager's names, id, repository, key names, F-Droid recipe draft, and its own **name/trademark** question; facts proven only for NextDNS Manager+ are marked so |
| `docs/research/…` | Owner answers to Q1–Q5 recorded; the plan's new path |
| `docs/superpowers/plans/2026-11-20-phase-2-identity.md` | The plan this session followed and its result |

**New fact, measured for this app:** two fresh clones of commit `1fa8540` at different paths build the **same**
unsigned release APK (SHA-256 `bb0699c7…0c1c5a`, JDK 21): the reproducible build F-Droid needs holds after the
identity change. (JDK 17 couldn't be tried: only JDK 21 is installed in the sandbox.)

**Why.** The copied documents described another app (its name, id, repository, website facts and history), which
would mislead the next session and the owner.

**Verified.** Every link and `#anchor` in the rewritten documents checked by script; every number (tests, files, lines,
APK size, hashes, commit ids) taken from a run or `git log` in this session; claims about AdGuard re-read against the
research file. Build unchanged: `test lint assembleDebug assembleRelease` pass (92 tests, 0 lint issues). **Not
verified:** anything on a phone.

**Size.** App unchanged.

**What to test on your phone**

Nothing new to install. Read `handoff.md` §8 (open tasks) and answer **Q2** (Ping window), PROJECT.md §11 Phase 7.

---

## 2026-11-20 — README for AdGuard Manager, banner and badges in the icon's blue (Phase 2)

**What.** The public page (`README.md`) now describes **AdGuard Manager**: why it exists, an honest **Status** table
(what is done, what is next: the app still opens NextDNS until Phase 3), the features (shared ones work; AdGuard
parts marked *planned*), a section on **Sign-in with Apple, Google or Facebook** that says openly that Google's page
gets a desktop user agent (plan §11, F-Droid), WebView limits, privacy, credits. The banner shows the new icon,
"AdGuard Manager" and "Every AdGuard DNS account, one private Android app"; every badge is re-coloured from the icon's
blue `#1A6BF4`.

**How.** `tools/readme-badges/generate.mjs`: `BRAND` = `#1A6BF4`, new banner texts. One fix the new icon needed: the
banner drew the icon with a transform made for the old 24-unit shield; it now keeps the icon group's own transform
and maps the icon's safe zone (21–87) onto the banner, so any future `icon.svg` fits. The title is sized by its
capitals ("ADGUARD"), because a sample with a "g" would have made it smaller. All 17 files in `artwork/readme/` are
generated (never edit them by hand).

**Verified.** Banner and all badges rendered in Chromium and looked at, light and dark. Every `#anchor` and local file
the README links to exists (script check). Each claim re-read against the research file; one battery sentence was
corrected (WebView slows a hidden page's timers, it doesn't stop them). **Not verified:** how GitHub itself renders it
(after the push).

**Size.** App unchanged.

**What to test on your phone**

1. Open the repository on GitHub in your phone's browser, in light and in dark mode: the banner and badges switch
   with it and nothing overlaps.

---

## 2026-11-20 — Own app icon for AdGuard Manager (Phase 2)

**What.** A new launcher icon: **two white server units** (each with a round status light and a slot) on a bright
blue `#1A6BF4`. The same drawing becomes the round and square launcher shapes, the one-colour "themed" icon of
Android 13+, the About screen's icon and the 512 × 512 store icon. Only `artwork/icon.svg` was edited; the other four
files are made from it by `tools/icon` (never edit those by hand).

**Why this drawing.** It must be the app's own artwork, not AdGuard's: AdGuard's logo is a green **shield**, and
NextDNS Manager+ already uses a shield, so a shield would look like either of them. A server rack is the common
picture for "DNS server". The blue is from the AdGuard dashboard's own colour scale (`--product-primary-60`, read from
its CSS in Phase 1), so the icon matches the website you see inside; white on it has a contrast of **4.72 : 1**
(computed). It is lighter than NextDNS Manager+'s `#0056B3`, so the two apps are easy to tell apart on one phone.
To change it: edit `artwork/icon.svg` (rules at the top of the file), then run the *App icon* workflow (PROJECT.md §7).

**Verified.** Generator: foreground spans 30–78 × 33–75, inside the safe zone 21–87; preview looked at (circle,
rounded square, themed) and the store icon. Running the generator again gives byte-identical files (what the App icon
workflow checks on push). Clean `test lint assembleDebug assembleRelease`: 92 tests, 0 lint issues (lint also checks
vector paths). **Not verified:** how phone launchers draw it (needs a phone); Android's path reader is assumed to read
the arcs like Chromium (they are written in the standard spaced form, not the compact form that LLM Central found
Android misreads).

**Size.** Release APK (clean build) 187,438 → **187,414 bytes** (−24).

**What to test on your phone**

1. Install this commit's debug APK. On the home screen and in the app drawer, the icon shows two white bars with dots
   on blue, inside your launcher's shape (circle, squircle…), nothing cut off.
2. If your phone has *themed icons* (Android 13+, Wallpaper & style → Themed icons): the icon turns one-coloured like
   the others.
3. ⋮ → Settings → About AdGuard Manager: the same icon at the top.

---

## 2026-11-20 — The app is now "AdGuard Manager" (Phase 2: name, id, repository, About)

**What.** The app's identity changed from NextDNS Manager+ to the owner's choice:

| | Before | Now |
|---|---|---|
| Name (launcher, About, Settings, unlock prompt) | NextDNS Manager+ | **AdGuard Manager** |
| Debug build's name | NextDNS Manager+ (Debug) | AdGuard Manager (Debug) |
| applicationId (the app's identity on the phone and in F-Droid) | `org.rafee_m.nextdnsmanagerplus` | **`org.rafee_m.adguardmanager`** (`.debug` for debug builds) |
| Repository (About → Source code, Star, Report an issue) | `UpperPanda1/nextdnsmanager-plus` | **`UpperPanda1/adguard-manager`** (`gradle.properties` `app.repoUrl`) |
| About → Built on | NextDNS Manager, LLM Central | **NextDNS Manager+** (this app's base), LLM Central, NextDNS Manager |
| About → note | Not affiliated with NextDNS | Not affiliated with AdGuard |

Also renamed: the Gradle project (`settings.gradle`) and the three tool packages (`tools/*/package.json`). The Java
package stays `org.dnsmanager.app` on purpose (plan §2: a NextDNS fix then applies here as an almost identical diff).

**Not yet changed (later phases of `docs/ADGUARD_PLAN.md`):** the app still **opens the NextDNS dashboard** and Settings
still says "NextDNS privacy policy" etc., because the service's name and addresses come from `site/Site.java`
(Phase 3). The icon is next (own commit), colours are Phase 4, the release workflow's file names Phase 9.

**Why.** Owner's decisions (2026-11-20): repository `adguard-manager`, name *AdGuard Manager*, id
`org.rafee_m.adguardmanager`. A new applicationId makes it a separate app from NextDNS Manager+: both can be installed
side by side. It must not change after the first public release.

**How it was found.** `grep -ri nextdns` over the whole tree, then each hit sorted into "identity" (this commit) or
"service" (`Site.NAME` fills those in at run time).

**Verified.** Clean `test lint assembleDebug assembleRelease`: 92 tests, 0 lint issues. `aapt dump badging`: release
`org.rafee_m.adguardmanager` / "AdGuard Manager", debug `org.rafee_m.adguardmanager.debug` / "AdGuard Manager (Debug)".
`BuildConfig.REPO_URL` = `https://github.com/UpperPanda1/adguard-manager`. No old id left in the code. Page checks 6 of
6 (About and Settings stand-ins now use the new name); About looked at in light and dark. **Not verified:** on a phone.

**Size.** Release APK (clean build) 187,234 → **187,438 bytes** (+204: the third credit row and longer names).

**What to test on your phone**

1. Install the debug APK from this commit's CI run (artifact `apks-<commit>`). It installs as a **new app** "AdGuard
   Manager (Debug)", next to NextDNS Manager+ (Debug) if you have it; your NextDNS accounts are not carried over.
2. Open it: it still shows the **NextDNS** dashboard (expected until Phase 3).
3. ⋮ → Settings → About AdGuard Manager: name "AdGuard Manager", "Developed by Rafee-M", source code
   `UpperPanda1/adguard-manager`; tap **Source code**, **Star** and **Report an issue**: each opens that repository on GitHub
   (public). "Built on" lists three apps; each opens its page.
4. Turn on App lock (Settings), leave the app for a minute, come back: the unlock prompt says "Unlock AdGuard Manager".

---

## 2026-11-20 — The AdGuard plan is in the repository (`docs/ADGUARD_PLAN.md`)

**What.** The owner's plan (phases 0–12, rules, testing, F-Droid, decisions) added **unchanged** (byte-compared with
the uploaded file), so every session reads the same plan. The owner's later answers are kept in PROJECT.md §2.2, not
edited into the plan. **Verified:** `cmp` with the upload. **Size.** App unchanged. **Phone:** nothing to test.

---

## 2026-11-19 — UPSTREAM.md: the port list with NextDNS Manager+

**What.** `UPSTREAM.md` records the NextDNS Manager+ commit this app was imported from (`eaa1665`) and has two tables
for fixes ported in either direction, with the rule for which folders count as shared code and how to port a fix
(`git format-patch` / `git am -3`; the Java package is the same in both apps). **Why.** Plan §7.5: a fix to shared code
must reach both apps. **Verified:** no NextDNS commits after `eaa1665` on its branch yet. **Size.** App unchanged.

---

## 2026-11-19 — AdGuard DNS: what the live website does (Phase 1 research, docs + a probe tool)

**What.** Before changing any code for AdGuard, every open question of the plan (`docs/ADGUARD_PLAN.md` §4–§5) was checked
on the **live** AdGuard website, not from memory. The results are a facts table with a date, a source and a "how
sure" mark for every row: `docs/research/2026-11-19-adguard-live-probe.md`. New development tool
`tools/site-probe/` re-checks the most important facts with one command each (sign-in chain, page colours, DNS
check), because websites change. The app itself is unchanged.

**What was found (the short version).**
- The dashboard is `adguard-dns.io`; you log in on `auth.adguardaccount.com`; account pages are on `adguardaccount.com`.
- **Sign in with Apple, Google and Facebook** opens the provider's page in the same tab (no pop-up window). AdGuard
  tells Google to refuse apps like ours (`disallow_webview=true`), so the plan's "disguise only Google's page" step
  is really needed.
- **"Am I using AdGuard DNS?"** works like NextDNS's test, but differently: the app asks for a made-up name that only
  AdGuard DNS can answer. Its answer has different fields (e.g. `protocol: "dot"` in lower case).
- **There is no ping page**: the Ping window has nothing to show.
- **Colours:** white page, dark page `#242d38`, blue accent `#3c81f6`.
- **One problem found early:** "Export rules" makes a `blob:` file, which the copied NextDNS code can't save.

**Why.** Phase 1 of the plan: nothing is coded from memory. Five things only you can decide came out of it (§3 of the
research file, Q1–Q5), and six things only a phone can show (§4, P1–P6).

**Verified.** Every row was re-read against the saved evidence before committing (three rows were corrected that
way). `tools/site-probe` was run in all three modes against the live site, and a wrong argument gives a clear message.
`test lint assembleDebug assembleRelease` still pass (92 tests, 0 lint issues). **Not verified:** anything that needs
a phone or a signed-in account (research file §4).

**Size.** App unchanged (no app files touched).

**What to test on your phone**

Nothing to install yet. To help Phase 5 and the downloads question, if you have an AdGuard DNS account:

1. In your phone's browser, open the AdGuard DNS dashboard, go to a server's **user rules** and tap **Export**. Note
   whether a file `rules.txt` is saved (this is the `blob:` download the app will have to handle).
2. On a device's page, copy the **Apple configuration profile** download link (long-press → copy link) and open it in a
   private browser tab where you are **not** logged in. Does the file download? (Tells us if the app can hand such
   links to the browser; research file P4.)

---

---

## 2026-11-19 — AdGuard Manager starts as an exact copy of NextDNS Manager+ (Phase 0)

**What.** One commit (`abb10bf`) with the whole NextDNS Manager+ tree at commit `eaa1665`, **unchanged** and without
its history. From here on every commit is one AdGuard adaptation, so each can be reviewed, reverted and compared with
NextDNS. The newest NextDNS state was on branch `claude/nextdns-manager-plus-review-el0q2u` (the checked-out branch was
older and didn't match the plan's numbers).

**Why.** Plan §2: a new repository copied once (not a GitHub fork, not Gradle flavors), so each app stands alone.

**Verified.** The git tree hash of the import (`11ec3ad6…`) equals NextDNS's at `eaa1665`; clean `test lint
assembleDebug assembleRelease`: 92 tests, 0 lint issues; page checks 6 of 6; CI run 1 and App icon run 1 green on
GitHub, no `##[warning]` lines.

**Size.** Release APK (clean build) **187,234 bytes**.

**What to test on your phone**

Nothing yet: at this commit the app is NextDNS Manager+ with the same id. Phone tests start with Phase 2.
