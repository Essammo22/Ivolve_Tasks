# Lab 22: CI/CD Pipeline with Jenkins Agents and Shared Libraries

A CI/CD pipeline built with Jenkins that builds, tests, scans, and deploys a containerized application to a Kubernetes cluster. The pipeline logic is split into reusable **Jenkins Shared Libraries** and runs on a dedicated **Jenkins agent (slave)** instead of the controller.

## Project Goals

- Clone and use the Dockerfile from the [DemoApp](https://github.com/Ibrahim-Adel15/DemoApp.git) repository
- Create three shared libraries: `BuildApp`, `BuildImage`, `DeployOnK8s`
- Update the pipeline (Jenkinsfile) to call these libraries
- Configure a Jenkins agent to run the pipeline

## Architecture

```
Developer ──push──> Git Repo ──> Jenkins Controller ──> Jenkins Agent (slave)
                                                              │
                      ┌───────────────────────────────────────┤
                      ▼                   ▼                   ▼
                  BuildApp           BuildImage          DeployOnK8s
               (unit test, build)  (build, scan,       (edit deployment,
                                    push, cleanup)      deploy to cluster)
                                                              │
                                                              ▼
                                                      Kubernetes Cluster
```

## Shared Libraries

| Library | Responsibility |
|---|---|
| **BuildApp** | Run unit tests, build the application |
| **BuildImage** | Build the Docker image, scan the image, push it to the registry, delete the image locally |
| **DeployOnK8s** | Edit the deployment file (update image tag), deploy to the Kubernetes cluster |

### Repository Structure

```
.
├── Jenkinsfile
├── README.md
├── vars/
│   ├── buildApp.groovy
│   ├── buildImage.groovy
│   └── deployOnK8s.groovy
└── (Dockerfile and app source from DemoApp)
```

## Pipeline Stages

1. **Checkout**: pull the source code
2. **BuildApp**: unit tests + build
3. **BuildImage**: build image, scan, push, remove local image
4. **DeployOnK8s**: update the deployment manifest and apply it to the cluster

## Jenkins Agent Setup

1. Prepare a machine/VM to act as the agent (Java installed, Docker and `kubectl` available).
2. In Jenkins: **Manage Jenkins → Nodes → New Node**.
3. Set the remote root directory, a label (e.g. `slave`), and the launch method (SSH).
4. Add credentials and verify the node shows as **online**.
5. In the Jenkinsfile, target it with `agent { label 'slave' }`.

## Configuring the Shared Library in Jenkins

1. **Manage Jenkins → System → Global Pipeline Libraries**.
2. Add a library with a name, default version (branch), and the Git repository URL.
3. Load it in the Jenkinsfile:

```groovy
@Library('<your-library-name>') _
```

## Prerequisites

- Jenkins with the Pipeline and Docker plugins
- A Jenkins agent with Docker and `kubectl`
- A container registry account (credentials stored in Jenkins)
- A Kubernetes cluster and kubeconfig credentials stored in Jenkins
- An image scanning tool installed on the agent

## How to Run

1. Create a Pipeline job pointing to this repository.
2. Make sure the agent is online.
3. Click **Build Now**.
4. Check the console output for each stage.

## Screenshots

_Add screenshots here: agent online, successful pipeline run, pods running on the cluster._

## Tools Used

Jenkins · Jenkins Shared Libraries · Docker · Kubernetes · Git · Groovy

## Author

**Essam Mostafa**: [GitHub @Essammo22](https://github.com/Essammo22)
