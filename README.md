# Fire Extinguisher Survey (Android)

Android app for power plant operators to survey fire extinguishers by scanning the QR code on each one.

## What it does

1. **Scan the QR code** on an extinguisher (or type the code if the label is damaged). The flashlight toggle helps in dark plant areas.
2. **See the extinguisher's details**: name, code, location, type, capacity, plus
   - the **date of the last check** and who did it
   - the **condition found at the last check** (OK, or the list of defects)
   - a warning when the monthly check is due (over 30 days since the last one)
   - the full inspection history
3. **Record a new survey**:
   - **OK**, or
   - **NOT OK**, then choose every condition that applies:
     corroded/rusted, physically damaged/dented, needs casing/cabinet, low pressure,
     safety pin or tamper seal missing/broken, hose or nozzle damaged, label unreadable,
     access obstructed, needs refill/recharge, service/hydrostatic test overdue,
     bracket/mounting damaged, missing from location, other (notes required)
   - optional notes and the operator name (remembered for next time)

Other features:

- **Home dashboard** listing every extinguisher with its last result, with counts for *Not OK* and *Due* and filters for each, plus search.
- **Register extinguishers**: scanning an unknown QR offers to register it. You can also add one with **+**.
- **QR label generation**: open an extinguisher and tap the QR icon to share or print a label (QR + name + code + location).
- **CSV export** of all surveys (download icon on the home screen), for Excel or sending by email.
- Works **fully offline**. Data is stored on the device (Room/SQLite) and QR decoding runs on-device with ML Kit.

## QR code format

The QR code simply contains the extinguisher's unique ID text, e.g. `FE-U2-TH-003`. You can use
existing QR labels (whatever text they contain becomes the ID) or print new ones from the app.

## Build

Requirements: Android Studio (Ladybug or newer) or JDK 17 + Android SDK 35.

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Each push also builds the APK in GitHub Actions (**Actions → Android build → Artifacts**).

Min Android version: 8.0 (API 26).

## Tech

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · Room · CameraX + ML Kit barcode scanning · ZXing (QR generation)

## Project layout

```
app/src/main/java/com/powerplant/firesurvey/
├── MainActivity.kt          navigation graph
├── SurveyViewModel.kt
├── data/                    Room entities, DAO, database, issue list
├── ui/screens/              Home, Scan, Detail, Survey, Edit screens
├── ui/theme/
└── util/                    QR generation, CSV/label sharing, date formats, prefs
```

To change the list of defect conditions, edit the `Issue` enum in `data/Models.kt`.
