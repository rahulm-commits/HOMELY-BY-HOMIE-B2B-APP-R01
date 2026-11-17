# SnackRoute Laptop Local Server

Run this local server on your laptop so the SnackRoute Android app can sync **shops, orders, and routes** directly to your laptop over Wi-Fi (or phone hotspot) in addition to the Global Cloud Server (Firebase Firestore).

## 1. Save & Start in `D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER` (Windows)

1. Copy `server.py` and `start_snackroute_server.bat` from this `laptop_server` folder into:
   `D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER`
2. Double-click **`start_snackroute_server.bat`** (or open Command Prompt in that folder and run `python server.py`).
3. If Windows Defender Firewall asks to allow Python on Private/Public networks, click **Allow access** so your phone can reach port `8787`.

All synced records are stored directly inside `D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER`:
- `snackroute_laptop.db` (SQLite database)
- `snackroute_data.json` (auto-updated readable JSON of all routes, shops & orders)
- `snackroute_shops.csv` (auto-updated Excel-ready CSV)

### Mac / Linux
```bash
cd laptop_server
python3 server.py
```
Optional custom folder:
```bash
python3 server.py --data-dir "/path/to/LOCAL SERVER"
```

## 2. Connect Your Phone App

1. Connect your **laptop and Android phone to the same Wi-Fi** (or turn ON your phone's hotspot and connect your laptop to it).
2. In the SnackRoute Android app, tap the top status pill on **Home** (or **Sync / Queue** on **Shops**).
3. Enter the URL printed in your laptop terminal, for example:
   `http://192.168.1.45:8787`
4. Tap **Save & Test** — once connected, Laptop Sync activates automatically.
5. Tap **Sync Now** to push all phone/cloud records to your laptop, or **Pull From Laptop** to download edits made on your laptop!

> **Note about Global Cloud Server:** Cloud sync uses Firebase Firestore under your Google account. Make sure you sign in with Google (instead of "Skip Login (Test Mode)") whenever you want records synced to the Global Cloud Server as well.
