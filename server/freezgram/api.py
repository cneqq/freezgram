import hashlib
import hmac
import json
import secrets
import sqlite3
import sys
import time
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

STATE = Path("/var/lib/freezgram-api")
MAX_ID = (1 << 52) - 1
DAY = 86400


@contextmanager
def database():
    db = sqlite3.connect(STATE / "activity.sqlite3")
    try:
        with db:
            db.execute("CREATE TABLE IF NOT EXISTS active (account BLOB PRIMARY KEY, seen INTEGER NOT NULL)")
            db.execute("CREATE INDEX IF NOT EXISTS active_seen ON active(seen)")
            yield db
    finally:
        db.close()


class Api(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def reply(self, status, data=None):
        body = b"" if data is None else json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if body:
            self.wfile.write(body)

    def do_GET(self):
        if self.path != "/badges":
            return self.reply(404)
        try:
            data = json.loads(BADGES.read_bytes())
            for kind in ("users", "channels"):
                ids = data[kind]
                if not isinstance(ids, list) or len(ids) > 5000 or any(
                    type(i) is not int or not 0 < i <= MAX_ID for i in ids
                ):
                    raise ValueError("Invalid badge recipients")
            self.reply(200, {kind: data[kind] for kind in ("users", "channels")})
        except (OSError, ValueError, KeyError, TypeError):
            self.reply(503)

    def do_POST(self):
        if self.path != "/activity":
            return self.reply(404)
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 128:
                return self.reply(400)
            user_id = json.loads(self.rfile.read(size))["id"]
            if type(user_id) is not int or not 0 < user_id <= MAX_ID:
                return self.reply(400)
        except (ValueError, KeyError, TypeError):
            return self.reply(400)
        now = int(time.time())
        account = hmac.digest(KEY, str(user_id).encode(), hashlib.sha256)
        with database() as db:
            db.execute("INSERT INTO active VALUES (?, ?) ON CONFLICT(account) DO UPDATE SET seen = excluded.seen",
                       (account, now))
        self.reply(204)

    def setup(self):
        self.request.settimeout(5)
        super().setup()


class Server(HTTPServer):
    next_cleanup = 0

    def service_actions(self):
        now = int(time.time())
        if now >= self.next_cleanup:
            with database() as db:
                db.execute("DELETE FROM active WHERE seen < ?", (now - 30 * DAY,))
            self.next_cleanup = now + 3600


if __name__ == "__main__":
    if sys.argv[1:] == ["stats"]:
        now = int(time.time())
        with database() as db:
            print(json.dumps({f"{days}d": db.execute(
                "SELECT count(*) FROM active WHERE seen >= ?", (now - days * DAY,)
            ).fetchone()[0] for days in (1, 30)}))
    else:
        BADGES = Path("/etc/freezgram-api/badges.json")
        key_path = STATE / "hmac.key"
        try:
            with key_path.open("xb") as key_file:
                key_file.write(secrets.token_bytes(32))
        except FileExistsError:
            pass
        KEY = key_path.read_bytes()
        if len(KEY) != 32:
            raise RuntimeError("Invalid server key")
        Server(("127.0.0.1", 8091), Api).serve_forever()
