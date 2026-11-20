# Phase 2 — Identity (plan and result)

**Goal** (`docs/ADGUARD_PLAN.md` §6, Phase 2): the app becomes *AdGuard Manager* — name, applicationId, repository
address and author, icon, About credits, README with new badges and banner — and the documentation describes this app.
**Done when:** it builds; About shows the new repository and Rafee-M; the icon check passes.

**Owner input (2026-11-20):** repository `adguard-manager`, name *AdGuard Manager*, id `org.rafee_m.adguardmanager`;
Q1 port blob downloads, Q2 "why remove Ping?" (open), Q3 page theme as is, Q4 account pages inside / checkout in the
browser, Q5 Apple + Google + Facebook; add the plan to the repository; rewrite the docs in depth for AdGuard Manager,
no NextDNS-only content kept; one commit per feature.

## Steps

| # | Step | Commit | Verified by |
|---|---|---|---|
| 1 | Add the plan unchanged (`docs/ADGUARD_PLAN.md`) | `2c2cf8b` | `cmp` with the upload |
| 2 | Inventory: `grep -ri nextdns` over the tree; sort each hit into identity (now) or service (`Site.NAME`, Phases 3–9) | — | the list in PROJECT.md §3 |
| 3 | Identity: `app_name`, debug name, `applicationId`, `rootProject.name`, `app.repoUrl`, About fallback name + "Built on" + note, page-check stand-ins, tool package names | `8d6c321` | clean `test lint assembleDebug assembleRelease` (92/0); `aapt dump badging` (package + label, debug and release); `BuildConfig.REPO_URL`; no old id left; page checks 6/6; About screenshots light/dark |
| 4 | Icon: own drawing in `artwork/icon.svg` → `tools/icon` | `9197934` | safe zone (30–78 × 33–75 of 21–87); preview + store icon looked at; regenerating is byte-identical; contrast 4.72:1; build + lint |
| 5 | README + `tools/readme-badges` (`BRAND`, banner texts, banner reads any icon) → `artwork/readme/` | `1fa8540` | banner/badges rendered and looked at (light/dark); README anchors + local links by script; claims vs research file |
| 6 | Push; CI and App icon runs | — | GitHub Actions |
| 7 | Reproducibility of this app: two fresh clones of the pushed commit, different paths | — | same SHA-256 `bb0699c7…0c1c5a` (JDK 21) |
| 8 | Documentation rewritten for AdGuard Manager (`PROJECT.md`, `CLAUDE.md`, `CHANGES.md`, `handoff.md`, `REVIEW.md`, `BUILDING.md`, `RELEASE.md`, `fdroid.md`, `re-sign.md`, research file, `UPSTREAM.md`, this plan) | docs commit | links/anchors by script; numbers from runs and `git log` |

**Ordering decision:** identity code first, docs rewrite last, so every commit's documents are true at that commit
(a docs rewrite first would have described a name and id the code didn't have yet).

## Findings on the way

- `gradle.properties` has Windows line endings: the first `sed` silently missed the `app.repoUrl` line (seen in
  `git diff --stat`, fixed with a `\r`-aware pattern).
- The README banner drew the icon with a transform made for the old 24-unit shield; it now honours the icon group's
  transform and maps the icon's safe zone, so any valid `icon.svg` fits.
- The repository is **public** (an early draft of a phone step said "private"; checked and corrected).
- "AdGuard Manager" may read as an official AdGuard app: recorded as F-Droid risk 1 (`fdroid.md` §3.2).
- A README sentence said hidden pages' timers "only run while shown"; WebView throttles them instead: corrected.

## Left for later phases

`site/Site.java` (3), `theme/Themes.java` (4), sign-in (5), status (6), Ping (7, owner Q2), blob downloads (D), texts and
store files (8), `release.yml` names (9). Exact contents: PROJECT.md §11.
