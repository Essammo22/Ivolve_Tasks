# Lab 11: Namespace Management and Resource Quota Enforcement

## 1. Create the namespace `ivolve`

```bash
kubectl create namespace ivolve
```

Output:

```
namespace/ivolve created
```

## 2. Create the ResourceQuota `ivolvequota` (limit: 3 pods)

```bash
kubectl create quota ivolvequota --hard=pods=3 -n ivolve
```

Output:

```
resourcequota/ivolvequota created
```

## 3. Verify the ResourceQuota

```bash
kubectl get resourcequota -n ivolve
kubectl describe resourcequota ivolvequota -n ivolve
```

Output:

```
NAME          AGE   REQUEST     LIMIT
ivolvequota   10s   pods: 0/3

Name:       ivolvequota
Namespace:  ivolve
Resource    Used  Hard
--------    ----  ----
pods        0     3
```

## 4. Confirm Kubernetes blocks extra pods

Create three pods (the limit):

```bash
kubectl run pod1 --image=nginx -n ivolve
kubectl run pod2 --image=nginx -n ivolve
kubectl run pod3 --image=nginx -n ivolve
```

Output:

```
pod/pod1 created
pod/pod2 created
pod/pod3 created
```

Try to create a fourth pod:

```bash
kubectl run pod4 --image=nginx -n ivolve
```

Output:

```
Error from server (Forbidden): pods "pod4" is forbidden: exceeded quota: ivolvequota, requested: pods=1, used: pods=3, limited: pods=3
```

Quota usage after reaching the limit:

```bash
kubectl describe resourcequota ivolvequota -n ivolve
```

```
Resource    Used  Hard
--------    ----  ----
pods        3     3
```
