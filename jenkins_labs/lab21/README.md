# Lab 21: Jenkins Pipeline for Application Deployment

A declarative Jenkins pipeline that builds, scans, ships and deploys a Java (Spring Boot) application to a Kubernetes cluster.

**Source app:** [Ibrahim-Adel15/DemoApp](https://github.com/Ibrahim-Adel15/DemoApp.git) (Java 17 + Maven)
**Docker image:** `essammo22/demoapp`
**Kubernetes namespace:** `ivolve`

---

## Pipeline Overview

```
Build App -> Build Docker Image -> Scan Image -> Push to DockerHub
        -> Delete Local Image -> Update Deployment File -> Deploy to Kubernetes
```

| # | Stage | What it does |
|---|-------|--------------|
| 1 | Build App | Compiles and packages the app with Maven (JDK 17) |
| 2 | Build Docker Image | Builds the image from the Dockerfile, tagged with `BUILD_NUMBER` |
| 3 | Scan Docker Image | Scans the image for HIGH/CRITICAL vulnerabilities with Trivy |
| 4 | Push Image to DockerHub | Logs in with Jenkins credentials and pushes the image |
| 5 | Delete Local Image | Removes the image from the Jenkins agent to free disk space |
| 6 | Update Deployment File | Replaces the image tag in `deployment.yaml` using `sed` |
| 7 | Deploy to Kubernetes | Applies the manifest with `kubectl` using a kubeconfig credential |

### Post actions

| Block | Purpose |
|-------|---------|
| `always` | Runs after every build, prints that the pipeline finished |
| `success` | Runs only when all stages pass |
| `failure` | Runs only when a stage fails |

---

## Prerequisites

| Component | Notes |
|-----------|-------|
| Jenkins | Runs on **Java 21** (required by recent Jenkins versions) |
| JDK 17 | Used by the build itself, registered in Jenkins as `jdk17` |
| Maven | Registered in Jenkins as `maven3` |
| Docker | The `jenkins` user must be in the `docker` group |
| Trivy | Installed on the Jenkins machine |
| kubectl | Installed on the Jenkins machine |
| Kubernetes cluster | Namespace `ivolve` |

### Jenkins plugins

- Pipeline
- Git
- Docker Pipeline
- Kubernetes CLI

### Jenkins credentials

| ID | Type | Used for |
|----|------|----------|
| `dockerhub-creds` | Username with password | Pushing to DockerHub |
| `kubeconfig` | Secret file | Authenticating to the cluster |

> Adjust the IDs above if yours are named differently in the Jenkinsfile.

---

## Setup

### 1. Run Jenkins on Java 21 while keeping JDK 17 for the app

Jenkins needs Java 21, but the app needs JDK 17. Both can live on the same machine: point only the Jenkins service to Java 21.

```bash
sudo apt install -y openjdk-21-jre-headless openjdk-17-jdk maven
sudo systemctl edit jenkins
```

```ini
[Service]
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
```

```bash
sudo systemctl daemon-reload
sudo systemctl reset-failed jenkins
sudo systemctl start jenkins
```

### 2. Register the tools in Jenkins

**Manage Jenkins -> Tools**

| Tool | Name | Path |
|------|------|------|
| JDK | `jdk17` | `/usr/lib/jvm/java-17-openjdk-amd64` |
| Maven | `maven3` | `/usr/share/maven` |

The names must match the `tools {}` block in the Jenkinsfile exactly.

### 3. Give Jenkins access to Docker

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
sudo -u jenkins docker ps
```

### 4. Install Trivy

```bash
sudo apt install -y wget apt-transport-https gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | sudo tee /etc/apt/sources.list.d/trivy.list
sudo apt update
sudo apt install -y trivy
```

### 5. Kubernetes access (RBAC)

The pipeline deploys with the `jenkins-sa` ServiceAccount in the `ivolve` namespace.

```bash
kubectl create namespace ivolve
kubectl create serviceaccount jenkins-sa -n ivolve
```

Role used by the pipeline:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: ivolve
  name: ivolve-deploy-role
rules:
  - apiGroups: [""]
    resources: ["pods", "services"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch", "create", "update", "patch"]
  - apiGroups: ["apps"]
    resources: ["replicasets"]
    verbs: ["get", "list", "watch"]
```

Bind it to `jenkins-sa` with a RoleBinding, then build the kubeconfig for that ServiceAccount and upload it to Jenkins as the `kubeconfig` secret file.

### 6. Create the Jenkins job

1. **New Item -> Pipeline**
2. **Pipeline script from SCM** -> Git
3. Repository: `https://github.com/Essammo22/Ivolve_Tasks.git`
4. Script Path: `jenkins_labs/lab21/jenkinsfile`
5. **Build Now**

---

## Image tagging

Each build is tagged with Jenkins' `BUILD_NUMBER` instead of `latest`, so every deployment maps to a specific build and rollbacks are easy.

```
essammo22/demoapp:<BUILD_NUMBER>
```

## Security scan

Trivy scans the image for `HIGH` and `CRITICAL` vulnerabilities:

```bash
trivy image --timeout 15m --scanners vuln --severity HIGH,CRITICAL --exit-code 0 essammo22/demoapp:<BUILD_NUMBER>
```

- `--exit-code 0` is used for this lab so findings are reported without blocking the pipeline.
- In a production pipeline this should be `--exit-code 1` so the deployment is blocked when serious vulnerabilities are found.
- `--timeout 15m` avoids `context deadline exceeded` on slower machines.

---

## Troubleshooting (issues hit while building this lab)

| Problem | Cause | Fix |
|---------|-------|-----|
| Jenkins fails to start: "older than the minimum required version (Java 21)" | Jenkins needs Java 21 | Set `JAVA_HOME` to Java 21 in a systemd override |
| `release version 17 not supported` | Maven used a JDK without `javac` 17 | Install `openjdk-17-jdk` and set `jdk17` in the Jenkinsfile `tools {}` block |
| `Tool type "maven" does not have an install of "maven3"` | Maven not registered in Jenkins | Add it in Manage Jenkins -> Tools |
| `permission denied ... /var/run/docker.sock` | `jenkins` user not in the `docker` group | `usermod -aG docker jenkins` then restart Jenkins |
| Trivy `context deadline exceeded` | Default 5 minute timeout too short | Add `--timeout 15m` |
| Trivy `Require at least 1 argument` | Image name variables were empty | Define `IMAGE_NAME` / `IMAGE_TAG` in `environment {}` and use double quotes in `sh` |
| `services is forbidden` for `jenkins-sa` | Role missing permission | Add `services` to the Role rules |
| `no space left on device` risk | Disk almost full | Grow the partition (`growpart`, `resize2fs`) and prune Docker |

---

## Project structure

```
jenkins_labs/
  lab21/
    jenkinsfile
    README.md
```

## Possible improvements

- Use a multi-stage Dockerfile so Maven and build tools are not shipped in the final image.
- Upgrade Spring Boot to resolve the dependency vulnerabilities reported by Trivy.
- Switch Trivy to `--exit-code 1` to enforce a security gate.
- Add a `kubectl rollout status` check after the deploy stage.

## Author

**Essam Mostafa** - [GitHub: Essammo22](https://github.com/Essammo22)
