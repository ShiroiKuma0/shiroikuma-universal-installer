# In-app review (Play Store rating prompt)

The Play build can ask for a rating through Google's in-app review sheet. The open-source build
never does, and ships none of the library that would show it.

| Flavor | In-app review | Manual "Rate" row in About |
| :--- | :--- | :--- |
| `opensource` | none — `NoOpReviewPrompter`, no Play Services libraries | present, opens the store page in a browser |
| `play` | Google's review sheet, gated by `ReviewGate` | same row, unchanged |

`com.google.android.play:review-ktx` is declared as `playImplementation` for exactly the reason
Firebase is (see [FIREBASE.md](FIREBASE.md)): it is a closed Play Services artifact, and
`opensource` is what we publish as open source. `review-ktx` pulls `review` in transitively, so
one dependency line covers both. **Never move it to a plain `implementation(...)`.**

## The seam

```
app/src/main/…/review/AppReview.kt                  # ReviewPrompter, NoOpReviewPrompter, AppReview
app/src/main/…/review/ReviewGate.kt                 # when to ask, and the announcement
app/src/play/…/review/ReviewPrompterFactory.kt      # Google's ReviewManager
app/src/opensource/…/review/ReviewPrompterFactory.kt # no-op
```

Identical in shape to `Telemetry`: code under `src/main` only ever talks to `AppReview`, and each
flavor source set defines `createReviewPrompter` under the same package. `App.onCreate` binds it,
one line below `Telemetry.install`.

`AppReview.isAvailable` says whether this build can show a sheet at all — it is about the build,
not about whether Play will actually show something. Nothing else can know that; see below.

## Where the ask happens, and where it must not

The ask fires in **`InstallActivity`**, after an install has succeeded and the screen has gone
quiet: no sessions left running, no pending APK, no dialog stage. `ReviewGate.opportunities` is
announced from the install controllers; `InstallActivity` is the only collector, and only while
`RESUMED`.

That flow has three deliberate properties:

- **An install that finishes with no screen in front is dropped, not queued.** `opportunities` has
  no replay and one slot of buffer, so an install completed from a notification never surfaces an
  ask later, out of context.
- **The ask waits for the screen to settle** (up to 30s). The success lands while its session card
  is still up, and a batch may have more installs behind it. Talking over that is exactly the
  interruption the policy is about.
- **`DialogInstallActivity` is not a candidate**, even though it is where the word "Success"
  appears. It calls `finish()` immediately after, the user is mid-flow in whatever app handed us
  the APK, and the sheet would die with the activity. It is the tempting wrong answer.

Other completed tasks — batch install, APK backup, LAN share — are plausible second homes, but
Play's quota means more call sites do not buy more reviews, only harder debugging. Left at one on
purpose.

## The gate

`ReviewGate` persists four preferences in the shared DataStore:

| Key | Meaning |
| :--- | :--- |
| `review_first_launch_at` | stamped on first run, so an ask can't hit a brand-new install |
| `review_successful_installs` | successful installs, counted independently of `install_history` |
| `review_last_prompt_at` | when we last asked |
| `review_prompt_count` | how many times we have asked, ever |

Thresholds: **≥ 3 successful installs**, **≥ 3 days** since first launch, **≥ 90 days** between
asks, **at most 3 asks** for the lifetime of the install.

The install count is its own counter rather than `SELECT COUNT(*) FROM install_history WHERE
success = 1`, because the user can clear that history from the Install screen — which clears their
history, not the fact that the app worked for them.

## Rules that shaped this

Google's policy, not our preference:

- **The sheet may not be attached to a button.** The "Rate" row in `AboutScreen` therefore keeps
  opening `https://play.google.com/store/apps/details?id=…` and must not be switched to the
  in-app flow.
- **No question of our own before it** — no "Enjoying the app?" pre-prompt.
- **Play rate-limits it.** Over quota, or for a user who already reviewed, the call shows nothing
  and still reports success. There is no callback saying whether the sheet appeared or whether a
  review was left, so nothing in the app may branch on the outcome.
- The sheet needs the Play Store app on the device and a resumed activity that outlives it.

`feature_used` with `feature = review_prompt` records that an ask was made — opportunities, not
reviews. It only distinguishes "the gate never opens" from "it opens and nothing comes of it".

## Testing

- `FakeReviewManager` (same artifact, `com.google.android.play.core.review.testing`) always
  succeeds immediately — useful for exercising the flow in a debug build.
- For the real sheet: internal app sharing or the internal testing track. An account that has
  already reviewed the app sees nothing, which is indistinguishable from a bug.
- Faster than waiting out the gate: temporarily lower the constants in `ReviewGate`.
- The artifact published to Play **must be the `play` flavor** (`./gradlew :app:bundlePlayRelease`).
  An `opensource` AAB uploaded to Play contains none of this, and the symptom — "the sheet never
  shows" — looks exactly like a policy or quota problem. Verify before uploading:

  ```bash
  unzip -p app/build/outputs/bundle/playRelease/app-play-release.aab base/resources.pb \
    | strings | grep 1:466719970426
  ```
