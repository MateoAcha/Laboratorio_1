# Lab 1 — Docker & Databases

A minimal template that shows how to run a **PostgreSQL** database inside a Docker container and connect to it from your machine.

---

## Project structure

```
lab1/
├── docker-compose.yml   ← defines the database service
└── db/
    └── init.sql         ← table + sample data (runs on first start)
```

---

## Installing Docker

### macOS

1. Go to [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)
2. Click **Download for Mac** — choose **Apple Silicon** (M1/M2/M3) or **Intel** depending on your machine
   - To check your chip: Apple menu → **About This Mac**
3. Open the downloaded `.dmg` file and drag **Docker** to your Applications folder
4. Launch Docker from Applications and follow the setup wizard
5. Wait for the whale icon in the menu bar to stop animating (Docker is ready)
6. Verify in a terminal:
   ```bash
   docker --version
   docker compose version
   ```

### Windows

1. Make sure **WSL 2** is enabled (required by Docker Desktop):
   - Open PowerShell as Administrator and run:
     ```powershell
     wsl --install
     ```
   - Restart your computer if prompted
2. Go to [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)
3. Click **Download for Windows**
4. Run the installer (`Docker Desktop Installer.exe`) and follow the wizard
   - Leave **"Use WSL 2 instead of Hyper-V"** checked
5. After installation, launch **Docker Desktop** from the Start menu
6. Wait for the whale icon in the taskbar to stop animating (Docker is ready)
7. Verify in PowerShell or Command Prompt:
   ```powershell
   docker --version
   docker compose version
   ```

---

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Docker Desktop | ≥ 24 | `docker --version` |
| Docker Compose | included with Docker Desktop | `docker compose version` |

---

## Quick start

```bash
# 1. Enter the project folder
cd lab1

# 2. Start the database container
docker compose up -d

# 3. Connect with psql
docker compose exec db psql -U postgres -d demo
```

Stop everything:
```bash
docker compose down          # stop & remove the container (data is preserved)
docker compose down -v       # also delete the volume (fresh start)
```

---

## Key concepts illustrated

### 1. docker-compose.yml — running a container declaratively
- The **`image`** field pulls a ready-made PostgreSQL image from Docker Hub — no build step needed.
- **`ports`** maps `host:container`, so you can connect from your machine on port 5432.
- **`environment`** sets the DB name, user, and password at startup.

### 2. Volumes — persisting data
```yaml
volumes:
  - db_data:/var/lib/postgresql/data   # named volume → data survives restarts
  - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql  # seed script
```

### 3. init.sql — automatic initialisation
Any `.sql` file mounted into `/docker-entrypoint-initdb.d/` runs **once** the first time the container starts (when no data volume exists yet).

---

## Useful commands

```bash
# Start in background
docker compose up -d

# View logs
docker compose logs db

# Open a psql shell inside the container
docker compose exec db psql -U postgres -d demo

# List tables
\dt

# Query the sample data
SELECT * FROM students;

# Exit psql
\q
```

---

## Exercises for students

1. **Explore the data** — connect with `psql` and run `SELECT * FROM students;`
2. **Add a row** — insert a new student with `INSERT INTO students (name, course, grade) VALUES ('You', 'Docker 101', 10);`
3. **Add a column** — add an `email` column to `init.sql`, then do `docker compose down -v && docker compose up -d` and check the result
4. **Persistence** — stop the stack and restart it; verify your inserted row is still there
5. **Fresh start** — run `docker compose down -v` and notice all data is gone
