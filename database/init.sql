-- Инициализация базы данных для системы управления производственными заданиями v2.0

CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_number TEXT UNIQUE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status TEXT DEFAULT 'created', -- created, sent, in_progress, completed
    comment TEXT,
    assembly_count INTEGER DEFAULT 1,
    sent_to_tablet BOOLEAN DEFAULT FALSE,
    sent_at DATETIME,
    created_by TEXT DEFAULT 'Инженер ПДО'
);

CREATE TABLE IF NOT EXISTS task_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    drawing_number TEXT,
    name TEXT,
    order_number TEXT,
    required_quantity TEXT,
    ready_quantity TEXT,
    in_production_quantity TEXT,
    on_sgp_quantity TEXT,
    binding TEXT,
    status TEXT DEFAULT 'pending', -- pending, in_progress, completed
    level INTEGER DEFAULT 0,
    priority INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks (id)
);

-- Новая таблица для реестра приемки
CREATE TABLE IF NOT EXISTS registry_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    task_item_id INTEGER,
    drawing_number TEXT,
    name TEXT,
    received_quantity TEXT,
    quality_status TEXT DEFAULT 'pending', -- pending, approved, rejected, rework
    location TEXT, -- местоположение на складе
    notes TEXT,
    received_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    received_by TEXT,
    checked_by TEXT,
    transferred_to_manager BOOLEAN DEFAULT FALSE,
    transfer_date DATETIME,
    FOREIGN KEY (task_id) REFERENCES tasks (id),
    FOREIGN KEY (task_item_id) REFERENCES task_items (id)
);

-- Таблица истории изменений
CREATE TABLE IF NOT EXISTS change_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_name TEXT,
    record_id INTEGER,
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    changed_by TEXT,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    action_type TEXT -- INSERT, UPDATE, DELETE
);

-- Таблица статусов для настройки
CREATE TABLE IF NOT EXISTS status_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category TEXT, -- task, item, registry
    status_code TEXT,
    status_name TEXT,
    status_color TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Вставка базовых статусов
INSERT OR IGNORE INTO status_config (category, status_code, status_name, status_color, sort_order) VALUES
('task', 'created', 'Создано', '#6B7280', 1),
('task', 'sent', 'Отправлено', '#3B82F6', 2),
('task', 'in_progress', 'В работе', '#F59E0B', 3),
('task', 'completed', 'Выполнено', '#10B981', 4),
('item', 'pending', 'Ожидает', '#6B7280', 1),
('item', 'in_progress', 'В работе', '#F59E0B', 2),
('item', 'completed', 'Выполнено', '#10B981', 3),
('registry', 'pending', 'Ожидает проверки', '#6B7280', 1),
('registry', 'approved', 'Принято', '#10B981', 2),
('registry', 'rejected', 'Отклонено', '#EF4444', 3),
('registry', 'rework', 'На доработку', '#F59E0B', 4);

-- Триггеры для истории изменений
CREATE TRIGGER IF NOT EXISTS tasks_history_update 
AFTER UPDATE ON tasks
FOR EACH ROW
BEGIN
    INSERT INTO change_history (table_name, record_id, field_name, old_value, new_value, changed_by, action_type)
    SELECT 'tasks', NEW.id, 'status', OLD.status, NEW.status, 'system', 'UPDATE'
    WHERE OLD.status != NEW.status;
END;

CREATE TRIGGER IF NOT EXISTS registry_history_insert
AFTER INSERT ON registry_entries
FOR EACH ROW
BEGIN
    INSERT INTO change_history (table_name, record_id, field_name, old_value, new_value, changed_by, action_type)
    VALUES ('registry_entries', NEW.id, 'created', '', 'Новая запись в реестре', NEW.received_by, 'INSERT');
END;

CREATE TRIGGER IF NOT EXISTS task_items_history_update
AFTER UPDATE ON task_items
FOR EACH ROW
BEGIN
    INSERT INTO change_history (table_name, record_id, field_name, old_value, new_value, changed_by, action_type)
    SELECT 'task_items', NEW.id, 'status', OLD.status, NEW.status, 'system', 'UPDATE'
    WHERE OLD.status != NEW.status;
END; 