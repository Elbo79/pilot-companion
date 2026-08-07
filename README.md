# Pilot Companion

Pilot Companion is an Android month-view planner for flight schedules. It shows local departure/arrival times, true elapsed block time, seat position, hotels, countdowns, and month navigation.

Version 0.2 loaded the August-September 2026 schedule from supplied Flight Ops and Crew Access screenshots. Version 0.3 restored the operational leg card.

## Milestone 2 / Version 0.4

- Schedule changes are explicit data states instead of being inferred only from time differences.
- **Blue = TRADED**: pilot-initiated trades.
- **Yellow = REVISED**: company-initiated revisions.
- The approved Aug 7 trade replaces A70746 with **A70327R** and loads the current Crew Access legs ANC-SDF-CGN-SZX-ANC.
- A70327R is marked TRADED throughout the month and detail views.
- Pairing number is displayed with traded legs.
- The data model is prepared for a shared schedule source so a spouse/family installation can read the same roster.

### Shared schedule architecture

The next sync step is a single cloud schedule document/account shared by the pilot and invited family viewers. The pilot installation will be the owner/editor; family installations will be read-only. Do not store UPS credentials in the app or share them with family devices. Until a cloud provider/project is configured, version 0.4 continues to use the bundled schedule as its offline source.

### Schedule source rules

- Flight Ops pairing detail is the original awarded schedule.
- Crew Access is the current operational schedule and overrides matching awarded legs.
- Trade confirmations identify pilot-initiated TRADED pairings.
- Company changes are REVISED, independently of trades.
- Source timestamps are converted to local time at each endpoint using the airport time zone.
- Position labels follow: `DH`, `FO2`, and `IRO`/`RO`; all other labels display as `FO`.

## Install the latest debug APK

1. Open this repository on GitHub and select **Actions**.
2. Open the newest successful **Android debug APK** run.
3. In **Artifacts**, download **pilot-companion-debug**.
4. Unzip it and copy `app-debug.apk` to the Android phone.
5. Open the APK. If Android asks, allow **Install unknown apps**, then choose **Install**.

## Build locally

Use Android SDK 36 and Java 17, then run `./gradlew testDebugUnitTest assembleDebug` (Windows: `./gradlew.bat testDebugUnitTest assembleDebug`).

## Roadmap

- Connect shared cloud schedule and family read-only access
- Import Crew Access screenshots and parse trips automatically
- Add pay/pay-period tracking
- Add weather, hotel/airport maps, notifications, and logbook integration
