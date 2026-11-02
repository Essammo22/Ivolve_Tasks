# Lab 16: Pod Resource Management with CPU and Memory Requests and Limits

## Objective

Update the existing `ivolveApp` Deployment to define CPU and memory **requests** and **limits**, verify them with `kubectl describe pod`, and monitor real-time usage with `kubectl top pod` (which requires metrics-server).

| Resource | Request | Limit |
|----------|---------|-------|
| CPU      | 1 vCPU  | 2 vCPU |
| Memory   | 1Gi     | 2Gi    |

## Environment

- Kubernetes v1.34 cluster built with kubeadm (1 control-plane + 1 worker, `worker1`)
- CNI: Calico
- Namespace: `ivolve`
- `worker1` has the label `project=ivolve` and the taint `workload=ivolveApp:NoSchedule`; the Deployment uses a matching `nodeSelector` and `toleration`
- Each node has 2 vCPUs (increased on `worker1` to run 2 replicas, see Troubleshooting)

## Implementation

### 1. Add resources to the Deployment

In the container spec of the `ivolveApp` Deployment:

```yaml
resources:
  requests:
    cpu: "1"
    memory: 1Gi
  limits:
    cpu: "2"
    memory: 2Gi
```

Apply it:

```bash
kubectl apply -f <deployment-file>.yaml
kubectl rollout status deployment/ivolveapp -n ivolve
```

Full manifest (`ivolve-app.yaml`):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ivolveapp
  namespace: ivolve
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 0
      maxUnavailable: 1
  selector:
    matchLabels:
      app: ivolveapp
  template:
    metadata:
      labels:
        app: ivolveapp
    spec:
      nodeSelector:
        project: ivolve
      tolerations:
        - key: workload
          operator: Equal
          value: ivolveApp
          effect: NoSchedule
      initContainers:
        - name: init-db
          image: mysql:latest
          env:
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: ivolvecm
                  key: DB_HOST
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: ivolvesecret
                  key: MYSQL_ROOT_PASSWORD
            - name: DB_USER
              valueFrom:
                configMapKeyRef:
                  name: ivolvecm
                  key: DB_USER
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: ivolvesecret
                  key: DB_PASSWORD
          command:
            - "sh"
            - "-c"
            - |
              until mysql -h "$DB_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; do
                echo "Waiting for MySQL at $DB_HOST to be ready..."
                sleep 3
              done
              mysql -h "$DB_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS ivolve; CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$DB_PASSWORD'; GRANT ALL PRIVILEGES ON ivolve.* TO '$DB_USER'@'%'; FLUSH PRIVILEGES;"
              echo "Database ivolve initialized and privileges granted to $DB_USER."
      containers:
        - name: myapp
          image: essammo22/ivolve-app:v1
          imagePullPolicy: Always
          resources:
            requests:
              cpu: "1"
              memory: "1Gi"
            limits:
              cpu: "2"
              memory: "2Gi"
          ports:
            - containerPort: 3000
          env:
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: ivolvecm
                  key: DB_HOST
            - name: DB_USER
              valueFrom:
                configMapKeyRef:
                  name: ivolvecm
                  key: DB_USER
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: ivolvesecret
                  key: DB_PASSWORD
          volumeMounts:
            - name: app-volume
              mountPath: /app/logs
      volumes:
        - name: app-volume
          persistentVolumeClaim:
            claimName: ivolvepvc
```

Credentials are not stored in the manifest; they come from the `ivolvecm` ConfigMap and the `ivolvesecret` Secret.

### 2. Verify requests and limits

```bash
kubectl describe pod <pod-name> -n ivolve
```

Output (excerpt) for the `myapp` container:

```
Containers:
  myapp:
    Image:          essammo22/ivolve-app:v1
    State:          Running
    Ready:          True
    Limits:
      cpu:     2
      memory:  2Gi
    Requests:
      cpu:     1
      memory:  1Gi
...
QoS Class:       Burstable
Node-Selectors:  project=ivolve
Tolerations:     workload=ivolveApp:NoSchedule
```

The Pod is `Running` on `worker1`, and its QoS class is `Burstable` because requests are lower than limits.

<!-- TODO: add screenshot of describe pod -->

### 3. Install metrics-server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

On this cluster two changes were required in the metrics-server Deployment:

- `--kubelet-insecure-tls` added to the container `args` (kubeadm kubelet certificates are self-signed; lab use only)
- A toleration so it can run on the control-plane node, because both nodes are tainted:

```yaml
tolerations:
- key: node-role.kubernetes.io/control-plane
  operator: Exists
  effect: NoSchedule
```

### 4. Monitor real-time usage

```bash
kubectl top pod -n ivolve
kubectl top node
```

Result:

```
NAME                         CPU(cores)   MEMORY(bytes)
ivolveapp-8648b4f5db-9dlrc   1m           20Mi
ivolveapp-8648b4f5db-b88vj   1m           20Mi
ivolvedb-0                   5m           464Mi

NAME            CPU(cores)   CPU(%)   MEMORY(bytes)   MEMORY(%)
control-plane   199m         9%       2503Mi          66%
worker1         66m          1%       2270Mi          60%
```

<!-- TODO: add screenshot of top pod / top node -->

## Troubleshooting and lessons learned

### Pods stuck in Pending (Insufficient cpu)

Events showed: `0/2 nodes are available: 1 Insufficient cpu, 1 node(s) had untolerated taint(s)`.

- The scheduler places Pods based on **requests**, not actual usage.
- `worker1` (2 vCPU) already had ~250m reserved by `calico-node`, so only one Pod requesting 1 CPU fits.
- The control-plane node was excluded by its default taint.
- A `rollout restart` made it worse: with the default RollingUpdate (`maxSurge: 25%`), the new Pod must be scheduled before the old one is removed, so the rollout hung.

Fixes applied:

- Ran with `replicas: 1` and set the update strategy so the old Pod is removed first:

  ```yaml
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 0
      maxUnavailable: 1
  ```

- To run 2 replicas, `worker1` needs more CPU: 2 replicas x 1 CPU + 250m (calico) = 2250m. <!-- TODO: write how you increased CPU (e.g. 4 vCPU on the VM) -->

### `kubectl top` returned "Metrics API not available"

1. metrics-server Pods were `Pending`: both nodes had untolerated taints, fixed with the toleration above.
2. The APIService showed `False (MissingEndpoints)`: the Service selector did not match any Pod. metrics-server had previously been installed with **Helm**, then `components.yaml` was applied on top, mixing two different label schemes.
3. Fix: `helm uninstall metrics-server -n kube-system`, deleted and re-applied the official manifest (single source of truth), then re-added the toleration and `--kubelet-insecure-tls`.

### Key takeaways

- Requests affect **scheduling**; limits affect **runtime** (CPU throttling, OOM kill).
- Actual usage here (~1m CPU, ~20Mi memory) is far below the requests, so the requests reserve capacity the app does not use. The values follow the lab requirements; in a real environment they would be tuned from observed usage.
- Do not mix installation methods (Helm and raw manifests) for the same component.
- Read the `Events` section of `kubectl describe pod` first; it usually states the scheduling problem directly.
