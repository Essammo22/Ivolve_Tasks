# Lab 17: Control Pod-to-Pod Traffic via Network Policy

## Objective

Restrict network access to the MySQL pod using a Kubernetes **NetworkPolicy**, so that only the application pods can connect to the database, and only on the MySQL port (3306).

## Specifications

| Item | Value |
|------|-------|
| Name | `ivolvenp` (Kubernetes names must be lowercase, so `ivolveNP` becomes `ivolvenp`) |
| Namespace | `ivolve` |
| Pod Selector | Targets the MySQL pod (`app: ivolvedb`) |
| Policy Types | Ingress only |
| Ingress Rule | Allow traffic only from application pods (`app: ivolveapp`) |
| Port | 3306 (MySQL default port) |

## Architecture

```
app pod (app=ivolveapp)  --->  ivolvedbsvc (Service)  --->  mysql pod (app=ivolvedb)
                                                                  ^
                                                      NetworkPolicy enforced here
```

- The database runs as a **StatefulSet** exposed through the `ivolvedbsvc` Service.
- The application pods carry the label `app=ivolveapp`.
- Any other pod in the namespace is blocked from reaching the database.

## NetworkPolicy Manifest

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ivolvenp
  namespace: ivolve
spec:
  podSelector:
    matchLabels:
      app: ivolvedb
  policyTypes:
    - Ingress
  ingress:
    - from:
      - podSelector:
          matchLabels:
            app: ivolveapp
      ports:
        - port: 3306
```

### How it works

- `podSelector` selects the pods the policy protects (the MySQL pod).
- `policyTypes: Ingress` controls incoming traffic only. Outgoing traffic is not affected.
- The `ingress` rule allows connections only from pods labeled `app=ivolveapp`, and only to port `3306`.
- Once a pod is selected by an Ingress policy, any traffic that is not explicitly allowed is denied.

> **Note:** NetworkPolicies are only enforced if the cluster uses a CNI plugin that supports them (for example Calico or Cilium).

## Steps

### 1. Apply the policy

```bash
kubectl apply -f networkpolicy.yaml
```

### 2. Verify the policy

```bash
kubectl get networkpolicy -n ivolve
kubectl describe networkpolicy ivolvenp -n ivolve
```

**Output:**

```
$ kubectl get networkpolicy -n ivolve
NAME       POD-SELECTOR   AGE
ivolvenp   app=ivolvedb   35m

$ kubectl describe networkpolicy ivolvenp -n ivolve
Name:         ivolvenp
Namespace:    ivolve
Created on:   2026-11-03 02:59:05 +0200 EET
Labels:       <none>
Annotations:  <none>
Spec:
  PodSelector:     app=ivolvedb
  Allowing ingress traffic:
    To Port: 3306/TCP
    From:
      PodSelector: app=ivolveapp
  Not affecting egress traffic
  Policy Types: Ingress
```

### 3. Positive test: connect from the application pod

```bash
kubectl exec -it -n ivolve $(kubectl get pods -n ivolve -l app=ivolveapp -o jsonpath="{.items[0].metadata.name}") -c myapp -- nc -zv -w 3 ivolvedbsvc 3306
```

Expected: the connection succeeds (`open`).

**Output:**

```
ivolvedbsvc (192.168.235.219:3306) open
```

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| `nc: not found` | The image has no netcat | Use `nicolaka/netshoot` or another image that includes it |
| `container not found` | Wrong container name (the app container here is `myapp`) | Check with `kubectl get pod <pod> -n ivolve -o jsonpath="{.spec.containers[*].name}"` |
| Blocked pod can still connect | The CNI plugin does not enforce NetworkPolicies | Use a CNI that supports them (Calico, Cilium) |

## Key Takeaways

- A NetworkPolicy decides **who can talk to whom** based on pod labels, not on the node a pod runs on.
- Taints and tolerations decide **where a pod can be scheduled**. They are a separate mechanism.
- Once an Ingress policy selects a pod, everything not explicitly allowed is denied.
