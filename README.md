# Pilot Companion

Pilot Companion is an Android planner for UPS flight schedules. It shows local and Zulu times, elapsed block time, seat position, hotels, countdowns, traded/revised state, pay-period grouping, and a shared schedule feed.

## Milestone 2 / Version 0.6

- **Blue = TRADED** for pilot-initiated trades made before the trip starts.
- **Yellow = REVISED** for company revisions.
- Once a trip has started, any later schedule change is classified as **REVISED**.
- Current A70327R schedule is loaded: ANC-SDF-CGN-SZX-ANC, with FO/FO/RO/RO positions.
- Tapping a flight opens a detailed flight card with local departure/arrival, Zulu departure/arrival, block time, position, status, pairing, hotel, and source.
- The app opens on the current date when scheduled, otherwise the nearest scheduled date, and highlights it.
- **Upload schedule** accepts Crew Access screenshots (on-device OCR), text, and JSON/text files. Imported schedules persist on that device without rebuilding the APK.
- **Sync now** downloads `shared_schedule.txt` from this repository. Every installed copy of the app reads the same public shared feed, so schedule changes published there appear on all devices without a new APK.
- Calendar navigation is grouped into 28-day pay-period windows. The current 2026 anchor is configurable in `PayPeriodCalculator`.

### Important sync distinction

Version 0.6 has two schedule paths:

1. **In-app import:** owner can import a screenshot/file and use it immediately on that device; the import is stored locally.
2. **Shared-device sync:** all devices pull the public `shared_schedule.txt` feed. Updating that feed updates every installed device after sync/startup without rebuilding the app.

A future backend/OAuth step will allow an owner-device import to publish directly to the shared feed. No GitHub token or UPS credential is embedded in the APK.

### Pay-period source rule

The UPS/IPA Agreement provides a 75:00 guarantee for a normal 28-day pay period and a 96:00 guarantee for a 35-day pay period. Version 0.6 therefore models 28-day periods but keeps the anchor configurable so an official payroll/bid calendar can replace the provisional 2026 anchor.

### Schedule source rules

- Flight Ops pairing detail is the original awarded schedule.
- Crew Access is the current operational schedule and overrides matching awarded legs.
- Trade confirmations identify pilot-initiated TRADED pairings before trip start.
- Once the trip has started, any detected schedule change is REVISED.
- Company changes before trip start are also REVISED.
- Crew Access times are interpreted as Zulu and converted to local time at each airport.
- Position labels follow: `DH`, `FO2`, and `IRO`/`RO`; all other labels display as `FO`.

## Install the latest debug APK

1. Open this repository on GitHub and select **Actions**.
2. Open the newest successful **Android debug APK** run.
3. In **Artifacts**, download **pilot-companion-debug**.
4. Unzip it and copy `app-debug.apk` to the Android phone.
5. Open the APK and install it over the existing Pilot Companion app.

## Build locally

Use Android SDK 36 and Java 17, then run `./gradlew testDebugUnitTest assembleDebug` (Windows: `./gradlew.bat testDebugUnitTest assembleDebug`).

## Next

- Owner-authenticated publishing so a schedule imported on one phone automatically writes to the shared feed
- More robust Crew Access OCR layouts and PDF parsing
- Pay/credit tracking by pay period
- Weather, hotel/airport maps, notifications, and logbook integration
