# Lab 13: Persistent Storage Setup for Application Logging

This lab sets up persistent storage in Kubernetes for application logs using a **PersistentVolume (PV)** backed by `hostPath` and a **PersistentVolumeClaim (PVC)** that binds to it.

## Requirements

**PersistentVolume**

| Setting        | Value                          |
|----------------|--------------------------------|
| Name           | `ivolvepv`                     |
| Size           | `1Gi`                          |
| Storage type   | `hostPath`                     |
| Path           | `/mnt/app-logs` (on the node)  |
| Access mode    | `ReadWriteMany`                |
| Reclaim policy | `Retain`                       |

**PersistentVolumeClaim**

- Name: `ivolvepvc`
- Requests `1Gi` of storage
- Access mode matches the PV (`ReadWriteMany`)
- No StorageClass is used

## Manifests

### `pv.yaml`

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: ivolvepv
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  hostPath:
    path: /mnt/app-logs
```

### `pvc.yaml`

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ivolvepvc
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: ""
  resources:
    requests:
      storage: 1Gi
```

> `storageClassName: ""` is required to meet the "no StorageClass" rule. Without it, clusters such as Minikube and kind assign the default `standard` StorageClass, and the PVC would bind to a dynamically created PV instead of `ivolvepv`.

## Usage

```bash
kubectl apply -f pv.yaml
kubectl apply -f pvc.yaml
```

## Verification

```bash
kubectl get pv
kubectl get pvc
kubectl describe pv ivolvepv
```

Expected output:

```
NAME       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM               STORAGECLASS
ivolvepv   1Gi        RWX            Retain           Bound    default/ivolvepvc

NAME        STATUS   VOLUME     CAPACITY   ACCESS MODES   STORAGECLASS
ivolvepvc   Bound    ivolvepv   1Gi        RWX
```

Checklist:

- PVC status is `Bound` and the volume is `ivolvepv`.
- The `STORAGECLASS` column is empty for both resources.
- Reclaim policy is `Retain` and access mode is `RWX`.

## Testing with a Pod (optional)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: log-test
spec:
  containers:
    - name: logger
      image: busybox
      command: ["sh", "-c", "while true; do echo \"$(date) - hello from pod\" >> /var/log/app/app.log; sleep 5; done"]
      volumeMounts:
        - name: logs-vol
          mountPath: /var/log/app
  volumes:
    - name: logs-vol
      persistentVolumeClaim:
        claimName: ivolvepvc
```

```bash
kubectl apply -f pod.yaml
kubectl exec log-test -- cat /var/log/app/app.log
```

To confirm the logs reached the node, open a shell on it (`minikube ssh` or `docker exec -it <node> bash`) and run:

```bash
cat /mnt/app-logs/app.log
```

## Key concepts

- **hostPath** stores data on the node's own file system, so it suits labs and single-node clusters, not production.
- **ReadWriteMany** allows multiple Pods to read and write the volume at the same time.
- **Retain** keeps the data on the node after the PVC is deleted. The PV moves to `Released` and must be cleaned up manually.

## Cleanup

```bash
kubectl delete pod log-test
kubectl delete pvc ivolvepvc
kubectl delete pv ivolvepv
```

The files in `/mnt/app-logs` stay on the node until you remove them manually.
