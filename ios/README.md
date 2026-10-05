# Fire Extinguisher Survey – iPhone app

A native SwiftUI version of the Android app, with the same features:

- Scan an extinguisher's QR code with the camera, with a flashlight toggle and manual code entry
- See the name, code, location, type and capacity
- See the **last check date**, who did it and the **condition found** (OK or the list of defects), plus full history
- Record a survey: **OK** or **NOT OK**, and for Not OK pick every condition that applies
  (corroded, damaged, needs casing/cabinet, low pressure, pin/seal, hose/nozzle, label, obstructed,
  needs refill, service due, mounting, missing, other)
- Dashboard with Not OK / Due counts, filters and search
- Register extinguishers (an unknown QR code offers to register it), and generate and share/print QR labels
- Export all surveys to CSV
- Fully offline: data is stored on the device with SwiftData

Requires **iOS 17** or newer.

## Install on an iPhone

Apple only lets signed apps run on an iPhone, so you need a Mac with Xcode:

1. Install **Xcode 16** or newer from the Mac App Store.
2. Open `ios/FireSurvey.xcodeproj`.
3. Select the **FireSurvey** target → **Signing & Capabilities** → choose your **Team**
   (a free Apple ID works for testing; a paid Apple Developer account is needed for
   TestFlight/App Store or devices you don't own). If the bundle ID
   `com.powerplant.firesurvey` is taken, change it to something unique.
4. Plug in the iPhone, select it as the run destination and press **Run** (⌘R).
5. On the iPhone, trust the developer under **Settings → General → VPN & Device Management**.

To roll the app out to several operators, use **TestFlight** or **Apple Business Manager**
(through Product → Archive in Xcode). Both need a paid Apple Developer account.

Each push also builds the app in GitHub Actions (**Actions → iOS build**). That build produces an
**unsigned** `.ipa` artifact. It must be re-signed (for example with Xcode or a sideloading tool)
before it will install on a device.

## Project layout

```
ios/
├── FireSurvey.xcodeproj
└── FireSurvey/
    ├── FireSurveyApp.swift      app entry, navigation routes, colours
    ├── Models/Models.swift      Extinguisher, Inspection, Issue list
    ├── Views/                   Home, Scanner, Detail, Survey, Edit screens
    ├── Utilities/               QR generation, CSV export, date formats
    └── Assets.xcassets          app icon
```

The project uses Xcode 16's synchronized folders, so new `.swift` files added under `FireSurvey/`
are picked up automatically. To change the defect list, edit the `Issue` enum in `Models/Models.swift`.
