import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).parent / "data.db"

INIT_SQL = Path(__file__).parent.parent / "database" / "init.sql"


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_connection()
    with open(INIT_SQL, "r", encoding="utf-8") as f:
        conn.executescript(f.read())
    conn.commit()
    conn.close()
