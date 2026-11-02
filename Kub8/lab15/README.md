# Lab 15: Node.js Application Deployment with ClusterIP Service

Deploying a containerized Node.js application on Kubernetes that connects to a MySQL database. The Deployment uses an init container to prepare the database, reads its configuration from a ConfigMap and a Secret, and is exposed internally through a ClusterIP Service.

## Objectives

- Create a Deployment named `ivolveapp` in the `ivolve` namespace with **2 replicas**
- Use the custom Docker image from Docker Hub created in Lab 8
- Consume environment variables from the existing **ConfigMap** and **Secret**
- Schedule pods only on nodes labeled `project=ivolve` using a **nodeSelector**
- Add a **toleration** (`workload=ivolveApp:NoSchedule`)
- Mount the **PersistentVolumeClaim** created in Lab 13
- Add an **init container** (MySQL client) that creates the `ivolve` database and grants privileges to the application user
- Create a **ClusterIP Service** to load-balance traffic across the application pods
- Verify the database, user privileges, and application logs

## Architecture

```
                 ┌────────────────────────────┐
                 │  ClusterIP Service         │
                 │  ivolvaappsvc : 3000       │
                 └─────────────┬──────────────┘
                               │ (load balancing)
              ┌────────────────┴────────────────┐
              ▼                                 ▼
     ┌─────────────────┐               ┌─────────────────┐
     │ ivolveapp pod 1 │               │ ivolveapp pod 2 │
     │  init-db  ──┐   │               │  init-db  ──┐   │
     │  myapp      │   │               │  myapp      │   │
     └─────────────┼───┘               └─────────────┼───┘
                   └──────────────┬──────────────────┘
                                  ▼
                        ┌──────────────────┐
                        │ MySQL (ivolvedb) │
                        │   StatefulSet    │
                        └──────────────────┘
```

## Resources Used

| Resource | Name | Notes |
|---|---|---|
| Namespace | `ivolve` | |
| Deployment | `ivolveapp` | 2 replicas |
| Service | `ivolvaappsvc` | Type `ClusterIP`, port `3000` |
| ConfigMap | `ivolvecm` | Provides `DB_HOST`, `DB_USER` |
| Secret | `ivolvesecret` | Provides `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` |
| PVC | `ivolvepvc` | From Lab 13, mounted at `/app/logs` |
| App image | `essammo22/ivolve-app:v1` | From Lab 8 |
| Init image | `mysql:latest` | MySQL client |

> Kubernetes resource names must be lowercase, so `ivolveApp` and `ivolvaAppSvc` from the task are written as `ivolveapp` and `ivolvaappsvc`.

## Implementation

### 1. Deployment (`deployment.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ivolveapp
  namespace: ivolve
spec:
  replicas: 2
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

**How it works**

- **Init container (`init-db`)**: first loops until MySQL accepts connections, then creates the `ivolve` database, creates the application user, and grants it all privileges on `ivolve.*`. It uses `IF NOT EXISTS`, so it is safe to re-run every time a pod starts.
- **Configuration**: non-sensitive values (`DB_HOST`, `DB_USER`) come from the ConfigMap; sensitive values (`DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`) come from the Secret.
- **Scheduling**: `nodeSelector` restricts pods to nodes labeled `project=ivolve`, and the toleration allows them onto nodes tainted `workload=ivolveApp:NoSchedule`.
- **Storage**: the PVC from Lab 13 is mounted at `/app/logs`.

### 2. Service (`service.yaml`)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: ivolvaappsvc
  namespace: ivolve
  labels:
    app: ivolveapp
spec:
  type: ClusterIP
  ports:
    - port: 3000
      targetPort: 3000
  selector:
    app: ivolveapp
```

The Service selects pods with the label `app=ivolveapp` and distributes traffic across them inside the cluster.

### 3. Apply

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

## Verification

### 1. Pods are running

```bash
kubectl get pod -n ivolve
```

```
NAME                        READY   STATUS    RESTARTS      AGE
ivolveapp-8f9d56997-d7xwl   1/1     Running   0             45m
ivolveapp-8f9d56997-x8wzp   1/1     Running   0             44m
ivolvedb-0                  1/1     Running   1 (11h ago)   2d
```

Both application replicas are `Running`, along with the MySQL pod `ivolvedb-0`.

### 2. Database and privileges (manual check in MySQL)

```bash
kubectl exec -it ivolvedb-0 -n ivolve -- mysql -u root -p<ROOT_PASSWORD> \
  -e "SHOW DATABASES; SHOW GRANTS FOR 'ivolve_user'@'%';"
```

```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| ivolve             |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
+---------------------------------------------------------+
| Grants for ivolve_user@%                                |
+---------------------------------------------------------+
| GRANT USAGE ON *.* TO `ivolve_user`@`%`                 |
| GRANT ALL PRIVILEGES ON `ivolve`.* TO `ivolve_user`@`%` |
+---------------------------------------------------------+
```

- The `ivolve` database was created by the init container.
- `ivolve_user` has `ALL PRIVILEGES` on `ivolve.*`.

### 3. Application logs

```bash
kubectl logs -l app=ivolveapp -c myapp -n ivolve --tail=10
```

```
🔄 Attempting to connect to MySQL...
✅ Connected to MySQL and 'ivolve' DB found.
🚀 Server started on http://0.0.0.0:3000
🔄 Attempting to connect to MySQL...
✅ Connected to MySQL and 'ivolve' DB found.
🚀 Server started on http://0.0.0.0:3000
```

Both replicas connect to MySQL, find the `ivolve` database, and start the server on port 3000.

### 4. Service and endpoints

```bash
kubectl get svc -n ivolve
```

```
NAME           TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
ivolvaappsvc   ClusterIP   10.104.215.147   <none>        3000/TCP   9h
ivolvedbsvc    ClusterIP   None             <none>        3306/TCP   2d1h
```

```bash
kubectl get endpoints -n ivolve
```

```
NAME           ENDPOINTS                                   AGE
ivolvaappsvc   192.168.235.206:3000,192.168.235.207:3000   9h
ivolvedbsvc    192.168.235.196:3306                        2d1h
```

- `ivolvaappsvc` is of type `ClusterIP` and exposes port `3000` inside the cluster only (no external IP).
- The Service has **two endpoints**, one per application pod, which confirms traffic is distributed across both replicas.
- `ivolvedbsvc` is the existing MySQL service, listening on port `3306`. It has `ClusterIP: None`, which means it is a headless service.

## Key Concepts Practiced

- Deployments with multiple replicas
- Init containers for database bootstrapping
- ConfigMaps and Secrets as environment variables
- Node selectors and tolerations for scheduling control
- PersistentVolumeClaims mounted into pods
- ClusterIP Services for internal load balancing
- Verifying a deployment through pod status, database checks, and logs

## Possible Improvements

- Pin the init container image to a specific MySQL version instead of `mysql:latest`
- Add readiness and liveness probes to the application container
- Use a dedicated least-privilege account in the init container instead of root where possible
