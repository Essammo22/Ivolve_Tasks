# Lab 10: Node Isolation Using Taints and Labels in Kubernetes

## 1. Apply a taint to the worker node

Key `workload`, value `ivolveApp`, effect `NoSchedule`.

```bash
kubectl taint nodes worker1 workload=ivolveApp:NoSchedule
```

Output:

```
node/worker1 tainted
```

## 2. Label the same worker node

```bash
kubectl label nodes worker1 project=ivolve
```

Output:

```
node/worker1 labeled
```

## 3. Verify the taint

```bash
kubectl describe node worker1 | grep -A3 -i taints
```

Output:

```
Taints:             workload=ivolveApp:NoSchedule
```

## 4. Verify the label

```bash
kubectl get nodes --show-labels
```

Output:

```
NAME      STATUS   ROLES   AGE   VERSION   LABELS
worker1   Ready    <none>  ...   ...       ...,project=ivolve
```
