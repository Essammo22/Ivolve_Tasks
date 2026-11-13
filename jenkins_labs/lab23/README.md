# GitOps with Jenkins and Argo CD

> Lab 23 | iVolve Cloud DevOps Accelerator

Every change to the application goes through Git. Jenkins builds and publishes the image, then commits the new image tag to the repository. Argo CD sees the commit and updates the cluster by itself. Nobody runs `kubectl apply` by hand.

## Tech Stack

| Layer | Tool |
|---|---|
| CI | Jenkins (Shared Library pipeline) |
| Build | Maven, JDK 17, Spring Boot |
| Containers | Docker |
| Orchestration | Kubernetes |
| CD / GitOps | Argo CD |
| Source of truth | GitHub |

## How It Works

1. A pipeline run starts in Jenkins.
2. Jenkins builds the app and creates a Docker image.
3. The image is pushed to the registry.
4. The `DeployOnK8s` stage edits the image tag in the deployment file.
5. Jenkins commits and pushes the change to GitHub.
6. Argo CD (application `ivolve`) detects the new commit.
7. Auto-sync applies the updated manifests to the cluster.

## Checklist

- [ ] Argo CD installed in the cluster
- [ ] Argo CD UI reachable and login works
- [ ] GitHub repository connected
- [ ] Application `ivolve` created
- [ ] Application points to the right repo and manifest directory
- [ ] Automatic sync enabled
- [ ] `DeployOnK8s` stage pushes the updated deployment file to GitHub
- [ ] Pipeline triggered, new image published
- [ ] Argo CD detected the change and synced

## Quick Start

**Install Argo CD**

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

**Get the admin password**

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d; echo
```

**Open the UI**

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Then browse to `https://localhost:8080` and log in as `admin`.

**Create the application** (replace the placeholders)

```bash
argocd app create ivolve \
  --repo https://github.com/Essammo22/Ivolve_Tasks.git \
  --path <manifests-directory> \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace <namespace> \
  --sync-policy automated \
  --auto-prune --self-heal
```

## Jenkins Stage: DeployOnK8s

The stage no longer talks to the cluster. It only updates Git:

```
edit image tag in deployment file -> git commit -> git push
```

Use `${BUILD_NUMBER}` as the image tag so each run changes the file and gives Argo CD something to detect.

## Result

Add screenshots here after running the lab:

- Jenkins pipeline, all stages green
- Argo CD application showing **Synced** and **Healthy**
- `kubectl get pods` showing the new version running

## What I Learned

- The difference between CI (Jenkins) and CD (Argo CD)
- Why Git should be the single source of truth for the cluster
- How auto-sync and self-heal keep the cluster matching the repo

## Author

**Essam Mostafa** | [GitHub: Essammo22](https://github.com/Essammo22)
