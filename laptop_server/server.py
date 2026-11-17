#!/usr/bin/env python3
"""
SnackRoute Laptop Local Server (Zero-Dependency Python 3 + SQLite)

Windows target folder:
    D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER

Run on Windows:
    Double-click start_snackroute_server.bat
    or run: python server.py

Features:
- Stores SQLite database + live JSON & CSV snapshots inside DATA_DIR
  (defaults on Windows to D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER when D:\ is present)
- REST API compatible with SnackRoute Android App:
    GET    /api/health
    GET    /api/shops
    POST   /api/shops
    DELETE /api/shops/<id>
    GET    /api/orders
    POST   /api/orders
    GET    /api/routes
    POST   /api/routes
    GET    /api/export.csv
    GET    /api/export.json
- Built-in Web Dashboard at http://localhost:8787/
"""

import argparse
import csv
import io
import json
import os
import socket
import sqlite3
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

DEFAULT_WINDOWS_DIR = r"D:\IDEAS AND PLANS\1 BANANA CHIPS\LOCAL SERVER"
PORT = int(os.environ.get("SNACKROUTE_PORT", "8787"))
API_TOKEN = os.environ.get("SNACKROUTE_TOKEN", "").strip()


def resolve_data_dir(cli_dir: str = "") -> str:
    if cli_dir.strip():
        target = os.path.abspath(cli_dir.strip())
    elif os.environ.get("SNACKROUTE_DATA_DIR", "").strip():
        target = os.path.abspath(os.environ["SNACKROUTE_DATA_DIR"].strip())
    elif os.name == "nt" and os.path.exists("D:\\"):
        target = DEFAULT_WINDOWS_DIR
    else:
        target = os.path.dirname(os.path.abspath(__file__))
    os.makedirs(target, exist_ok=True)
    return target


DATA_DIR = resolve_data_dir()
DB_PATH = os.path.join(DATA_DIR, "snackroute_laptop.db")
JSON_SNAPSHOT_PATH = os.path.join(DATA_DIR, "snackroute_data.json")
CSV_SNAPSHOT_PATH = os.path.join(DATA_DIR, "snackroute_shops.csv")


def dict_factory(cursor, row):
    return {col[0]: row[idx] for idx, col in enumerate(cursor.description)}


def init_db():
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS shops (
                id TEXT PRIMARY KEY,
                userId TEXT DEFAULT '',
                shopCode TEXT NOT NULL,
                name TEXT NOT NULL,
                ownerName TEXT NOT NULL,
                phone TEXT NOT NULL,
                secondaryPhone TEXT DEFAULT '',
                address TEXT NOT NULL,
                pinCode TEXT DEFAULT '',
                routeId TEXT DEFAULT '',
                photoUri TEXT DEFAULT '',
                latitude REAL DEFAULT 0.0,
                longitude REAL DEFAULT 0.0,
                note TEXT DEFAULT '',
                status TEXT DEFAULT 'active',
                inquiryStatus TEXT DEFAULT 'pending_inquiry',
                enquiryDate TEXT DEFAULT '',
                callDate TEXT DEFAULT '',
                lastOrderDate TEXT DEFAULT '',
                lastCallDate TEXT DEFAULT '',
                confirmedPriceNotes TEXT DEFAULT '',
                mapsLink TEXT DEFAULT '',
                addedBy TEXT DEFAULT '',
                updatedAtMillis INTEGER DEFAULT 0
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id TEXT PRIMARY KEY,
                userId TEXT DEFAULT '',
                orderCode TEXT NOT NULL,
                shopId TEXT DEFAULT '',
                shopCode TEXT DEFAULT '',
                shopName TEXT DEFAULT '',
                routeId TEXT DEFAULT '',
                status TEXT DEFAULT 'confirmed',
                totalAmount REAL DEFAULT 0.0,
                itemCount INTEGER DEFAULT 0,
                itemsSummary TEXT DEFAULT '',
                orderDate TEXT DEFAULT '',
                note TEXT DEFAULT '',
                updatedAtMillis INTEGER DEFAULT 0
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS routes (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                vehicle TEXT DEFAULT '',
                maxCrates INTEGER DEFAULT 35,
                driverName TEXT DEFAULT '',
                driverPhone TEXT DEFAULT '',
                dispatchWindow TEXT DEFAULT '',
                stagingBay TEXT DEFAULT '',
                colorIndex INTEGER DEFAULT 0,
                isActive INTEGER DEFAULT 1,
                updatedAtMillis INTEGER DEFAULT 0
            )
            """
        )
        conn.commit()
    write_snapshots()


def write_snapshots():
    """Keep human-readable JSON & CSV backups synced inside DATA_DIR on every update."""
    try:
        with sqlite3.connect(DB_PATH) as conn:
            conn.row_factory = dict_factory
            shops = conn.execute("SELECT * FROM shops ORDER BY updatedAtMillis DESC").fetchall()
            orders = conn.execute("SELECT * FROM orders ORDER BY updatedAtMillis DESC").fetchall()
            routes = conn.execute("SELECT * FROM routes ORDER BY colorIndex ASC, id ASC").fetchall()

        with open(JSON_SNAPSHOT_PATH, "w", encoding="utf-8") as f:
            json.dump(
                {
                    "updatedAtMillis": int(time.time() * 1000),
                    "dataDir": DATA_DIR,
                    "routes": routes,
                    "shops": shops,
                    "orders": orders,
                },
                f,
                indent=2,
                ensure_ascii=False,
            )

        cols = [
            "id", "shopCode", "name", "ownerName", "phone", "secondaryPhone",
            "address", "pinCode", "routeId", "latitude", "longitude",
            "status", "inquiryStatus", "confirmedPriceNotes", "note", "mapsLink", "addedBy",
        ]
        with open(CSV_SNAPSHOT_PATH, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(cols)
            for s in shops:
                writer.writerow([s.get(c, "") for c in cols])
    except Exception as exc:
        print(f"[Warn] Could not write snapshot files: {exc}")


def get_lan_ips():
    ips = set()
    try:
        hostname = socket.gethostname()
        for info in socket.getaddrinfo(hostname, None):
            ip = info[4][0]
            if ":" not in ip and not ip.startswith("127."):
                ips.add(ip)
    except Exception:
        pass
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ips.add(s.getsockname()[0])
        s.close()
    except Exception:
        pass
    return sorted(ips)


DASHBOARD_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>SnackRoute Laptop Local Server</title>
  <style>
    body { font-family: system-ui, -apple-system, sans-serif; background: #FAF7F2; color: #1A1F1C; margin: 0; padding: 20px; }
    .wrap { max-width: 1120px; margin: 0 auto; }
    .header { background: #1F4E43; color: #fff; padding: 18px 22px; border-radius: 16px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
    .header h1 { margin: 0; font-size: 20px; }
    .header p { margin: 4px 0 0; opacity: 0.9; font-size: 13px; }
    .btn { background: #D95300; color: #fff; border: none; padding: 9px 14px; border-radius: 10px; font-weight: 600; cursor: pointer; text-decoration: none; font-size: 13px; }
    .btn.secondary { background: #fff; color: #1F4E43; }
    .grid { display: grid; grid-template-columns: 1fr 2fr; gap: 16px; margin-top: 16px; }
    @media (max-width: 840px) { .grid { grid-template-columns: 1fr; } }
    .card { background: #fff; border-radius: 14px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
    label { display: block; font-size: 12px; font-weight: 600; margin-top: 10px; color: #5D6560; }
    input, select, textarea { width: 100%; box-sizing: border-box; padding: 8px 10px; border: 1px solid #DCD3C6; border-radius: 8px; margin-top: 4px; font-size: 13px; }
    table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 13px; }
    th, td { text-align: left; padding: 8px; border-bottom: 1px solid #F0EAE0; }
    th { color: #5D6560; font-size: 12px; }
    .pill { display: inline-block; padding: 2px 8px; border-radius: 999px; background: #CDEEE5; color: #1F4E43; font-size: 11px; font-weight: 700; }
    .meta-bar { background: #EFECE6; padding: 10px 14px; border-radius: 10px; margin-top: 12px; font-size: 12px; color: #3D4540; }
  </style>
</head>
<body>
<div class="wrap">
  <div class="header">
    <div>
      <h1>SnackRoute Laptop Local Server</h1>
      <p>Live SQLite + JSON + CSV Storage • Bidirectional Sync with Phone & Global Cloud</p>
    </div>
    <div style="display:flex;gap:8px;">
      <a class="btn secondary" href="/api/export.csv">Download CSV</a>
      <a class="btn secondary" href="/api/export.json">Download JSON</a>
      <button class="btn" onclick="loadAll()">Refresh</button>
    </div>
  </div>

  <div class="meta-bar" id="serverMeta">Loading server folder & LAN address...</div>

  <div class="grid">
    <div class="card">
      <h3 style="margin-top:0;">Add / Update Shop on Laptop</h3>
      <form id="shopForm" onsubmit="saveShop(event)">
        <label>Shop Code</label>
        <input id="shopCode" placeholder="SHOP-000201" required />
        <label>Shop Name</label>
        <input id="name" placeholder="Kaveri Bakery" required />
        <label>Owner Name</label>
        <input id="ownerName" placeholder="Rajesh" required />
        <label>Phone</label>
        <input id="phone" placeholder="9845012345" required />
        <label>Address</label>
        <textarea id="address" rows="2" placeholder="Main Road, Kannur" required></textarea>
        <label>PIN Code</label>
        <input id="pinCode" placeholder="670001" required />
        <label>Route</label>
        <select id="routeId">
          <option value="R01-KANNUR-BANGALORE">R01-KANNUR-BANGALORE</option>
          <option value="R02-KANNUR-KOZHIKODE">R02-KANNUR-KOZHIKODE</option>
          <option value="R03-BANGALORE-PUNE">R03-BANGALORE-PUNE</option>
          <option value="UNALLOCATED">UNALLOCATED</option>
        </select>
        <label>Notes</label>
        <input id="note" placeholder="Optional delivery note" />
        <button class="btn" style="width:100%;margin-top:14px;" type="submit">Save to Laptop Server</button>
      </form>
    </div>

    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3 style="margin:0;">Saved Shops (<span id="shopCount">0</span>)</h3>
        <input id="searchBox" placeholder="Filter shops..." oninput="renderShops()" style="width:210px;margin:0;" />
      </div>
      <table>
        <thead>
          <tr><th>Code</th><th>Shop & Owner</th><th>Phone</th><th>Route</th><th>Status</th></tr>
        </thead>
        <tbody id="shopsTable"></tbody>
      </table>

      <h3 style="margin-top:22px;">Saved Orders (<span id="orderCount">0</span>)</h3>
      <table>
        <thead>
          <tr><th>Order</th><th>Shop</th><th>Items</th><th>Amount</th><th>Date</th></tr>
        </thead>
        <tbody id="ordersTable"></tbody>
      </table>
    </div>
  </div>
</div>

<script>
let allShops = [];
async function loadAll() {
  const [hRes, sRes, oRes, rRes] = await Promise.all([
    fetch('/api/health'),
    fetch('/api/shops'),
    fetch('/api/orders'),
    fetch('/api/routes')
  ]);
  const health = await hRes.json();
  allShops = await sRes.json();
  const orders = await oRes.json();
  const routes = await rRes.json();

  document.getElementById('serverMeta').innerHTML =
    `<b>Storage Folder:</b> ${health.dataDir || ''} &nbsp;|&nbsp; ` +
    `<b>Phone App URL(s):</b> ${(health.urls || []).join(' , ')}`;

  if (routes.length > 0) {
    const sel = document.getElementById('routeId');
    const existing = new Set([...sel.options].map(o => o.value));
    routes.forEach(r => {
      if (r.id && !existing.has(r.id)) {
        const opt = document.createElement('option');
        opt.value = r.id;
        opt.textContent = r.id;
        sel.appendChild(opt);
      }
    });
  }

  document.getElementById('shopCount').textContent = allShops.length;
  document.getElementById('orderCount').textContent = orders.length;
  renderShops();
  document.getElementById('ordersTable').innerHTML = orders.map(o =>
    `<tr><td><span class="pill">${o.orderCode}</span></td><td>${o.shopName}</td><td>${o.itemsSummary}</td><td>₹${o.totalAmount}</td><td>${o.orderDate}</td></tr>`
  ).join('') || '<tr><td colspan="5">No orders synced yet.</td></tr>';
}
function renderShops() {
  const q = document.getElementById('searchBox').value.trim().toLowerCase();
  const list = allShops.filter(s => !q || (s.name + s.shopCode + s.ownerName + s.phone + s.routeId).toLowerCase().includes(q));
  document.getElementById('shopsTable').innerHTML = list.map(s =>
    `<tr><td><span class="pill">${s.shopCode}</span></td><td><b>${s.name}</b><br><small>${s.ownerName} • ${s.address} (${s.pinCode})</small></td><td>${s.phone}</td><td>${s.routeId}</td><td>${s.status} / ${s.inquiryStatus}</td></tr>`
  ).join('') || '<tr><td colspan="5">No shops stored on laptop server yet.</td></tr>';
}
async function saveShop(e) {
  e.preventDefault();
  const payload = {
    id: 'laptop_' + Date.now(),
    shopCode: document.getElementById('shopCode').value,
    name: document.getElementById('name').value,
    ownerName: document.getElementById('ownerName').value,
    phone: document.getElementById('phone').value,
    address: document.getElementById('address').value,
    pinCode: document.getElementById('pinCode').value,
    routeId: document.getElementById('routeId').value,
    note: document.getElementById('note').value,
    status: 'active',
    inquiryStatus: 'pending_inquiry',
    updatedAtMillis: Date.now()
  };
  await fetch('/api/shops', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(payload)
  });
  e.target.reset();
  loadAll();
}
loadAll();
</script>
</body>
</html>
"""


class SnackRouteHandler(BaseHTTPRequestHandler):
    def _check_auth(self):
        if not API_TOKEN:
            return True
        provided = self.headers.get("X-SnackRoute-Token", "").strip()
        if provided != API_TOKEN:
            self._send_json(401, {"ok": False, "error": "Invalid X-SnackRoute-Token"})
            return False
        return True

    def _send_json(self, code, data):
        body = json.dumps(data).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _read_json(self):
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        return json.loads(raw.decode("utf-8"))

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, X-SnackRoute-Token")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/":
            html = DASHBOARD_HTML.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self.end_headers()
            self.wfile.write(html)
            return

        if not self._check_auth():
            return

        with sqlite3.connect(DB_PATH) as conn:
            conn.row_factory = dict_factory
            if path == "/api/health":
                shop_count = conn.execute("SELECT COUNT(*) AS c FROM shops").fetchone()["c"]
                order_count = conn.execute("SELECT COUNT(*) AS c FROM orders").fetchone()["c"]
                route_count = conn.execute("SELECT COUNT(*) AS c FROM routes").fetchone()["c"]
                urls = [f"http://{ip}:{PORT}" for ip in get_lan_ips()]
                self._send_json(200, {
                    "ok": True,
                    "server": "SnackRoute Laptop Server",
                    "dataDir": DATA_DIR,
                    "urls": urls,
                    "shopCount": shop_count,
                    "orderCount": order_count,
                    "routeCount": route_count,
                    "timestamp": int(time.time() * 1000),
                })
            elif path == "/api/shops":
                rows = conn.execute("SELECT * FROM shops ORDER BY updatedAtMillis DESC").fetchall()
                self._send_json(200, rows)
            elif path == "/api/orders":
                rows = conn.execute("SELECT * FROM orders ORDER BY updatedAtMillis DESC").fetchall()
                self._send_json(200, rows)
            elif path == "/api/routes":
                rows = conn.execute("SELECT * FROM routes ORDER BY colorIndex ASC, id ASC").fetchall()
                self._send_json(200, rows)
            elif path == "/api/export.json":
                shops = conn.execute("SELECT * FROM shops ORDER BY updatedAtMillis DESC").fetchall()
                orders = conn.execute("SELECT * FROM orders ORDER BY updatedAtMillis DESC").fetchall()
                routes = conn.execute("SELECT * FROM routes ORDER BY colorIndex ASC, id ASC").fetchall()
                self._send_json(200, {"dataDir": DATA_DIR, "routes": routes, "shops": shops, "orders": orders})
            elif path == "/api/export.csv":
                shops = conn.execute("SELECT * FROM shops ORDER BY updatedAtMillis DESC").fetchall()
                out = io.StringIO()
                writer = csv.writer(out)
                cols = ["id", "shopCode", "name", "ownerName", "phone", "secondaryPhone", "address", "pinCode", "routeId", "latitude", "longitude", "status", "inquiryStatus", "confirmedPriceNotes", "note", "mapsLink", "addedBy"]
                writer.writerow(cols)
                for s in shops:
                    writer.writerow([s.get(c, "") for c in cols])
                data = out.getvalue().encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "text/csv; charset=utf-8")
                self.send_header("Content-Disposition", "attachment; filename=snackroute_shops.csv")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            else:
                self._send_json(404, {"ok": False, "error": "Not found"})

    def do_POST(self):
        if not self._check_auth():
            return
        parsed = urlparse(self.path)
        path = parsed.path
        try:
            data = self._read_json()
        except Exception as exc:
            self._send_json(400, {"ok": False, "error": str(exc)})
            return

        with sqlite3.connect(DB_PATH) as conn:
            if path == "/api/shops":
                shop_id = str(data.get("id") or data.get("shopCode") or f"shop_{int(time.time() * 1000)}")
                conn.execute(
                    """
                    INSERT INTO shops (
                        id, userId, shopCode, name, ownerName, phone, secondaryPhone,
                        address, pinCode, routeId, photoUri, latitude, longitude, note,
                        status, inquiryStatus, enquiryDate, callDate, lastOrderDate,
                        lastCallDate, confirmedPriceNotes, mapsLink, addedBy, updatedAtMillis
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        userId=excluded.userId,
                        shopCode=excluded.shopCode,
                        name=excluded.name,
                        ownerName=excluded.ownerName,
                        phone=excluded.phone,
                        secondaryPhone=excluded.secondaryPhone,
                        address=excluded.address,
                        pinCode=excluded.pinCode,
                        routeId=excluded.routeId,
                        photoUri=excluded.photoUri,
                        latitude=excluded.latitude,
                        longitude=excluded.longitude,
                        note=excluded.note,
                        status=excluded.status,
                        inquiryStatus=excluded.inquiryStatus,
                        enquiryDate=excluded.enquiryDate,
                        callDate=excluded.callDate,
                        lastOrderDate=excluded.lastOrderDate,
                        lastCallDate=excluded.lastCallDate,
                        confirmedPriceNotes=excluded.confirmedPriceNotes,
                        mapsLink=excluded.mapsLink,
                        addedBy=excluded.addedBy,
                        updatedAtMillis=excluded.updatedAtMillis
                    """,
                    (
                        shop_id,
                        data.get("userId", ""),
                        data.get("shopCode", ""),
                        data.get("name", ""),
                        data.get("ownerName", ""),
                        data.get("phone", ""),
                        data.get("secondaryPhone", ""),
                        data.get("address", ""),
                        data.get("pinCode", ""),
                        data.get("routeId", ""),
                        data.get("photoUri", ""),
                        float(data.get("latitude", 0.0) or 0.0),
                        float(data.get("longitude", 0.0) or 0.0),
                        data.get("note", ""),
                        data.get("status", "active"),
                        data.get("inquiryStatus", "pending_inquiry"),
                        data.get("enquiryDate", ""),
                        data.get("callDate", ""),
                        data.get("lastOrderDate", ""),
                        data.get("lastCallDate", ""),
                        data.get("confirmedPriceNotes", ""),
                        data.get("mapsLink", ""),
                        data.get("addedBy", ""),
                        int(data.get("updatedAtMillis", int(time.time() * 1000)) or 0),
                    ),
                )
                conn.commit()
                write_snapshots()
                self._send_json(200, {"ok": True, "id": shop_id})
            elif path == "/api/orders":
                order_id = str(data.get("id") or data.get("orderCode") or f"ord_{int(time.time() * 1000)}")
                conn.execute(
                    """
                    INSERT INTO orders (
                        id, userId, orderCode, shopId, shopCode, shopName, routeId,
                        status, totalAmount, itemCount, itemsSummary, orderDate, note, updatedAtMillis
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        status=excluded.status,
                        totalAmount=excluded.totalAmount,
                        itemCount=excluded.itemCount,
                        itemsSummary=excluded.itemsSummary,
                        note=excluded.note,
                        updatedAtMillis=excluded.updatedAtMillis
                    """,
                    (
                        order_id,
                        data.get("userId", ""),
                        data.get("orderCode", ""),
                        data.get("shopId", ""),
                        data.get("shopCode", ""),
                        data.get("shopName", ""),
                        data.get("routeId", ""),
                        data.get("status", "confirmed"),
                        float(data.get("totalAmount", 0.0) or 0.0),
                        int(data.get("itemCount", 0) or 0),
                        data.get("itemsSummary", ""),
                        data.get("orderDate", ""),
                        data.get("note", ""),
                        int(data.get("updatedAtMillis", int(time.time() * 1000)) or 0),
                    ),
                )
                conn.commit()
                write_snapshots()
                self._send_json(200, {"ok": True, "id": order_id})
            elif path == "/api/routes":
                route_id = str(data.get("id") or f"R_{int(time.time() * 1000)}")
                conn.execute(
                    """
                    INSERT INTO routes (
                        id, name, vehicle, maxCrates, driverName, driverPhone,
                        dispatchWindow, stagingBay, colorIndex, isActive, updatedAtMillis
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        name=excluded.name,
                        vehicle=excluded.vehicle,
                        maxCrates=excluded.maxCrates,
                        driverName=excluded.driverName,
                        driverPhone=excluded.driverPhone,
                        dispatchWindow=excluded.dispatchWindow,
                        stagingBay=excluded.stagingBay,
                        colorIndex=excluded.colorIndex,
                        isActive=excluded.isActive,
                        updatedAtMillis=excluded.updatedAtMillis
                    """,
                    (
                        route_id,
                        data.get("name", route_id),
                        data.get("vehicle", ""),
                        int(data.get("maxCrates", 35) or 35),
                        data.get("driverName", ""),
                        data.get("driverPhone", ""),
                        data.get("dispatchWindow", ""),
                        data.get("stagingBay", ""),
                        int(data.get("colorIndex", 0) or 0),
                        1 if data.get("isActive", True) else 0,
                        int(data.get("updatedAtMillis", int(time.time() * 1000)) or 0),
                    ),
                )
                conn.commit()
                write_snapshots()
                self._send_json(200, {"ok": True, "id": route_id})
            else:
                self._send_json(404, {"ok": False, "error": "Not found"})

    def do_DELETE(self):
        if not self._check_auth():
            return
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/shops/"):
            shop_id = parsed.path.split("/api/shops/", 1)[1]
            with sqlite3.connect(DB_PATH) as conn:
                conn.execute("DELETE FROM shops WHERE id = ?", (shop_id,))
                conn.commit()
            write_snapshots()
            self._send_json(200, {"ok": True, "deleted": shop_id})
        else:
            self._send_json(404, {"ok": False, "error": "Not found"})


def main():
    global DATA_DIR, DB_PATH, JSON_SNAPSHOT_PATH, CSV_SNAPSHOT_PATH, PORT
    parser = argparse.ArgumentParser(description="SnackRoute Laptop Local Server")
    parser.add_argument("--data-dir", default="", help="Folder to store SQLite DB and JSON/CSV snapshots")
    parser.add_argument("--port", type=int, default=PORT, help="HTTP port (default 8787)")
    args = parser.parse_args()

    PORT = args.port
    DATA_DIR = resolve_data_dir(args.data_dir)
    DB_PATH = os.path.join(DATA_DIR, "snackroute_laptop.db")
    JSON_SNAPSHOT_PATH = os.path.join(DATA_DIR, "snackroute_data.json")
    CSV_SNAPSHOT_PATH = os.path.join(DATA_DIR, "snackroute_shops.csv")

    init_db()
    server = ThreadingHTTPServer(("0.0.0.0", PORT), SnackRouteHandler)
    ips = get_lan_ips()
    print("=" * 64)
    print("  SnackRoute Laptop Local Server is RUNNING")
    print(f"  Storage Folder : {DATA_DIR}")
    print(f"  SQLite Database: {DB_PATH}")
    print(f"  JSON Snapshot  : {JSON_SNAPSHOT_PATH}")
    print(f"  Web Dashboard  : http://localhost:{PORT}")
    for ip in ips:
        print(f"  Use in Phone App: http://{ip}:{PORT}")
    print("=" * 64)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down SnackRoute Laptop Local Server.")
        server.server_close()


if __name__ == "__main__":
    main()
