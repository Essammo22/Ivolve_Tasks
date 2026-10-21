# Lab 4: Docker Image Optimization and Export/Load

## Overview

This lab shows how to shrink a Docker image using a **multi-stage build**, run the application as a **non-root user**, and move an image between hosts using `docker save` and `docker load`.

The application is a simple Java Spring Boot app built with Maven.

Source: [Ibrahim-Adel15/ImageOptimization](https://github.com/Ibrahim-Adel15/ImageOptimization)

## Project Structure

```
ImageOptimization-/
├── Dockerfile          # Original single-stage Dockerfile
├── Dockerfile.stage    # Optimized multi-stage Dockerfile (non-root)
├── pom.xml
└── src/main/java/com/example/demo/DemoApplication.java
```

## Step 1: Build the Original Image (image1)

The original Dockerfile uses a single stage. The final image keeps Maven, the full JDK, the source code, and the dependency cache.

```bash
docker build -t image1 .
docker images image1
docker run -d --name container1 -p 8080:8080 image1
docker logs container1
curl http://localhost:8080
docker stop container1 && docker rm container1
```

## Step 2: Optimized Multi-Stage Dockerfile

```dockerfile
# Stage 1: Builder
FROM maven:3.9.15-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Final
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder --chown=appuser:appgroup /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### What changed and why

| Change | Reason |
|---|---|
| Two stages (`builder` and final) | Build tools are needed only to build, not to run |
| `eclipse-temurin:17-jre-alpine` as the final base | JRE only, on a minimal Alpine image |
| `COPY --from=builder` for the jar only | Maven, source code, and the dependency cache are left behind |
| `pom.xml` copied before `src` | Dependencies are cached, so code changes rebuild faster |
| Non-root user with `USER appuser` | If the app is compromised, the attacker does not get root inside the container |

## Step 3: Build and Verify the Optimized Image (image2)

```bash
docker build -f Dockerfile.stage -t image2 .
docker images
docker run -d --name container2 -p 8080:8080 image2
docker logs container2
curl http://localhost:8080
docker exec container2 whoami
docker stop container2 && docker rm container2
```

`whoami` returns `appuser`, which confirms the container is not running as root.

## Size Comparison

| Image | Disk Usage | Content Size |
|---|---|---|
| image1 (single-stage) | 671 MB | 239 MB |
| image2 (multi-stage) | 302 MB | 88.2 MB |

The multi-stage build reduced the disk usage by about **55%** (671 MB to 302 MB) and the content size by about **63%** (239 MB to 88.2 MB).

> Disk Usage is the uncompressed size on the host. Content Size is the compressed size of the image layers.

## Step 4: Export and Load the Image

```bash
docker save -o lab-image.tar image2
ls -lh lab-image.tar

docker rmi image2
docker images

docker load -i lab-image.tar
docker images
```

After `docker load`, `image2` appears again in the local image list.

> Note: `lab-image.tar` is excluded from the repo through `.gitignore` because of its size.

## Screenshots

<!-- Add screenshots here, for example: -->
<!-- ![docker images](screenshots/docker-images.png) -->
<!-- ![whoami](screenshots/whoami.png) -->

## Key Takeaways

- Multi-stage builds separate the build environment from the runtime environment.
- A smaller image means faster pulls and a smaller attack surface.
- Running as non-root is a basic container security practice.
- `docker save` and `docker load` let you move images without a registry.
