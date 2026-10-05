# Fire Extinguisher Survey – web app

A browser version of the Android and iPhone apps. It works on any phone, tablet or PC with a modern
browser (Chrome, Edge, Safari, Firefox) and can be **installed to the home screen** like a normal app.
It keeps working **offline** once it has been opened once.

Features are the same as the mobile apps:

- Scan an extinguisher's QR code with the camera, with a flashlight button (on supported Android phones) and manual code entry
- See its name, code, location, type, capacity, **last check date**, who checked it and the **condition found**, plus full history
- Record a survey: **OK**, or **NOT OK** with every condition that applies
  (corroded, damaged, needs casing/cabinet, low pressure, pin/seal, hose/nozzle, label, obstructed,
  needs refill, service due, mounting, missing, other)
- Dashboard with Not OK / Due counts, filters and search
- Register extinguishers, and download or print QR labels
- Export surveys to CSV (Excel)
- **Backup / restore** all data as a `.json` file (⋮ menu)

It uses plain HTML, CSS and JavaScript with no build step. QR decoding uses the browser's built-in
`BarcodeDetector` when it is available, and the bundled [jsQR](https://github.com/cozmo/jsQR)
otherwise. Labels are generated with the bundled [qrcode-generator](https://github.com/kazuhikoarase/qrcode-generator).

## Data storage

Data is saved **in the browser on that device** (localStorage). It is not sent anywhere.
Clearing the browser's site data deletes it, so download a backup from the ⋮ menu regularly.
To move data to another device, download a backup on one and restore it on the other.

## Hosting

The camera only works over **HTTPS** (or `http://localhost`). Pick one of these:

### GitHub Pages (free)

1. Merge this branch into `main`.
2. In the repository go to **Settings → Pages → Build and deployment → Source** and choose **GitHub Actions**.
3. The **Web app (GitHub Pages)** workflow publishes `web/` to
   `https://<your-github-user>.github.io/<repo-name>/`.

The page is public, but each person's survey data stays only in their own browser.

### Any web server or intranet

Copy the contents of `web/` to any static web server with HTTPS (IIS, nginx, Apache, an
internal plant server, Netlify and so on). No server-side code or database is needed.

### Try it locally

```bash
cd web
python3 -m http.server 8000
# open http://localhost:8000
```

## Install on a phone

- **Android (Chrome)**: open the site → ⋮ menu → **Add to Home screen / Install app**
- **iPhone (Safari)**: open the site → Share → **Add to Home Screen**
