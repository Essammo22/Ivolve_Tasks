# Lab 19: Securing Kubernetes with RBAC and Service Accounts

## Overview

This lab demonstrates how to apply the **principle of least privilege** in Kubernetes using **RBAC (Role-Based Access Control)**. A dedicated Service Account is created for Jenkins and is only allowed to **create/update Deployments** inside the `ivolve` namespace, and nothing else.

## Objectives

- Create a Service Account in the `ivolve` namespace
- Generate a token for the Service Account
- Define a Role that grants create/update permissions on Deployments
- Bind the Role to the Service Account using a RoleBinding
- Validate that the permissions are limited to the `ivolve` namespace only

## Resources

| Resource        | Name                    | Namespace |
|-----------------|-------------------------|-----------|
| ServiceAccount  | `jenkinssa`             | ivolve    |
| Role            | `ivolveDeployRole`      | ivolve    |
| RoleBinding     | `ivolveDeployRoleBinding` | ivolve  |

> Note: Kubernetes ServiceAccount names must be lowercase (DNS-1123), so `jenkinsSA` is written as `jenkinssa`.

## Prerequisites

- A running Kubernetes cluster (Minikube / kubeadm / etc.)
- `kubectl` configured to talk to the cluster

## Steps

### 1. Create the namespace (if it doesn't exist)

```bash
kubectl create namespace ivolve
```

### 2. Create the Service Account

```bash
kubectl create serviceaccount jenkinssa -n ivolve
```

### 3. Create a token for the Service Account

```bash
kubectl create token jenkinssa -n ivolve
```

### 4. Define the Role

`role.yaml`

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: ivolveDeployRole
  namespace: ivolve
rules:
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["create", "update"]
```

```bash
kubectl apply -f role.yaml
```

### 5. Create the RoleBinding

`rolebinding.yaml`

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ivolveDeployRoleBinding
  namespace: ivolve
subjects:
  - kind: ServiceAccount
    name: jenkinssa
    namespace: ivolve
roleRef:
  kind: Role
  name: ivolveDeployRole
  apiGroup: rbac.authorization.k8s.io
```

```bash
kubectl apply -f rolebinding.yaml
```

## Validation

Use `kubectl auth can-i` while impersonating the Service Account.

**Allowed in the `ivolve` namespace:**

```bash
kubectl auth can-i create deployments -n ivolve --as=system:serviceaccount:ivolve:jenkinssa   # yes
kubectl auth can-i update deployments -n ivolve --as=system:serviceaccount:ivolve:jenkinssa   # yes
```

**Denied for other actions / namespaces:**

```bash
kubectl auth can-i delete deployments -n ivolve --as=system:serviceaccount:ivolve:jenkinssa   # no
kubectl auth can-i create pods -n ivolve --as=system:serviceaccount:ivolve:jenkinssa          # no
kubectl auth can-i create deployments -n default --as=system:serviceaccount:ivolve:jenkinssa  # no
```

| Check                                   | Expected |
|-----------------------------------------|----------|
| create deployments in `ivolve`          | yes      |
| update deployments in `ivolve`          | yes      |
| delete deployments in `ivolve`          | no       |
| create pods in `ivolve`                 | no       |
| create deployments in `default`         | no       |

## Screenshots

> Add your screenshots here (service account, role, rolebinding, and `auth can-i` results).

## Key Takeaways

- A **Role** is namespaced, so its permissions never leak outside `ivolve`.
- A **RoleBinding** connects the Role to a subject (here, the Service Account).
- Using a dedicated Service Account with minimal permissions is the right way to give CI/CD tools like Jenkins access to a cluster.
