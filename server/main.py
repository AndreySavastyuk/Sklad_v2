from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.responses import HTMLResponse
from typing import List, Optional
import sqlite3
from pathlib import Path
import csv
from openpyxl import load_workbook

from .database import get_connection, init_db
from .schemas import Task, TaskCreate

app = FastAPI(title="Warehouse Task Server")

STATIC_DIR = Path(__file__).parent / "public"


@app.on_event("startup")
def startup():
    init_db()


@app.get("/", response_class=HTMLResponse)
def index():
    html_file = STATIC_DIR / "index.html"
    if html_file.exists():
        return html_file.read_text(encoding="utf-8")
    return "<h1>Warehouse Task Server</h1>"


@app.post("/tasks", response_model=Task)
def create_task(task: TaskCreate):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(
        """INSERT INTO tasks (task_number, comment, assembly_count, created_by)
               VALUES (?, ?, ?, ?)""",
        (task.task_number, task.comment, task.assembly_count, task.created_by),
    )
    conn.commit()
    task_id = cursor.lastrowid
    row = cursor.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    conn.close()
    return Task(**row)


@app.get("/tasks", response_model=List[Task])
def list_tasks(status: Optional[str] = None):
    conn = get_connection()
    cursor = conn.cursor()
    if status:
        rows = cursor.execute(
            "SELECT * FROM tasks WHERE status = ? ORDER BY created_at DESC", (status,)
        ).fetchall()
    else:
        rows = cursor.execute("SELECT * FROM tasks ORDER BY created_at DESC").fetchall()
    conn.close()
    return [Task(**row) for row in rows]


@app.get("/tasks/{task_id}", response_model=Task)
def get_task(task_id: int):
    conn = get_connection()
    row = conn.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    conn.close()
    if row:
        return Task(**row)
    raise HTTPException(status_code=404, detail="Task not found")


@app.put("/tasks/{task_id}/status", response_model=Task)
def update_status(task_id: int, status: str):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(
        "UPDATE tasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        (status, task_id),
    )
    conn.commit()
    row = cursor.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    conn.close()
    if row:
        return Task(**row)
    raise HTTPException(status_code=404, detail="Task not found")


@app.post("/tasks/upload")
def upload_excel(file: UploadFile = File(...)):
    wb = load_workbook(file.file)
    sheet = wb.active
    conn = get_connection()
    cursor = conn.cursor()
    created_ids = []
    for row in sheet.iter_rows(min_row=2, values_only=True):
        task_number = str(row[0])
        comment = str(row[1]) if row[1] else None
        cursor.execute(
            "INSERT INTO tasks (task_number, comment) VALUES (?, ?)",
            (task_number, comment),
        )
        created_ids.append(cursor.lastrowid)
    conn.commit()
    conn.close()
    return {"created": created_ids}
