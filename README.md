# Pilot Companion

Pilot Companion is an Android month-view planner for flight schedules. The first version shows each leg in the calendar and a selected-day detail panel with:

- departure and arrival in each airport's local time;
- elapsed flight time calculated from instants, so time-zone changes are handled correctly;
- seat positions using the planner rules: `IRO` → `RO`, `FO2` → `FO2`, deadhead → `DH`, and every other assignment → `FO`;
- previous/next month navigation.

The current schedule is demonstration data in `ScheduleRepository`. A future Crew Access importer can replace that data source without changing the calendar UI or flight calculations.

## Install the latest debug APK

1. Open this repository on GitHub and select **Actions**.
2. Open the newest successful **Android debug APK** run.
3. In **Artifacts**, download **pilot-companion-debug**.
4. Unzip the download and copy `app-debug.apk` to the Android phone.
5. Open the APK on the phone. If Android asks, allow the browser or file manager to **Install unknown apps**, then choose **Install**.

Debug APKs are for testing and are retained by GitHub Actions for 30 days. Android may warn that the app is from an unknown developer because it is not Play Store signed.

## Build locally

Install Android Studio with Android SDK 36 and a Java 17 runtime, then open the repository and let Gradle sync. To build from a terminal:

```bash
./gradlew testDebugUnitTest assembleDebug
```

On Windows PowerShell, use `./gradlew.bat testDebugUnitTest assembleDebug`. The APK will be written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

- `app/src/main/java/com/pilotcompanion/app` — month planner, sample schedule, and flight calculations
- `app/src/test` — seat-position and elapsed-time tests
- `.github/workflows/android.yml` — debug APK build and artifact upload on every push

## Roadmap

- Import Crew Access screenshots with OCR
- Parse trips automatically
- Add pay and pay-period tracking
- Add weather, hotel and airport maps, notifications, and logbook integration
