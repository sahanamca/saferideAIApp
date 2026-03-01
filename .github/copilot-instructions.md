# SafeRide AI - AI Agent Instructions

## 🎯 Project Overview
SafeRide AI is a **Flutter mobile app** implementing real-time driver safety monitoring using Google ML Kit facial recognition. It runs on Android/iOS devices (or web for demos) and processes camera frames to detect drowsiness, distraction, yawning, and motion sickness—triggering voice/haptic alerts via a priority-based system.

**Tech Stack:** Flutter (Dart), Google ML Kit, Provider (state management), Firebase/Firestore (analytics), Camera plugin, TensorFlow Lite concepts

---

## 🏗️ Architecture & Data Flow

### Core Services (lib/services/)
1. **CameraService** (`camera_service.dart`)
   - Manages front-facing camera stream at 15 FPS
   - **Frame skipping strategy:** Processes every 3rd frame (300ms intervals) to optimize CPU/battery
   - Emits `InputImage` stream consumed by detection service
   - Handles both mobile and web platforms with fallback support

2. **DetectionService** (`detection_service.dart`)
   - **Single source of truth** for all detection logic and alert state
   - Uses Google ML Kit FaceDetector with `enableClassification: true` (required for eye/mouth detection)
   - Processes facial landmarks to calculate: Eye Aspect Ratio (EAR), Mouth Aspect Ratio (MAR), head pose angles
   - Maintains **alert cooldown timers** per detection type (drowsiness: 5s, distraction: 4s, yawning: 8s, etc.)
   - Triggers voice alerts via FlutterTts and vibration via vibration plugin

3. **StreamingService** (`streaming_service.dart`) & **CloudStreamingService** (`cloud_streaming_service.dart`)
   - Remote monitoring via WebSocket and Firebase Firestore
   - Streams frame data to dashboards (web-based admin view)

### State Management Pattern
- **Provider ChangeNotifier:** CameraService and DetectionService expose state via `notifyListeners()`
- Screens use `Consumer<DetectionService>` to react to detection changes
- SharedPreferences persists user settings (toggle detection types, alert preferences)

### Detection Priority System (from README)
1. **Sleep/Drowsiness** (60s window, highest priority)
2. **Driver Distraction** (head pose >45°)
3. **Yawning/Fatigue** (Mouth Aspect Ratio > 0.5)
4. **Motion Sickness** (head movement std dev > 1.5)
5. **Erratic Movement** (sudden head movements)

---

## 🔧 Configuration & Thresholds
All tuning values live in `lib/utils/constants.dart`:
```dart
eyeClosureThreshold: 0.6        // Eye closure confidence (0-1)
headPoseThreshold: 15.0         // Head turn tolerance (degrees)
drowsinessFrameThreshold: 2     // Consecutive frames to trigger alert
sleepRatioThreshold: 0.3        // Proportion of closed-eye frames
drowsinessAlertCooldown: 5000   // Milliseconds between alerts
```
⚠️ **Testing note:** These are set to VERY sensitive (fast triggers) for device testing. Adjust thresholds in this ONE file only.

---

## 📱 Screen Architecture
- **StatefulWidget + Provider pattern:** All screens import and consume DetectionService/CameraService
- **File naming:** `{feature}_screen.dart` (e.g., `detection_screen.dart`, `admin_dashboard_screen.dart`)
- **DetectionScreenAuto** (primary production screen) vs **DetectionScreen** (older variant) — use Auto version
- **Routing:** Named routes defined in `main.dart` routes map
- **Theme:** Dark theme only (`ThemeMode.dark`), colors in `AppConstants` (primaryColor: #3B82F6)

---

## 🎯 Common Development Patterns

### Adding a New Detection Type
1. Add enum to `DetectionType` in `detection_model.dart`
2. Add threshold constant to `constants.dart` (e.g., `newFeatureThreshold`)
3. Add detection logic to `DetectionService.processImage()` method (~150-300 lines in)
4. Add alert cooldown timestamp field (e.g., `lastNewFeatureAlert`) to track timing
5. Increment stats in `DetectionStats.incrementDetection()`
6. Add toggle/settings in UI + SharedPreferences save/load

### Alert Flow
```
DetectionService detects pattern → Set currentAlert string → notifyListeners()
→ UI Consumer<DetectionService> rebuilds with alert visual
→ FlutterTts speaks (if enabled) → Vibration triggers → Alert displays for 5s
```

### Important: Alert Debouncing
- Each detection type has its own **cooldown timer** (e.g., `lastDrowsinessAlert`)
- Check `DateTime.now().difference(lastAlert).inMilliseconds > cooldown` before firing alert
- Prevents alert spam from same condition

---

## 🚀 Build & Run Commands
```bash
# Mobile (requires device or emulator)
flutter run -d V2207              # Run on device with ID V2207
flutter run --release             # Optimized build for performance testing

# Web (development)
flutter run -d chrome             # Run in Chrome for UI testing

# Build APK (Android)
flutter build apk --release

# Build iOS
flutter build ios --release
```

**Tasks available:** VS Code task "Run SafeRide AI on Mobile" handles device deployment

---

## 📊 Testing Alerts
Use `ALERT_TESTING_GUIDE.md` for triggering each detection type manually (close eyes, turn head, yawn, etc.). Detection thresholds are aggressive for device testing.

---

## 🔐 Critical Constraints
- ✅ **On-device only:** No server uploads of raw frames (privacy-first)
- ✅ **Portrait orientation:** Locked via `SystemChrome.setPreferredOrientations([portraitUp])` in main.dart
- ✅ **15 FPS capture, 5 FPS detection:** Frame skipping in CameraService for battery
- ✅ **Front-facing camera only:** Driver/passenger monitoring perspective
- ⚠️ **ML Kit dependency:** Google ML Kit free tier has call quotas — verify still available
- ⚠️ **Firebase optional:** Cloud streaming uses Firebase; works without it (local mode)

---

## 🐛 Debugging Tips
- **Check console:** `debugPrint()` extensively used with emoji prefixes (✅ ❌ 🎤 📸 etc.)
- **Detection not triggering?** Check alert cooldown timers and threshold values in `constants.dart`
- **Voice not working?** Verify TTS initialized: `_initializeTTS()` in DetectionService or check device volume
- **Camera freeze?** Restart app; camera initialization can hang on some devices
- **Frame rate issues?** Adjust `_frameSkipCount` in CameraService (currently: process every 3rd frame)

---

## 📂 Key Files by Task
| Task | Files |
|------|-------|
| Add detection | `detection_model.dart`, `constants.dart`, `detection_service.dart` (processImage) |
| Fix alert timing | `constants.dart` (cooldowns), `detection_service.dart` (DateTime checks) |
| UI updates | `lib/screens/{feature}_screen.dart`, `constants.dart` (colors/text) |
| Performance | `camera_service.dart` (_frameSkipCount), `constants.dart` (thresholds) |
| Settings/persistence | `detection_service.dart` (_loadSettings, saveSettings), SharedPreferences keys |

---

## ✅ Before Committing
1. Run `flutter analyze` for lint issues
2. Test on actual device (thresholds tuned for real hardware)
3. Verify alert cooldowns prevent spam
4. Check `debugPrint()` output for [E] errors
5. Ensure dark theme colors match AppConstants

