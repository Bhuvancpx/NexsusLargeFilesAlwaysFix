# Nexsus Android Utilities

Public Android/Kotlin native integration code for the Nexsus app. This
repository currently contains `MainActivity.kt`, which connects a Flutter
application to Android system features through a platform method channel.

## Included features

- Device, battery, memory, storage, and next-alarm information
- MediaStore scanning for image, video, and audio files larger than 10 MiB,
  returning up to 100 of the largest results
- Opening scanned media with the user's selected Android app
- Scheduling and cancelling Android alarm notifications
- Android settings and permission entry points for notifications and overlays
- Hooks to start and stop the Nexsus always-on-display service

## Repository scope

This is a source-code contribution repository, not a complete, buildable Flutter
application. The included activity expects to be integrated into a compatible
Flutter Android project. The project-specific `NexsusAodService`, Dart method
channel callers, Android manifest declarations, Gradle files, and app resources
are not included here.

The method channel name is `com.nexsusai.app/system`. Android permissions and
platform behavior depend on the Android versions targeted by the host app.

## Contributing

Issues and pull requests are welcome. Include the Android version and relevant
Flutter/Android project context when reporting integration problems. Keep
permission handling explicit and avoid broadening file access beyond the
MediaStore collections used by this integration.

## License

Distributed under the MIT License; see [LICENSE](LICENSE).
