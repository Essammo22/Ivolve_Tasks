# Lab 7: Multi-Network Microservices, Volumes, and Bind Mounts

## Overview

This lab runs a three-service Python application (frontend, backend, notification) on two isolated Docker bridge networks. It uses Docker volumes for logs and a bind mount for the backend application data.

Source: [Ibrahim-Adel15/MicroserviceApp](https://github.com/Ibrahim-Adel15/MicroserviceApp)

## Architecture

```
                 frontend-backend                backend-notification
                 172.20.0.0/24                   172.21.0.0/24

 Browser ---> [ frontend ] -----------> [ backend ] -----------> [ notification ]
   :8080          :3000                    :5000                      :5001
                                              |                          |
                                      bind mount: ./backend-data   volume: notification-logs
                                      volume: backend (logs)
```

- The **backend** is connected to both networks, so it is the only service that can reach both the frontend and the notification service.
- The **frontend** and the **notification** service are on separate networks and cannot reach each other directly.

## Project Structure

```
Docker-lab7/
├── MicroserviceApp/
│   ├── frontend/        # Dockerfile, app.py, requirements.txt
│   ├── backend/         # Dockerfile, app.py, requirements.txt
│   └── notification/    # Dockerfile, app.py, requirements.txt
└── backend-data/        # Host directory (bind mount): messages.json, backend.log
```

## Step 1: Dockerfiles and Images

All three services use `python:3.12-slim`.

**Frontend** (port 3000):

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
EXPOSE 3000
CMD ["python", "app.py"]
```

**Backend** (port 5000):

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
RUN mkdir -p /app/data /app/logs
EXPOSE 5000
CMD ["python", "app.py"]
```

**Notification** (port 5001):

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
RUN mkdir -p /app/logs
EXPOSE 5001
CMD ["python", "app.py"]
```

Build the images:

```bash
cd MicroserviceApp
docker build -t frontend ./frontend
docker build -t backend ./backend
docker build -t notification_services ./notification
```

## Step 2: Networks with Separate Subnets

```bash
docker network create --driver bridge --subnet 172.20.0.0/24 frontend-backend
docker network create --driver bridge --subnet 172.21.0.0/24 backend-notification
docker network ls
```

| Network | Subnet | Connects |
|---|---|---|
| frontend-backend | 172.20.0.0/24 | frontend, backend |
| backend-notification | 172.21.0.0/24 | backend, notification |

## Step 3: Volumes and Bind Mount

```bash
# Docker-managed volumes for logs
docker volume create backend
docker volume create notification-logs

# Host directory for the backend application data (bind mount)
mkdir -p backend-data
```

| Storage | Type | Mounted at | Purpose |
|---|---|---|---|
| `backend` | Volume | `/app/logs` (backend) | Backend logs |
| `notification-logs` | Volume | `/app/logs` (notification) | Notification logs |
| `./backend-data` | Bind mount | `/app/data` (backend) | Messages file, visible on the host |

## Step 4: Run the Containers

The containers are started in dependency order: notification, then backend, then frontend.

**Notification** on `backend-notification`:

```bash
docker run -d --name notification \
  --network backend-notification \
  -v notification-logs:/app/logs \
  notification_services
```

**Backend** on both networks. `docker run` accepts one `--network`, so the second network is attached with `docker network connect`:

```bash
docker run -d --name backend \
  --network backend-notification \
  -v backend:/app/logs \
  -v "$(pwd)/backend-data:/app/data" \
  -e NOTIFICATION_URL=http://notification:5001/notify \
  backend

docker network connect frontend-backend backend
```

**Frontend** on `frontend-backend`, with port 8080 published:

```bash
docker run -d --name frontend \
  --network frontend-backend \
  -p 8080:3000 \
  -e BACK_END=http://backend:5000 \
  frontend
```

Containers reach each other by name (`notification`, `backend`) because Docker provides DNS on user-defined networks.

## Step 5: Verification

```bash
docker ps
docker network inspect frontend-backend
docker network inspect backend-notification
```

- `frontend-backend` lists the frontend and backend containers.
- `backend-notification` lists the backend and notification containers.

Open `http://localhost:8080`, create a message, and retrieve it from the page.

The bind mount makes the backend data available on the host:

```bash
ls backend-data
```

```
backend.log  messages.json
```

### Persistence check

```bash
docker rm -f backend
# run the backend container again with the same command from Step 4,
# then reconnect it to frontend-backend
```

The messages are still there after the container is recreated, because the data lives in `backend-data` on the host and not inside the container.

## Volumes vs Bind Mounts

| | Volume | Bind Mount |
|---|---|---|
| Location | Managed by Docker (`/var/lib/docker/volumes/`) | Any path on the host |
| Used here for | Logs | Backend application data |
| Visible from the host | Only through Docker paths | Directly in the project folder |

## Key Takeaways

- Separate bridge networks isolate services: the frontend cannot reach the notification service.
- A container on two networks acts as the bridge between them.
- Containers find each other by name on user-defined networks, so no IP addresses are hardcoded.
- Use volumes for data Docker should manage and bind mounts when the host needs direct access to the files.
