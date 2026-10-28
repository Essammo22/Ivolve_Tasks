# Lab 12: Managing Configuration and Sensitive Data with ConfigMaps and Secrets

> Resource names are lowercase because Kubernetes does not accept uppercase letters in object names.

## 1. Create the ConfigMap `ivolvecm` in the `ivolve` namespace

Stores the non-sensitive MySQL configuration: `DB_HOST` and `DB_USER`.

Generate the manifest with kubectl (without creating anything yet):

```bash
kubectl create configmap ivolvecm -n ivolve --from-literal=DB_HOST=db --from-literal=DB_USER=ivolve_user --dry-run=client -o yaml > configmap.yaml
ls
```

Output:

```
configmap.yaml
```

Validate the manifest against the API server (nothing is saved):

```bash
kubectl apply --dry-run=server -f configmap.yaml
```

Output:

```
configmap/ivolvecm created (server dry run)
```

Apply it:

```bash
kubectl apply -f configmap.yaml
```

Output:

```
configmap/ivolvecm created
```

## 2. Create the Secret `ivolvesecret` in the `ivolve` namespace

Stores the sensitive MySQL credentials: `MYSQL_ROOT_PASSWORD` and `DB_PASSWORD`.

```bash
kubectl create secret generic ivolvesecret --from-literal=MYSQL_ROOT_PASSWORD=123 --from-literal=DB_PASSWORD=admin123 -n ivolve
```

Output:

```
secret/ivolvesecret created
```

> `kubectl create secret` Base64-encodes the values automatically.

## 3. Verify the ConfigMap

```bash
kubectl get configmap ivolvecm -n ivolve
```

Output:

```
NAME       DATA   AGE
ivolvecm   2      4m46s
```

## 4. Verify the Secret

```bash
kubectl get secret ivolvesecret -n ivolve
```

Output:

```
NAME           TYPE     DATA   AGE
ivolvesecret   Opaque   2      27s
```
