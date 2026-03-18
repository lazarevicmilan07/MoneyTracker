---
name: user-feedback-analyzer
description: Analyze user reviews and feedback to extract patterns, prioritize issues, and generate actionable product improvements.
---

# User Feedback Analyzer

## Goal
Turn a raw batch of user reviews or feedback into structured, prioritized insights — separating noise from signal, quick wins from major investments.

## Instructions

### Step 1 — Collect Input
Ask the user to paste:
- Play Store reviews (copy/paste or export)
- Support emails or in-app feedback
- Social media comments or Reddit posts
- Any other user-generated text

Note the volume: under 20 reviews = qualitative only; 20+ = patterns can be detected.

### Step 2 — Categorize Feedback
Group every piece of feedback into one of these categories:

| Category | Description |
|---|---|
| **Bug / Crash** | App doesn't work as expected |
| **Performance** | Slow, laggy, battery drain, high memory |
| **UX / Usability** | Confusing, hard to find, too many taps |
| **Missing Feature** | User wants something that doesn't exist |
| **Existing Feature Improvement** | Feature exists but needs refinement |
| **Pricing / Monetization** | Complaints about cost, ads, paywalls |
| **Data / Privacy** | Concerns about data handling, permissions |
| **Positive / Praise** | What's working well (keep doing this) |
| **Off-topic / Spam** | Irrelevant or not actionable |

### Step 3 — Detect Patterns
For each category with 2+ items:
- Count frequency (how many users mentioned it)
- Note exact phrases users repeat (direct quotes are gold)
- Flag if a pattern correlates with a specific OS version, device type, or app version

Signal boosters:
- Low rating (1–2 stars) + specific complaint = high priority
- Multiple users use identical language = systemic issue, not one-off
- Feature request appears in 3+ reviews = real demand signal

### Step 4 — Classify by Effort vs Impact

```
HIGH IMPACT + LOW EFFORT  → Quick Wins (do first)
HIGH IMPACT + HIGH EFFORT → Major Projects (plan carefully)
LOW IMPACT + LOW EFFORT   → Nice to Have (do if time allows)
LOW IMPACT + HIGH EFFORT  → Deprioritize (skip or defer)
```

Effort estimation heuristics:
- Bug fix in existing code = Low
- New screen or flow = Medium
- New data layer / architecture change = High
- Third-party integration = Medium–High

### Step 5 — Feature Idea Extraction
From "Missing Feature" and "Existing Feature Improvement" categories:
- Reframe requests as user needs: "I want X" → "User needs to accomplish Y"
- Group similar requests into one feature concept
- Flag if a request aligns with premium feature opportunities (monetization angle)

### Step 6 — What's Working (Don't Break It)
Explicitly call out praised features and UX elements. These are anchors — be careful not to remove or significantly change them in future updates.

## Context
- This is a personal finance / expense tracker app
- Core users care about: simplicity, speed, reliability, data privacy
- Monetization sensitivity is high — users are wary of paywalls and excessive ads
- Indie app — responses to reviews are personal and can turn negative reviewers into advocates

## Output Format

### Summary
- Total feedback items analyzed
- Overall sentiment breakdown (% positive / neutral / negative)

### Categorized Feedback
For each category: count, sample quotes, key pattern description

### Key Insights
3–5 bullet points — the most important things to know

### Recommended Actions
Prioritized table:

| Priority | Action | Category | Effort | Impact |
|---|---|---|---|---|
| 1 | Fix [specific bug] | Bug | Low | High |
| 2 | Add [feature] | Missing Feature | Medium | High |
| ... | | | | |

### Feature Ideas
Bulleted list of validated feature concepts with user demand evidence

### What's Working — Don't Change
Bulleted list of praised elements
