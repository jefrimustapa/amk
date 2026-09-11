# AMK (Air Mouse & Keyboard) 🖱️⌨️📺

Modern, high-performance Android application written in **100% Native Kotlin with Jetpack Compose (Material 3)**. AMK transforms your Android phone into an authentic Bluetooth HID (Human Interface Device) controller for **Android TV**, **Google TV**, **PC**, and **Mac** — **zero companion server app required on the TV**.

---

## 🌟 Key Features

### 1. 🖱️ Dedicated Touchpad Page
- **Hardware 120Hz Touch Sampling**: Lag-free, responsive cursor gliding with speed acceleration.
- **Configurable Click Modes**:
  - `Tap Gestures Only`: 1-finger tap (Left Click), 2-finger tap (Right Click).
  - `Physical Buttons Only`: Pinned ergonomic Left & Right buttons at the bottom edge. Tap gestures on the trackpad are disabled to prevent accidental misclicks.
  - `Both Gestures & Buttons`: Full versatility with gestures and bottom click buttons simultaneously.
- **Smooth Scrolling**: Two-finger vertical sliding for scrolling carousels, feeds, and browsers.

### 2. ⌨️ Dedicated Keyboard Page
- **Live Text Forwarding**: Soft keyboard input text field streaming keystrokes and voice typing straight to the TV search bar.
- **TV Hotkey Controls**: `Enter/Search`, `Backspace`, `Esc/Back`, `Tab`, `Arrow keys`.
- **Send Clipboard Tool**: One-tap to beam copied text or streaming URLs directly to the TV.

### 3. 📺 Dedicated Remote Page
- **5-Way D-Pad**: High-contrast, tactile directional buttons (`Up`, `Down`, `Left`, `Right`) with centered `OK` button.
- **TV System Controls**: `Power`, `Home`, `Back`, `Menu`.
- **Media & Audio Controls**: `Volume Up`, `Volume Down`, `Mute`, `Play/Pause`, `Rewind`, `Fast-Forward`.

### 4. ⚙️ Dedicated Settings & Update Page
- **Zero-Loss Persistent Storage**: Settings automatically mirror to `/sdcard/Documents/AMK/amk_settings.json`. All user configurations survive app uninstallation and automatically restore on reinstall.
- **Pointer & Physics Controls**: Pointer sensitivity slider (0.5x - 3.0x), acceleration toggle, invert two-finger scroll.
- **Haptic Vibration**: Subtle tactile vibration feedback on click, tap, and keypress.
- **Bluetooth Device Manager**: 1-tap "Pair New TV" discoverable mode with remaining countdown timer.
- **In-App OTA Updates**: Direct GitHub Release checker, background APK download with real-time progress, and native `FileProvider` package installer prompt.

---

## 🚀 Versioning & CI/CD Pipeline

Adapted from `tmdb_stream` architecture:
- **Prebuild Versioning**: `node scripts/prebuild.js` computes semantic versions, monotonic int32 `versionCode`, and channels (`dev`, `ci`, `nightly`, `rc`, `stable`).
- **GitHub Actions**:
  - `ci-pr.yml`: Validates all pull requests before merge.
  - `nightly-build.yml`: Daily automated cron build publishing nightly pre-releases.
  - `release.yml`: Production release pipeline triggered on `v*` git tags.

---

## 🛠️ Building & Testing

```bash
# Generate version metadata
node scripts/prebuild.js

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```
