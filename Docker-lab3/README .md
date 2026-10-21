# Lab 3: Dockerizing a Java Maven Application

This lab packages the Maven calculator app from Lab 1 into Docker images using two different approaches, then compares the resulting image sizes.

> **Source:** the application code is from [Ibrahim-Adel15/CalculatorMavenApp](https://github.com/Ibrahim-Adel15/CalculatorMavenApp).

## Project Structure

```
Docker-lab3
└── CalculatorMavenApp
    ├── Dockerfile          # Approach 1: build the app inside Docker
    ├── Dockerfile.maven    # Approach 2: build locally, then copy the jar
    ├── .dockerignore
    ├── pom.xml
    └── src
```

## Approach 1: Build the app inside Docker (`Dockerfile`)

The image is based on an official Maven + JDK 17 image. The source code is copied in and `mvn package` runs **during the image build**, so no Java or Maven is needed on the host.

The container runs as a **non-root user** (`appuser`):
the user is created, owns the `/app` directory, receives the copied files via `COPY --chown`, and is activated with `USER appuser` before the build runs.

```bash
docker build -t dockerfile-mavn1 .
```

## Approach 2: Build locally, then copy the jar (`Dockerfile.maven`)

The jar is built on the host first, and the image only copies the finished `target/calculator.jar`.

```bash
mvn package
docker build -f Dockerfile.maven -t dockerfile-mavn .
```

## Run the container

The app asks for two numbers, so run it interactively with `-it`:

```bash
docker run -it --rm dockerfile-mavn1
docker run -it --rm dockerfile-mavn
```

Verify the container does not run as root:

```bash
docker run --rm dockerfile-mavn1 whoami
```

Expected output: `appuser`.

## Image Size Comparison

```bash
docker image ls
```

| Image              | Approach                  | Disk usage | Content size |
|--------------------|---------------------------|-----------:|-------------:|
| `dockerfile-mavn1` | 1: build inside Docker    | 602 MB     | 206 MB       |
| `dockerfile-mavn`  | 2: build locally, copy jar | 453 MB     | 153 MB       |

## Conclusion

Approach 2 produces a smaller image (about 150 MB less on disk), because the final image holds only the runtime and the jar. Approach 1 is more self-contained and reproducible: anyone with Docker can build it without installing Java or Maven, at the cost of a larger image that includes the build toolchain.
