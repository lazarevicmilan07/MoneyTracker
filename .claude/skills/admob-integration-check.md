---
name: admob-integration-check
description: Audit an Android app's AdMob integration for correctness, policy compliance, and UX-friendly placement.
---

# AdMob Integration Check

## Goal
Review the AdMob setup in an Android project and identify misconfigurations, policy risks, and UX problems before they cause revenue loss or app suspension.

## Instructions

### Step 1 — Gather Files
Read the following before auditing:
- `app/build.gradle.kts` — check ad unit IDs per build variant
- `AndroidManifest.xml` — check `com.google.android.gms.ads.APPLICATION_ID` meta-data
- All files that reference `AdView`, `InterstitialAd`, `RewardedAd`, or `MobileAds`
- `BillingManager.kt` or equivalent — check if premium users bypass ads

### Step 2 — Checklist

#### Setup
- [ ] `MobileAds.initialize()` called once in `Application.onCreate()`, not in Activity/Fragment
- [ ] `APPLICATION_ID` in `AndroidManifest.xml` matches AdMob dashboard app ID
- [ ] Test ad unit IDs used in debug builds, production IDs only in release builds
- [ ] Test device registered via `RequestConfiguration` during development
- [ ] No hardcoded production ad unit IDs in source code without build-variant separation

#### Ad Unit Usage
- [ ] Banner: `AdView` sized with `AdSize.BANNER` or adaptive banner
- [ ] Interstitial: loaded in advance, shown only on natural transition points (not on back press)
- [ ] Rewarded: user explicitly opts in, reward granted only in `onUserEarnedReward` callback
- [ ] Native: layout matches app design, ad label clearly visible ("Ad" or "Sponsored")
- [ ] No auto-refreshing banner placed where user interaction is likely (violates policy)

#### Policy Compliance
- [ ] Ads not placed near interactive elements (buttons, nav bars) with < 32dp clearance
- [ ] No programmatic click simulation or incentivized banner clicks
- [ ] Interstitials not shown on app open (first launch), on back press, or more than once per 30s
- [ ] Content rating set correctly in AdMob dashboard for the app's audience
- [ ] GDPR/CCPA consent flow implemented if targeting EU/California users (UMP SDK or equivalent)
- [ ] `tagForChildDirectedTreatment` set correctly if app is for children (COPPA)

#### Premium / Ad Removal
- [ ] Ads completely hidden (not just made invisible) when user has premium
- [ ] Ad loading skipped (not just display) for premium users to avoid unnecessary network calls
- [ ] `AdView.destroy()` called in `onDestroy()` to prevent memory leaks

#### Performance
- [ ] Banner ads loaded after main content is visible, not blocking first render
- [ ] `AdView` destroyed and recreated on configuration change (or handled via `rememberSaveable`)
- [ ] No ad loading on every recomposition in Compose — loading triggered once, not in composable body

### Step 3 — Common Rejection Reasons
- Ads placed where accidental clicks are likely (overlapping content, floating over UI)
- Interstitials shown during gameplay or blocking user flow without clear dismissal
- Invalid traffic: test ads left in production, or clicks from automated tools
- Missing consent for personalized ads in GDPR regions
- Incorrect `APPLICATION_ID` causing `IllegalStateException` crash on init

### Step 4 — UX Recommendations
- Prefer **adaptive banners** over fixed-size — better fill rate and revenue
- Place banners at the **bottom** of screens, anchored above the navigation bar
- Show interstitials at natural pause points: after completing a task, before a new section
- Always give users a clear way to dismiss rewarded ad prompts — no dark patterns
- Consider frequency capping for interstitials in AdMob dashboard (e.g., max 1/hour)

## Context
- Project uses `BillingManager.kt` with `premium_unlock` one-time purchase
- Build variants define test vs production ad unit IDs in `build.gradle.kts`
- Jetpack Compose UI — ad views embedded via `AndroidView` composable

## Output Format
1. **Checklist results** — each item marked Pass / Fail / Not Found
2. **Issues found** — grouped by severity (Critical / Warning / Info)
3. **Recommendations** — actionable fixes with file references where possible
4. **Policy risks** — anything that could trigger a policy violation or account suspension
