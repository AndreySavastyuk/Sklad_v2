from pydantic import BaseModel
from typing import Optional


class TaskCreate(BaseModel):
    task_number: str
    comment: Optional[str] = None
    assembly_count: int = 1
    created_by: str = "Инженер ПДО"


class Task(BaseModel):
    id: int
    task_number: str
    status: str
    comment: Optional[str]
    assembly_count: int
    created_at: str
    created_by: str

    class Config:
        orm_mode = True


class TaskUpdate(BaseModel):
    comment: Optional[str] = None
    assembly_count: Optional[int] = None
    status: Optional[str] = None
