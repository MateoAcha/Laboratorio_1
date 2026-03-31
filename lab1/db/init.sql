-- This script runs automatically when the PostgreSQL container starts for the first time.
-- It creates the initial table and inserts some sample data.

CREATE TABLE IF NOT EXISTS students (
    id        SERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    course    VARCHAR(100) NOT NULL,
    grade     NUMERIC(4, 2),
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO students (name, course, grade) VALUES
    ('Alice Martin',  'Docker 101',    9.5),
    ('Bob Dupont',    'Docker 101',    8.0),
    ('Clara Nguyen',  'Databases 101', 9.0),
    ('David Sanchez', 'Databases 101', 7.5);
