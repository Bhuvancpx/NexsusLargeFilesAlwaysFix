# Nexsus Android test app

This repository is a small, runnable Flutter app for exercising the Nexsus
Android method channel and contributing improvements to it. The app is a
test harness, not a production Nexsus client.

## What is included

- Device, battery, memory, storage, and next-alarm information
- MediaStore scanning for image, video, and audio files larger than 10 MiB
  (limited to the 100 largest results), plus opening a selected result
- Scheduling and cancelling Android alarm notifications
- Android settings entry points for storage, exact alarms, overlays, display,
  and notification access
- A foreground-service sample that can be started and stopped from the UI
- Dart method-channel tests using Flutter's mock platform messenger

The method channel is `com.nexsusai.app/system`. Its Android implementation is
in `android/app/src/main/kotlin/com/nexsusai/app/MainActivity.kt`.

## Requirements

- Flutter (stable) with Dart, and the Android platform tools
- Android Studio (or another JDK 17 installation) with Android SDK Platform
  Tools and the Android SDK Command-line Tools
- An Android emulator or a physical Android device with USB debugging enabled

Install Flutter dependencies and run the test suite:

```sh
flutter pub get
flutter analyze
flutter test
```

For Android builds, install the Android SDK components requested by
`flutter doctor` and accept the SDK licenses with
`flutter doctor --android-licenses`.

To run on an attached device or a started emulator:

```sh
flutter devices
flutter run
```

To build a debug APK without installing it:

```sh
flutter build apk --debug
```

Android Studio can also open the `android` directory after Flutter has generated
`android/local.properties` (normally created by `flutter run`).

## Permissions and limitations

- The media scan asks Android for media-library permissions when needed. It
  uses MediaStore and does not request broad file-system access. Android 13+
  may offer separate image, video, and audio grants.
- Setting an alarm may require notification permission. Exact-alarm settings
  are exposed because access is controlled by Android and device policy.
- The always-on-display button starts a foreground-service sample and its
  ongoing notification. It is **not** an always-on display: it does not draw
  on top of the lock screen, keep the screen on, or work around device-specific
  screen-off display support. The existing overlay-permission check is kept in
  the native bridge.
- This sample does not include a notification-listener service, so notification
  access is normally disabled even if its settings page is opened.
- Opening media depends on another Android app that can handle its content
  type. Emulator media libraries may be empty; add test media or use a device.
- Device-specific settings pages and permission flows vary by Android version
  and manufacturer. Unsupported options should be tested on a physical device
  before relying on them.

## Contributing

Open an issue or pull request with the Android version, device/emulator model,
Flutter version, and steps to reproduce. Run `flutter test` before submitting.
Keep permissions explicit and do not broaden file access beyond the MediaStore
collections used by this integration.

## License

Distributed under the MIT License; see [LICENSE](LICENSE).
