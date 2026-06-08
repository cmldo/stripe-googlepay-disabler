# stripe-googlepay-disabler

An **LSPosed module** that forces Stripe's `CustomerSheet.Configuration.Builder.googlePayEnabled()` to always behave as if `false` was passed — at the framework level, with no modifications to the target app.

---

## How it works

The module uses the Xposed API to hook `CustomerSheet$Configuration$Builder.googlePayEnabled()` in any process that loads the Stripe Android SDK. Before the original method runs, the argument is overwritten to `false`, so the Builder's internal state is set to disabled regardless of what the host app intended.

```
Host app calls:
  CustomerSheet.Configuration.Builder().googlePayEnabled(true)

Hook intercepts:
  args[0] = true  →  args[0] = false

Original method runs with false — Google Pay is disabled.
```

Both overloads are covered:

| Overload | Stripe SDK version |
|---|---|
| `googlePayEnabled(Boolean)` | 20.x+ (Kotlin nullable) |
| `googlePayEnabled(boolean)` | Older / Java callers |

---

## Requirements

| Component | Version |
|---|---|
| Android | 9+ (API 28+) |
| LSPosed | 1.8+ (xposedminversion 93) |
| Stripe SDK | 20.x (tested), 19.x (untested but likely works) |

---

## Installation

### Option A — download pre-built APK

1. Go to the [**Releases**](https://github.com/cmldo/stripe-googlepay-disabler/releases) page.
2. Download the latest `stripe-googlepay-disabler-vX.Y.Z.apk`.
3. Install it on your device.

### Option B — build from source

```bash
git clone https://github.com/cmldo/stripe-googlepay-disabler.git
cd stripe-googlepay-disabler
./gradlew assembleRelease
# APK → app/build/outputs/apk/release/app-release.apk
```

### Activation

1. Open **LSPosed Manager → Modules**.
2. Enable **Stripe GooglePay Disabler**.
3. Tap the module, go to **Scope**, and add the app(s) that embed the Stripe SDK.
4. Force-stop (or reboot) the target app.
5. Verify via logcat:
   ```
   adb logcat -s StripeGPDisabler
   ```

---

## Scope

You need to set the scope in LSPosed Manager to the specific app(s) you want to target. The module skips processes where the Stripe SDK isn't loaded, so enabling it system-wide is safe but unnecessary.

---

## Logging

All log messages use the tag `StripeGPDisabler`:

```
adb logcat -s StripeGPDisabler
```

Example output:
```
D StripeGPDisabler: Hooked googlePayEnabled(Boolean)
I StripeGPDisabler: 1 hook(s) installed in com.example.app
I StripeGPDisabler: googlePayEnabled(true) intercepted → forcing false
```

---

## Release process

Releases are built automatically by GitHub Actions when a version tag is pushed:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow:
1. Builds the release APK with Gradle.
2. Renames it to `stripe-googlepay-disabler-vX.Y.Z.apk`.
3. Auto-generates release notes from commits since the previous tag.
4. Creates a GitHub Release and attaches the APK.

Tags containing a hyphen (e.g. `v1.0.0-beta`) are automatically marked as pre-releases.

---

## License

[MIT](LICENSE) © 2025 [cmldo](https://github.com/cmldo)
