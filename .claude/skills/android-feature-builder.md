---
name: android-feature-builder
description: Design and implement a new Android app feature from idea to production-ready code using Clean MVVM with Jetpack Compose.
---

# Android Feature Builder

## Goal
Take a feature idea and produce a structured implementation plan with architecture guidance, Kotlin code examples, and edge case considerations — ready to build.

## Instructions

### Step 1 — Clarify
If the request is vague, ask:
- What problem does this feature solve for the user?
- Is it a UI feature, background process, or data operation?
- Does it require persistence (Room), preferences (DataStore), or network calls?
- Are there premium/free tier implications?

### Step 2 — Break Down the Feature
Decompose into layers:
- **UI**: What screens or components are needed? (Compose screens, dialogs, bottom sheets)
- **ViewModel**: What state does it expose? What events does it handle?
- **Domain**: Are new use cases or domain models needed?
- **Data**: Are new DAOs, entities, or repository methods required?
- **DI**: What needs to be wired in `AppModule.kt`?
- **Navigation**: Does it need a new route in `NavGraph.kt`?

### Step 3 — Architecture Guidance
Apply Clean MVVM:
```
Screen (Compose) → ViewModel (StateFlow<UiState>) → UseCase → Repository → DAO (Flow<Entity>)
```
- UI observes `uiState: StateFlow<UiState>` via `collectAsStateWithLifecycle()`
- ViewModels use `@HiltViewModel` + `@Inject constructor`
- Repositories map entities to domain models via `Mappers.kt`
- Side effects (navigation, snackbars) go through `SharedFlow` events

### Step 4 — Sample Code
Provide Kotlin snippets for the most complex or non-obvious parts:
- New `UiState` data class
- ViewModel with relevant state and event handlers
- Room entity/DAO additions if needed
- Compose screen scaffold

### Step 5 — Edge Cases & Risks
Consider:
- Empty states and loading states
- Error handling (DB failures, validation)
- Premium gating (check billing status before enabling feature)
- Android lifecycle (don't collect flows in non-lifecycle-aware scopes)
- Database migrations if schema changes (bump DB version, add `Migration` object)

## Context
- Project uses Clean MVVM + Hilt + Jetpack Compose + Room + DataStore
- Min SDK 26, Target SDK 34, Kotlin 1.9.21
- Navigation: sealed class `Screen` routes in `NavGraph.kt`
- DI wiring: `di/AppModule.kt`
- Premium features guarded by `BillingManager` + DataStore flag

## Output Format
1. **Feature summary** — one paragraph describing what will be built
2. **Layer breakdown** — bullet list per layer (UI / ViewModel / Domain / Data / DI / Nav)
3. **Implementation steps** — numbered, ordered by dependency
4. **Code snippets** — only for non-trivial parts
5. **Edge cases** — bulleted list
6. **Open questions** — anything that needs a decision before coding starts
