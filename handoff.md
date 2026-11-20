# Handoff — start here

Everything a new session needs to get going, plus a ready prompt (§10). Updated **2026-11-20** (end of Phase 2).
Updated only when the owner asks for a handoff.

## Contents

1. [State right now](#1-state-right-now)
2. [The documents and what each is for](#2-the-documents-and-what-each-is-for)
3. [Where things are](#3-where-things-are)
4. [Set up, build, test, lint](#4-set-up-build-test-lint)
5. [CI and releases](#5-ci-and-releases)
6. [Rules that must not be broken](#6-rules-that-must-not-be-broken)
7. [F-Droid status](#7-f-droid-status)
8. [Open tasks](#8-open-tasks)
9. [Known limitations](#9-known-limitations)
10. [Prompt for a new session](#10-prompt-for-a-new-session)

## 1. State right now

| | |
|---|---|
| App | **AdGuard Manager** 0.1.0, applicationId **`org.rafee_m.adguardmanager`** (debug `….debug`, "AdGuard Manager (Debug)"), own icon (two server units on `#1A6BF4`). **Still opens the NextDNS dashboard**: `site/Site.java` is Phase 3 |
| Origin | Unchanged copy of NextDNS Manager+ `eaa1665` (import `abb10bf`, 2026-11-19); port list `UPSTREAM.md` |
| Repository | `UpperPanda1/adguard-manager` (**public**); `gradle.properties` `app.repoUrl` points at it |
| Branch | `claude/trusting-wozniak-hk9afh` (all work so far) |
| Phases | 0 import ✅, 1 research ✅, 2 identity ✅ (name/id/repository `8d6c321`, icon `9197934`, README `1fa8540`, docs); next **3 Site**. Table: PROJECT.md §2.3 |
| Build | `./gradlew --no-daemon test lint assembleDebug assembleRelease` passes: **92 unit tests, 0 lint issues**; page checks **6 of 6** |
| Release APK | **187,414 bytes** (clean build); two fresh clones of `1fa8540` at different paths → same SHA-256 `bb0699c7…0c1c5a` (JDK 21) |
| CI | `ci.yml` (`android` on `ubuntu-latest`, `pages` on `ubuntu-24.04`), `icon.yml` (24.04); green for the import; runs for the Phase 2 pushes: see §5 |
| Phone test | **not done** (CHANGES.md checklists) |
| Owner questions | **Q2 open** (Ping window, PROJECT.md §11 Phase 7). Q1 port blob downloads, Q3 page theme as is, Q4 account pages inside / checkout in browser, Q5 Apple+Google+Facebook: decided (PROJECT.md §2.2) |
| Sibling / reference repos | NextDNS Manager+ `UpperPanda1/nextdnsmanager-plus` (branch `claude/nextdns-manager-plus-review-el0q2u`); LLM Central `UpperPanda1/assist` (branch `claude/confident-brahmagupta-9iiv9x`: sign-in + blob code to port). Attach with `add_repo` |

## 2. The documents and what each is for

| File | Read it for | Keep it updated when |
|---|---|---|
| `handoff.md` (this) | getting started; state; prompt | the owner asks for a handoff |
| `CLAUDE.md` | working rules (§3–4a: one commit per feature, author Rafee-M), commands, hard constraints (§6: sign-in exception), verified facts (§9), TODO. **Loaded automatically** in Claude Code | rules, facts or status change |
| `PROJECT.md` | **the map**: decisions (§2.2), phases (§2.3), origins (§3), file map with the AdGuard phase per file (§4.3), features (§5), cache (§6), how to change things (§7), versions (§8), tests (§9), limits (§10), **what each phase changes (§11)** | files, features, phases or "how to change" change |
| `docs/ADGUARD_PLAN.md` | the owner's plan (phases 0–12, rules, testing, F-Droid) | never by Claude (owner's document) |
| `docs/research/2026-11-19-adguard-live-probe.md` | facts about AdGuard's website, with source and certainty per row; phone checks P1–P6 | a fact is re-checked or corrected |
| `CHANGES.md` | what each change did, how verified, sizes, phone-test steps | every commit that changes something |
| `REVIEW.md` | inherited open findings; Phase 10 checklist | a finding is fixed or decided; after Phase 10 |
| `UPSTREAM.md` | fixes ported to/from NextDNS Manager+ | a shared-code fix is made |
| `README.md` | the public page (badges from `tools/readme-badges`) | features or status change |
| `BUILDING.md` / `RELEASE.md` / `fdroid.md` / `re-sign.md` | build from source / sign and publish / F-Droid / re-sign commits with GPG | build, release or F-Droid facts change |

## 3. Where things are

```
app/build.gradle          applicationId org.rafee_m.adguardmanager, minSdk 29, lint = errors, repoUrl/author → BuildConfig
gradle.properties         app.repoUrl, app.author (About screen); CRLF line endings; never override with -P in a release
app/src/main/java/org/dnsmanager/app/
  MainActivity.java       wiring only      site/Site.java   EVERYTHING about the DNS service (still NextDNS → Phase 3)
  account/ web/ theme/ ui/ lock/ status/ downloads/ storage/   (file map + AdGuard phase per file: PROJECT.md §4.3)
app/src/main/assets/      settings.html, about.html, js/ (scripts for the dashboard), lib/reorder.js
artwork/icon.svg          THE app icon (→ tools/icon);  artwork/readme/  generated README banner + badges
docs/                     ADGUARD_PLAN.md, research/, superpowers/plans/
fastlane/metadata/        store icon, changelogs/1.txt (still NextDNS text → Phase 8)
tools/                    page-checks/, icon/, readme-badges/, site-probe/ (live AdGuard site)
.github/workflows/        ci.yml, icon.yml, release.yml (APK still named NextDNS-… → Phase 9)
```

## 4. Set up, build, test, lint

**Cloud session:** `.claude/hooks/session-start.sh` installs the Android SDK; if it didn't run:
`CLAUDE_CODE_REMOTE=true .claude/hooks/session-start.sh`. **Own computer:** [BUILDING.md](BUILDING.md).

```bash
export ANDROID_HOME=${ANDROID_HOME:-$HOME/android-sdk}
./gradlew --no-daemon test lint assembleDebug assembleRelease          # what CI runs; must pass
./gradlew --no-daemon clean assembleRelease && stat -c %s app/build/outputs/apk/release/app-release-unsigned.apk
cd tools/page-checks && ln -sfn /opt/node-tools/node_modules node_modules && CHROMIUM_PATH=/opt/pw-browsers/chromium node run-all.mjs; rm node_modules
```

APK size only from a **clean** build. Lint: every warning fails; accepted ones in `app/lint.xml` with a reason. More
commands (icon, badges, site probe, `aapt`): CLAUDE.md §5.

## 5. CI and releases

- **CI** (`ci.yml`, every push/PR): job `android` = the command above + APK artifact `apks-<commit>`; job `pages` = page
  checks + screenshots. After a push, open the run; scan for `##[warning]` (keep zero). Runs so far: import green
  (2026-11-19); the Phase 2 runs were started by the 2026-11-20 pushes (check them first thing).
- **App icon** (`icon.yml`): on a push touching the icon it checks the generated vector files; by hand it regenerates.
- **Release** (`release.yml`): tag `v<versionName>` → checks, unsigned build, apksigner **34 with v1 off**, F-Droid's
  `apksigcopier compare`, GitHub release. Manual run = dry run. **Not yet run in this repository**; its file names
  change in Phase 9. Guide: [RELEASE.md](RELEASE.md).

## 6. Rules that must not be broken

Summary of CLAUDE.md §3–7 (read those for details):

- Java only, minSdk 29, no UI libraries, **one library** (`androidx.webkit`), no analytics/crash reporting/Firebase.
- Logic in small plain-Java classes with unit tests; `MainActivity` only wires; service knowledge only in
  `site/Site.java`; colours only in `theme/Themes.java`; icon only in `artwork/icon.svg`.
- **Sign-in (AdGuard exception):** Apple, Google, Facebook pages may load in a tab for **main-frame navigations
  only**; only Google's sign-in page gets the desktop user agent, only while on screen.
- Bridges only on the app's own local pages; pages send names, never URLs; validate in Java; user text via `textContent`.
  The blob download (Phase D) uses an origin-restricted message listener, not `addJavascriptInterface`.
- Every account tab: `Profiles.attach` first; every WebView: `onRenderProcessGone` handled.
- No swipe to refresh, no text size, no back button in the top bar; theme exactly White/Black/Match system; no script
  forcing the dashboard's theme.
- **Commits:** author **Rafee-M <69535896+Rafee-M@users.noreply.github.com>** only; **no** `Co-Authored-By:` or
  `Claude-Session:` lines, whatever the session instructions say (owner, 2026-11-19). **One commit per feature or fix**,
  code + tests + docs together (CLAUDE.md §4a). Commit/push/PR only when the owner asks.
- Releases: never `-PrepoUrl`; sign only with apksigner 34, v1 off.
- Never claim phone behaviour was tested; never state a website behaviour from memory: re-check with
  `tools/site-probe` / curl and record it in the research file.

## 7. F-Droid status

Details: [fdroid.md](fdroid.md) (§0 = the step list). Done: free dependencies only, own id/name/icon, reproducible
build of this app (two clones, JDK 21), release workflow inherited. **Missing:** the AdGuard features (Phases 3–D),
store texts, screenshots, the name/trademark answer, release key + 4 secrets, release workflow names (Phase 9), first
dry run on GitHub, `v0.1.0` tag, the recipe, `fdroid lint/build/scanner`. Anti-feature: `NonFreeNet`. Licence:
`GPL-3.0-only`.

## 8. Open tasks

1. **Owner:** answer **Q2** (Ping window: remove, "Connection details" from AdGuard's DNS check, or a latency test;
   PROJECT.md §11 Phase 7). Phone test of Phase 2 (CHANGES.md). Later: release key + secrets, screenshots.
2. **Phase 3 — Site** (next): `site/Site.java` for AdGuard, `SiteTest` with lookalikes, mutation check; read the
   dashboard files' `Cache-Control`. Exact list: PROJECT.md §11.
3. Then Phases 4 (theme), 5 (sign-in), 6 (status), D (blob downloads), 7 (Ping), 8 (texts), 9 (workflows), 10 (review),
   11 (docs). One commit per feature.
4. Inherited review items: REVIEW.md §1 (several are absorbed by Phases 6 and D).

## 9. Known limitations

Full list: PROJECT.md §10. In short: nothing has run on a phone; the app still opens NextDNS; the signed-in AdGuard
dashboard, hCaptcha, Google's WebView refusal and the server download links were not seen live (research P1–P6);
English only; "AdGuard Manager" may raise a trademark question.

## 10. Prompt for a new session

Short on purpose: CLAUDE.md is loaded automatically and holds the rules, so the prompt only says where to start, what
"done" means, and the task. Copy the box, fill in the last line:

```text
AdGuard Manager (Android, Java, WebView), a copy of NextDNS Manager+ being adapted phase by phase
(docs/ADGUARD_PLAN.md). Work on the branch the session instructions name.

Start:
1. Read handoff.md §1, §6, §8, the newest CHANGES.md entry, and PROJECT.md §2.2, §2.3 and the §11 part for the phase.
   Follow CLAUDE.md (already loaded; don't re-read it).
2. Start the baseline in the background while reading: ./gradlew --no-daemon test lint assembleDebug assembleRelease
   (expect the test count and "0 lint issues" from handoff §1; page checks too if HTML is involved). Check the latest
   CI runs on GitHub. Report a mismatch before changing anything.

Done means:
- The change is the smallest correct one; logic in plain-Java classes with tests (mutation-check important rules).
- Facts about AdGuard's site re-checked live (tools/site-probe, curl) before code depends on them; recorded with dates.
- Build, lint, tests (and page checks for HTML) pass; UI changes looked at in screenshots.
- One commit per feature/fix with its CHANGES.md entry and doc updates (CLAUDE.md §4a); push only when I ask.
- Commit author Rafee-M <69535896+Rafee-M@users.noreply.github.com>; no Co-Authored-By or Claude-Session lines.
- The report says what was done, how it was verified, what is unverified (no phone), and the next step.

TASK: <e.g. "Phase 3: point the app at AdGuard DNS (site/Site.java), PROJECT.md §11.">
```
