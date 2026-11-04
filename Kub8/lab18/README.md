# Lab 18: Node-Wide Pod Management with DaemonSet

A complete Kubernetes monitoring stack built from plain YAML manifests. A **DaemonSet** runs Prometheus `node-exporter` on every node, **Prometheus** scrapes the metrics, **Alertmanager** handles alerts, and **Grafana** visualizes everything.

Part of the NTI HireReady Cloud DevOps Accelerator program.

---

## Architecture

```
 ┌──────────────────────── Kubernetes Cluster ────────────────────────┐
 │                                                                    │
 │   Node 1              Node 2              Node N                   │
 │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
 │  │ node-exporter│    │ node-exporter│    │ node-exporter│  :9100   │
 │  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘          │
 │         └───────────────────┼───────────────────┘                  │
 │                             │ scrape (every 15s)                   │
 │                      ┌──────▼───────┐        ┌───────────────┐     │
 │                      │  Prometheus  │───────▶│ Alertmanager  │     │
 │                      └──────┬───────┘ alerts └───────────────┘     │
 │                             │ query                                │
 │                      ┌──────▼───────┐                              │
 │                      │   Grafana    │                              │
 │                      └──────────────┘                              │
 │                      namespace: monitoring                         │
 └────────────────────────────────────────────────────────────────────┘
```

## Components

| Component | Kind | Purpose | Port |
|-----------|------|---------|------|
| node-exporter | DaemonSet + headless Service | Exposes node-level metrics (CPU, memory, disk, network) | 9100 |
| Prometheus | Deployment + ConfigMap + Service | Collects and stores metrics | 9090 (NodePort 30090) |
| Alertmanager | Deployment + ConfigMap + Service | Routes alerts sent by Prometheus | 9093 (NodePort 30093) |
| Grafana | Deployment + Service | Dashboards and visualization | 3000 (NodePort 30300) |

## Repository Structure

```
lab18/
├── 00-namespace.yaml               # monitoring namespace
├── 01-node-exporter-daemonset.yaml # DaemonSet + headless Service
├── 02-prometheus.yaml              # RBAC + ConfigMap + Deployment + Service
├── 03-alertmanager.yaml            # ConfigMap + Deployment + Service
├── 04-grafana.yaml                 # Deployment + Service
└── README.md
```

## Prerequisites

- A running Kubernetes cluster (minikube, kind, or a real cluster) with 1 or more nodes
- `kubectl` configured to talk to the cluster

## Deployment

Apply the manifests in order:

```bash
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-node-exporter-daemonset.yaml
kubectl apply -f 02-prometheus.yaml
kubectl apply -f 03-alertmanager.yaml
kubectl apply -f 04-grafana.yaml
```

Check that everything is running:

```bash
kubectl get all -n monitoring
```

## Lab Tasks and Validation

| # | Task | How it is done / verified |
|---|------|---------------------------|
| 1 | Create `monitoring` namespace | `kubectl get ns monitoring` shows `Active` |
| 2 | Deploy Prometheus | `02-prometheus.yaml` |
| 3 | Deploy Alertmanager | `03-alertmanager.yaml` |
| 4 | Deploy Grafana | `04-grafana.yaml` |
| 5 | Deploy node-exporter DaemonSet | `01-node-exporter-daemonset.yaml` |
| 6 | Verify one pod per node | See below |
| 7 | Verify metrics endpoint | See below |
| 8 | Configure Prometheus to scrape node-exporter | `scrape_configs` in the Prometheus ConfigMap |
| 9 | Configure Grafana dashboard | See below |

### Task 6: One node-exporter pod per node

```bash
kubectl get nodes
kubectl get pods -n monitoring -l app=node-exporter -o wide
```

The number of pods must equal the number of nodes, and the `NODE` column must show a different node for each pod.

### Task 7: Metrics exposure

```bash
curl http://<node_IP>:9100/metrics | head
```

On minikube, get the IP with `minikube ip`. You should see lines such as `node_cpu_seconds_total` and `node_memory_MemAvailable_bytes`.

### Task 8: Prometheus scraping node-exporter

Open `http://<node_IP>:30090/targets`. The `node-exporter` job must show every node as **UP**.

### Task 9: Grafana dashboard

1. Open `http://<node_IP>:30300` (default login: `admin` / `admin`).
2. Go to **Connections > Data sources > Add data source > Prometheus**.
3. Set the URL to `http://prometheus:9090` and click **Save & test**.
4. Go to **Dashboards > New > Import**, enter ID `1860` (Node Exporter Full), choose the Prometheus data source, and click **Import**.

## Key Concepts

- **DaemonSet**: guarantees exactly one pod on every node. When a new node joins, its pod is created automatically.
- **`hostNetwork: true` and `hostPort: 9100`**: expose the exporter on the node's own IP so `<node_IP>:9100/metrics` works.
- **`tolerations: operator: Exists`**: lets the DaemonSet pod run on tainted nodes such as the control plane.
- **Headless Service (`clusterIP: None`)**: lets Prometheus discover each node-exporter pod individually.
- **`kubernetes_sd_configs`**: Prometheus discovers scrape targets through the Kubernetes API, so new nodes are picked up automatically.
- **RBAC (ServiceAccount, ClusterRole, ClusterRoleBinding)**: gives Prometheus permission to list pods, services, and endpoints for service discovery.

## Troubleshooting

| Problem | Likely cause and fix |
|---------|----------------------|
| Fewer node-exporter pods than nodes | Missing tolerations for tainted nodes |
| `:9100/metrics` not reachable | Check `hostNetwork`/`hostPort`, and that the node firewall allows port 9100 |
| Target shows DOWN in Prometheus | Check pod status and the `relabel_configs` service name match |
| Prometheus cannot discover targets | Check the ClusterRole and ClusterRoleBinding |
| NodePort not reachable on kind | Use `kubectl port-forward -n monitoring svc/grafana 3000:3000` |

## Cleanup

```bash
kubectl delete namespace monitoring
kubectl delete clusterrole prometheus
kubectl delete clusterrolebinding prometheus
```

## Author

**Essam Mostafa**, GitHub: [Essammo22](https://github.com/Essammo22)
