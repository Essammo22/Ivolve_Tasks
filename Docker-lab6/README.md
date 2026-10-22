# Lab 6: Docker Environment Variables and Resource Management

## Overview

This lab covers three ways to pass configuration into a container (`-e`, `--env-file`, and `ENV` in the Dockerfile) and how to limit and monitor container resources (CPU and memory).

Source: [Ibrahim-Adel15/PythonApp](https://github.com/Ibrahim-Adel15/PythonApp)

## Project Structure

```
Docker-lab6/
├── app.py            # Python application (port 5000)
├── requirements.txt  # Python dependencies
├── Dockerfile        # Image without hardcoded variables (ivolve1)
├── Dockerfile2       # Image with hardcoded ENV variables (ivolve2)
└── env.txt           # Environment variables file
```

## Part 1: Build the Image and Run with `-e` and Resource Limits

### Dockerfile

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 5000
CMD ["python", "app.py"]
```

### Build and run

```bash
docker build -t ivolve1 .

docker run -d --name container1 -p 5000:5000 \
  -e APP_NAME=iVolveApp1 -e APP_ENV=stg \
  --cpus="1" --memory="256m" ivolve1
```

| Option | Purpose |
|---|---|
| `-e APP_NAME=... -e APP_ENV=...` | Sets environment variables inside the container |
| `--cpus="1"` | Limits the container to 1 CPU |
| `--memory="256m"` | Limits the container to 256 MB of memory |

### Verify the application

```bash
curl http://localhost:5000
```

```
<paste output here>
```

### Verify the environment variables

```bash
docker exec container1 env | grep APP_
```

```
APP_NAME=iVolveApp1
APP_ENV=stg
```

### Monitor CPU and memory

```bash
docker stats container1 --no-stream
```

```
CONTAINER ID   NAME         CPU %     MEM USAGE / LIMIT   MEM %     NET I/O         BLOCK I/O    PIDS
3ba724ac97db   container1   0.03%     21.43MiB / 256MiB   8.37%     3.15kB / 126B   0B / 176kB   1
```

The `MEM USAGE / LIMIT` column shows the 256 MiB limit, which confirms the memory limit was applied.

## Part 2: Load Variables from `env.txt`

`env.txt`:

```
APP_NAME=iVolveApp2
APP_ENV=prod
```

```bash
docker stop container1 && docker rm container1

docker run -d --name container2 -p 5000:5000 --env-file env.txt ivolve1
docker exec container2 env | grep APP_
```

```
APP_NAME=iVolveApp2
APP_ENV=prod
```

## Part 3: Hardcode Variables in the Dockerfile

`Dockerfile2` is the same as `Dockerfile`, with these two lines added before `CMD`:

```dockerfile
ENV APP_NAME=iVolveApp3
ENV APP_ENV=dev
```

```bash
docker stop container2 && docker rm container2

docker build -f Dockerfile2 -t ivolve2 .
docker run -d --name container2 -p 5000:5000 ivolve2
docker exec container2 env | grep APP_
```

```
APP_NAME=iVolveApp3
APP_ENV=dev
```

The application response confirms the values from the Dockerfile:

```bash
curl http://localhost:5000
```

```
{"application":"iVolveApp3","environment":"dev","message":"Welcome to the Docker Env & Resource Management Lab!"}
```

## Comparison of the Three Methods

| Method | Where it is set | Best for |
|---|---|---|
| `-e` | `docker run` command | A few variables, quick overrides |
| `--env-file` | A separate file | Many variables, per-environment config |
| `ENV` | Dockerfile (baked into the image) | Safe defaults that do not change |

Priority: values passed at run time (`-e` or `--env-file`) override `ENV` values from the Dockerfile.

> Do not put secrets (passwords, API keys) in `ENV`, because they stay visible in the image layers.

## Key Takeaways

- Environment variables let you change app behavior without rebuilding the image.
- `--cpus` and `--memory` prevent one container from consuming all host resources.
- `docker stats` shows live CPU and memory usage against the configured limits.
- Run-time variables take priority over `ENV` in the Dockerfile.
