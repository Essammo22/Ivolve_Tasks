# Lab 5: Docker Image Vulnerability Scanning with Trivy

## Overview

This lab uses [Trivy](https://trivy.dev) to scan a Docker image for known vulnerabilities (CVEs), generate HTML reports, and then fix the findings by updating the base image and the application dependencies.

Source: [Ibrahim-Adel15/TrivyImage](https://github.com/Ibrahim-Adel15/TrivyImage)

## Project Structure

```
Docker-lab5/
├── Dockerfile          # Original image (trivy-lab:v1)
├── Dockerfile.fixed    # Remediated image (ivolve-app:v2)
├── package.json
├── app.js
├── report-v1.html      # Scan report before the fix
└── report-v2.html      # Scan report after the fix
```

## Part 1: Scan the Original Image

### Original Dockerfile

```dockerfile
FROM node:18-bullseye
WORKDIR /app
COPY package.json ./
RUN npm install
COPY app.js ./
EXPOSE 3000
CMD ["npm", "start"]
```

### Build the image

```bash
docker build -t trivy-lab:v1 .
```

### Install and verify Trivy

```bash
sudo apt-get install wget gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy

trivy --version
```

```
Version: 0.76.1
Vulnerability DB:
  Version: 2
  UpdatedAt: 2026-10-22 07:10:52.423530461 +0000 UTC
  NextUpdate: 2026-10-23 07:10:52.423530061 +0000 UTC
  DownloadedAt: 2026-10-22 09:43:09.148050759 +0000 UTC
```

### Full scan

```bash
trivy image trivy-lab:v1
```

Key findings from the scan log:

- Detected OS: Debian 11.11 (bullseye) with 409 packages.
- Trivy warned that this OS version is no longer supported, so security updates are not provided and detection may be incomplete.
- The application is a Node.js app, so `node-pkg` dependencies were scanned as well.

### HIGH and CRITICAL only

```bash
trivy image --severity HIGH,CRITICAL trivy-lab:v1
```

```
Total: 2343 (HIGH: 2122, CRITICAL: 221)
```

### HTML report

```bash
trivy image --format template \
  --template "@/usr/local/share/trivy/templates/html.tpl" \
  -o report-v1.html trivy-lab:v1
```

## Part 2: Remediation

### Root causes

| Problem | Where | Fix |
|---|---|---|
| Node 18 is end of life | `FROM node:18-bullseye` | Move to a supported Node version |
| Debian 11 is no longer supported | `FROM node:18-bullseye` | Move to Debian 12 (bookworm) |
| Outdated OS packages | Base image | `apt-get upgrade` |
| Vulnerable npm inside the image | `/usr/local/lib/node_modules/npm` | Update npm globally |
| `body-parser` 1.20.1 | Application dependency | Update to 1.20.3 |
| `path-to-regexp` 0.1.7 | Application dependency | Update to 0.1.13 |

### Fixed Dockerfile

```dockerfile
FROM node:22-bookworm-slim

WORKDIR /app

RUN apt-get update && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/*

RUN npm install -g npm@latest

COPY package.json ./
RUN npm install

COPY app.js ./

EXPOSE 3000

CMD ["npm", "start"]
```

Application dependencies were updated through `package.json` (updating `express` and, where needed, adding `overrides` for `body-parser` and `path-to-regexp`).

### Rebuild and rescan

```bash
docker build -t ivolve-app:v2 .

trivy image --severity HIGH,CRITICAL --ignore-unfixed ivolve-app:v2

trivy image --severity HIGH,CRITICAL --ignore-unfixed --format template \
  --template "@/usr/local/share/trivy/templates/html.tpl" \
  -o report-v2.html ivolve-app:v2
```

`--ignore-unfixed` hides vulnerabilities that have no fix available yet, which matches the lab requirement ("where fixes are available").

```
Total: 4 (HIGH: 4, CRITICAL: 0)
```

## Results

| Image | Base | Total | HIGH | CRITICAL |
|---|---|---|---|---|
| trivy-lab:v1 | node:18-bullseye (Debian 11) | 2343 | 2122 | 221 |
| ivolve-app:v2 | node:22-bookworm-slim (Debian 12) | 4 | 4 | 0 |

The remediation removed all CRITICAL findings and reduced the total from 2343 to 4, a reduction of more than 99%.

### Vulnerabilities that remain

The final scan reports 4 HIGH findings. Some findings cannot be fixed by the user because no patched version exists (the status is `affected`, `fix_deferred`, or `will_not_fix`). The `--ignore-unfixed` flag hides them from the check.

## Key Takeaways

- Old or unsupported base images are the biggest source of vulnerabilities, and updating `FROM` gives the largest improvement.
- Vulnerabilities come from three places: the OS packages, the application dependencies, and the tooling inside the image (such as npm).
- The Fixed Version column in the Trivy report tells you exactly which version to upgrade to.
- Some vulnerabilities have no fix yet, so scanning should be repeated regularly.
