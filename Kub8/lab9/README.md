# Lab 9: Deploy a Kubernetes Cluster using Kubeadm

A two-node Kubernetes cluster (1 control plane + 1 worker) built from scratch with **kubeadm** on Ubuntu VMs, using **containerd** as the container runtime and **Calico** as the CNI plugin.

Part of the NTI HireReady Cloud DevOps Accelerator program.

## Objectives

- Install and configure containerd on all nodes
- Install kubeadm, kubelet, and kubectl on all nodes
- Initialize the control plane with `kubeadm init`
- Configure kubectl to talk to the cluster
- Install the Calico CNI plugin for pod networking
- Join the worker node with `kubeadm join`
- Verify that all nodes are `Ready` and all system components are running

## Environment

| Item | Control Plane | Worker |
|---|---|---|
| Hostname | `control-plane` | `worker1` |
| IP address | `192.168.40.135` | `192.168.40.136` |
| OS | Ubuntu 22.04.5 LTS | Ubuntu 22.04.5 LTS |
| Kernel | 6.8.0-138-generic | 6.8.0-138-generic |
| vCPU | 2 | 2 |
| RAM | 3.8 GiB | 3.8 GiB |
| Swap | Disabled | Disabled |
| Hypervisor | VMware | VMware |

### Software versions

| Component | Version |
|---|---|
| Kubernetes (kubeadm / kubelet / kubectl) | v1.34.12 |
| containerd | 2.2.1 |
| Calico | `<fill in version>` |

## Architecture

```
            +-----------------------------+
            |   control-plane             |
            |   192.168.40.135            |
            |   kube-apiserver, etcd,     |
            |   scheduler, controller-mgr |
            +--------------+--------------+
                           |  :6443
            +--------------+--------------+
            |   worker1                   |
            |   192.168.40.136            |
            |   kubelet, kube-proxy,      |
            |   calico-node               |
            +-----------------------------+
```

## Steps

### 1. Prepare all nodes

- Map the node names in `/etc/hosts` on **every** node:

  ```
  192.168.40.135 control-plane
  192.168.40.136 worker1
  ```

- Disable swap (kubelet will not run with swap enabled).
- Load the kernel modules `overlay` and `br_netfilter`.
- Set the sysctl values `net.ipv4.ip_forward = 1` and `net.bridge.bridge-nf-call-iptables = 1`.

Verification:

```bash
swapon --show                      # empty output = swap is off
lsmod | grep -E "overlay|br_netfilter"
sysctl net.ipv4.ip_forward net.bridge.bridge-nf-call-iptables
```

### 2. Install and configure containerd (all nodes)

Install containerd, generate the default config, and enable the systemd cgroup driver:

```bash
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml
# set SystemdCgroup = true
sudo systemctl restart containerd
sudo systemctl enable containerd
```

Verification:

```bash
containerd --version
grep SystemdCgroup /etc/containerd/config.toml   # SystemdCgroup = true
systemctl is-active containerd
```

### 3. Install kubeadm, kubelet, kubectl (all nodes)

Add the official Kubernetes apt repository (`pkgs.k8s.io`), install the three packages, and hold them so they are not upgraded automatically:

```bash
sudo apt-mark hold kubelet kubeadm kubectl
```

Verification:

```bash
kubeadm version -o short
kubelet --version
kubectl version --client
```

### 4. Initialize the control plane (control-plane node only)

```bash
sudo kubeadm init --pod-network-cidr=<fill in your pod CIDR>
```

On success, kubeadm prints the `kubeadm join` command. Save it.

### 5. Configure kubectl (control-plane node)

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

Right after `init`, the node shows `NotReady` because no CNI plugin is installed yet. This is expected.

### 6. Install the Calico CNI plugin (control-plane node)

```bash
# <fill in the exact commands you used: Tigera operator + custom resources, or the single manifest>
kubectl apply -f <calico manifest>
```

Wait until the Calico pods are `Running`.

### 7. Join the worker node (worker node)

```bash
sudo kubeadm join 192.168.40.135:6443 \
  --token <TOKEN> \
  --discovery-token-ca-cert-hash sha256:<HASH>
```

> Tokens expire after 24 hours. Generate a new join command from the control plane with:
> `kubeadm token create --print-join-command`

Worker output on success:

```
This node has joined the cluster:
* Certificate signing request was sent to apiserver and a response was received.
* The Kubelet was informed of the new secure connection details.
```

### 8. Verify the cluster (control-plane node)

```bash
kubectl get nodes -o wide
kubectl get pods -A -o wide
kubectl cluster-info
```

#### Nodes

```
NAME            STATUS   ROLES           AGE   VERSION    INTERNAL-IP      OS-IMAGE             CONTAINER-RUNTIME
control-plane   Ready    control-plane   37m   v1.34.12   192.168.40.135   Ubuntu 22.04.5 LTS   containerd://2.2.1
worker1         Ready    <none>          32m   v1.34.12   192.168.40.136   Ubuntu 22.04.5 LTS   containerd://2.2.1
```

#### System pods (`kube-system`)

All of the following are `1/1 Running`:

| Component | Where it runs |
|---|---|
| etcd | control-plane |
| kube-apiserver | control-plane |
| kube-controller-manager | control-plane |
| kube-scheduler | control-plane |
| coredns (x2) | control-plane |
| calico-kube-controllers | control-plane |
| calico-node | control-plane, worker1 |
| kube-proxy | control-plane, worker1 |

#### Cluster info

```
Kubernetes control plane is running at https://192.168.40.135:6443
CoreDNS is running at https://192.168.40.135:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
```

## Troubleshooting notes

**Hostname resolution failure**

- **Symptom:** `ping: worker: Temporary failure in name resolution`
- **Cause:** the node names were not mapped to IPs.
- **Fix:** added both nodes to `/etc/hosts` on every node.

<!-- Add any other problems you hit and how you solved them. -->

## Key takeaways

- The CNI plugin can only be installed after `kubeadm init`, because it is applied to a running cluster. Until then the node stays `NotReady` and CoreDNS stays `Pending`.
- containerd must use the **systemd cgroup driver** (`SystemdCgroup = true`) to match kubelet.
- Swap must be disabled on every node.
- Join tokens are short-lived; regenerate them when needed.

## Screenshots

Final verification from the control-plane node: both nodes are `Ready` and every system pod is `Running`.

![kubectl get nodes and kubectl get pods -A](images/cluster-verification.png)
