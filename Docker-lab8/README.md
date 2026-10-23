# Lab 8: Containerized Node.js and MySQL Stack Using Docker Compose

A multi-container setup that runs the **iVolve** Node.js application together with a **MySQL** database using Docker Compose, with persistent storage and a custom network.

## Overview

| Component | Details |
|-----------|---------|
| Application | Node.js app (built from the project Dockerfile), exposed on port `3000` |
| Database | MySQL with a database named `ivolve` |
| Network | Custom bridge network `ivolve-net` |
| Volumes | `mysql_db` for `/var/lib/mysql`, `apps_log` for `/app/logs/` |

Source application: https://github.com/Ibrahim-Adel15/iVolveApp.git

## Architecture

```
            ┌──────────────── ivolve-net ────────────────┐
 Browser ──►│  app (Node.js :3000)  ───────►  db (MySQL) │
 :3000      │        │                           │       │
            └────────┼───────────────────────────┼───────┘
                     ▼                           ▼
              apps_log volume               mysql_db volume
              (/app/logs/)                  (/var/lib/mysql)
```

## Prerequisites

- Docker
- Docker Compose v2
- A Docker Hub account

## Project Structure

```
iVolveApp/
├── Dockerfile
├── docker-compose.yml
├── app files...
└── README.md
```

## Steps

### 1. Clone the application

```bash
git clone https://github.com/Ibrahim-Adel15/iVolveApp.git
cd iVolveApp
```

### 2. Create `docker-compose.yml`

```yaml
services:
  db:
    image: mysql:8
    container_name: mysql-container
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_PASSWORD: appass
      MYSQL_DATABASE: ivolve
      MYSQL_USER: appuser
    networks:
      - ivolve-net
    volumes:
      - mysql_db:/var/lib/mysql
  app:
    build: .
    container_name: node-app
    environment:
      DB_HOST: db
      DB_PASSWORD: appass
      DB_PORT: 3306
      DB_USER: appuser
    networks:
      - ivolve-net
    volumes:
      - apps_log:/app/logs
    ports:
      - "3000:3000"
volumes:
  apps_log:
  mysql_db:
networks:
  ivolve-net:
    driver: bridge
```

Key points:
- The app connects to the database using the **service name** (`db`) as the host, not `localhost`.
- `MYSQL_DATABASE: ivolve` creates the required database on first start, and `MYSQL_USER` / `MYSQL_PASSWORD` create the application user.
- Both services are attached to the custom `ivolve-net` bridge network.
- Two named volumes keep data across restarts: `mysql_db` for `/var/lib/mysql` and `apps_log` for `/app/logs`.
- The credentials in this file are for lab use only. In a real project, keep secrets in an `.env` file or a secrets manager and never commit them.

### 3. Start the services

```bash
docker compose up -d --build
docker compose ps
```

### 4. Verify the application and DB connectivity

```bash
docker compose logs app
curl http://localhost:3000
docker compose exec db mysql -uroot -p -e "SHOW DATABASES;"
```

### 5. Verify network and volumes

```bash
docker network ls
docker volume ls
```

### 6. Verify data persistence

```bash
docker compose down
docker compose up -d
```

The data remains available because it is stored in the `mysql_db` volume.

### 7. Tag and push the image to Docker Hub

```bash
docker login
docker images
docker tag <local-image-name> <dockerhub-username>/ivolve-app:v1
docker push <dockerhub-username>/ivolve-app:v1
```

Docker Hub repository: `https://hub.docker.com/r/<dockerhub-username>/ivolve-app`

## Useful Commands

```bash
docker compose down          # stop and remove containers (volumes kept)
docker compose down -v       # also remove volumes (deletes data)
docker compose logs -f       # follow logs
docker compose exec db bash  # open a shell in the DB container
```

## Troubleshooting

- **App exits at startup:** MySQL may not be ready yet. Add a `healthcheck` to the `db` service and use `depends_on` with `condition: service_healthy`.
- **Cannot connect to DB:** confirm the app's DB host variable equals the service name, and that the credentials match the MySQL environment variables.

## What I Learned

- Defining multi-container applications with Docker Compose
- Service-to-service communication over a custom network
- Persisting data with named volumes
- Building, tagging, and publishing images to Docker Hub

## Author

**Essam Mostafa**  
GitHub: [Essammo22](https://github.com/Essammo22)
