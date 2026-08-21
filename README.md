# Pilot Companion

Pilot Companion is an Android planner for UPS flight schedules. It shows local and Zulu times, elapsed block time, seat position, hotels, countdowns, traded/revised state, pay-period grouping, a shared schedule feed, and ANC First Officer administrative deadlines.

## Milestone 3 / Version 0.7

Version 0.7 adds an ANC 747 First Officer important-date layer directly to the month calendar.

- **Blue = TRADED** for pilot-initiated trades made before the trip starts.
- **Yellow = REVISED** for company revisions.
- Once a trip has started, any later schedule change is classified as **REVISED**.
- Current A70327R schedule remains loaded: ANC-SDF-CGN-SZX-ANC, with FO/FO/RO/RO positions.
- Tapping a flight opens local and Zulu detail information.
- **Upload schedule** continues to accept Crew Access screenshots/files without an APK rebuild.
- **Sync now** continues to read the shared schedule feed for family devices.

### ANC FO important dates

The month view now displays dated events from the 2606 ANC 74Y First Officer bid transition timeline and the UPS 2026 Dates to Remember sheet, including:

- Primary line-bid closing and award milestones
- VTO/VTOR/RMUL/RSIM/LITT deadlines that apply to ANC FO
- Primary, Secondary and Tertiary vacation bid posting, due, and award dates
- BV banking deadline
- Pay-period start/end boundaries
- Published UPS paydays
- BP2607 publication date

Events with a contractual/administrative due date also get a **one-day-before REMINDER** entry in the calendar. Android notifications are scheduled one day before those deadlines; if the source provides no exact time, the reminder defaults to 09:00 ANC time. Notification permission is requested on Android 13+.

### Pay-period boundaries currently loaded

- PP09 ends Sep 6, 2026 at 02:59 ANC LDT
- PP10 begins Sep 6 at 03:00 and ends Oct 4 at 02:59
- PP11 begins Oct 4 at 03:00 and ends Nov 1 at 02:59
- PP12 begins Nov 1 at 03:00 and ends Nov 29 at 02:59
- PP13 begins Nov 29 at 03:00

### Important source rule

Exact event times are only shown when the UPS source supplies a time. Vacation/BV dates from the annual Dates to Remember sheet that do not include a clock time remain date-only rather than inventing a time.

### Important sync distinction

1. **In-app import:** owner can import a screenshot/file and use it immediately on that device; the import is stored locally.
2. **Shared-device sync:** all devices pull the public `shared_schedule.txt` feed. Updating that feed updates every installed device after sync/startup without rebuilding the app.

A future backend/OAuth step will allow an owner-device import to publish directly to the shared feed. No GitHub token or UPS credential is embedded in the APK.

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
- Weather, hotel/airport maps, and logbook integration
