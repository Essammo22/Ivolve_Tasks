# Lab 24: Initial Ansible Configuration and Ad-Hoc Execution

## Overview

This lab sets up Ansible on a control node, establishes passwordless SSH to a managed node, creates an inventory, and uses an ad-hoc command to install Docker remotely.

## Objectives

- Install and configure Ansible on the control node
- Generate a new SSH key on the control node
- Transfer the public key to the managed node using `ssh-copy-id`
- Create an inventory for the managed node
- Run an ad-hoc command to install Docker

## Architecture

```
+----------------+        SSH (key-based)        +----------------+
|  Control Node  | ----------------------------> |  Managed Node  |
|  (Ansible)     |                               |  (Ubuntu)      |
+----------------+                               +----------------+
```

## Prerequisites

- Two Ubuntu machines (control node and managed node) that can reach each other over the network
- A user with `sudo` privileges on both machines
- SSH server installed and running on the managed node

## Steps

### 1. Install Ansible on the control node

```bash
sudo apt update
sudo apt install -y ansible
ansible --version
```

### 2. Generate a new SSH key

```bash
ssh-keygen -t ed25519 -C "ansible-control"
```

### 3. Copy the public key to the managed node

```bash
ssh-copy-id worker@192.168.40.136
```

Verify passwordless login:

```bash
ssh worker@192.168.40.136
```

### 4. Create the inventory

Create `inventory.ini`:

```ini
[managed]
node1 ansible_host=192.168.40.136 ansible_user=worker
```

Test connectivity:

```bash
ansible managed -i inventory.ini -m ping
```

Expected output:

```
node1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

### 5. Install Docker with an ad-hoc command

```bash
ansible managed -i inventory.ini -m apt \
  -a "name=docker.io state=present update_cache=yes" --become -K
```

`-K` prompts for the sudo password on the managed node.

### 6. Verify

```bash
ansible managed -i inventory.ini -a "docker --version"
```

## Troubleshooting

### `Missing sudo password`

The remote user needs a password for `sudo`, and Ansible was not given one.

- Add `-K` (`--ask-become-pass`) to the command, or
- Configure passwordless sudo on the managed node:

```bash
echo "worker ALL=(ALL) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/ansible
sudo chmod 440 /etc/sudoers.d/ansible
```

### `to use the 'ssh' connection type with passwords, you must install the sshpass program`

This appears when using `-k` (lowercase), which asks for an **SSH** password. Since key-based authentication is already configured, `-k` is not needed. Use `-K` (uppercase) for the **sudo** password instead.

| Flag | Meaning |
|------|---------|
| `-k` | Ask for the SSH password (requires `sshpass`) |
| `-K` | Ask for the sudo (become) password |

## Key Concepts

- **Control node**: the machine where Ansible runs
- **Managed node**: the machine being configured
- **Inventory**: a file listing the managed hosts and their connection details
- **Ad-hoc command**: a one-off Ansible command run without writing a playbook
- **Module**: a unit of work Ansible executes (e.g. `ping`, `apt`)

## Notes

- Passwordless sudo is acceptable for lab environments. In production, restrict it to specific commands or manage credentials with Ansible Vault.

## Author

Essam Mostafa - [GitHub: Essammo22](https://github.com/Essammo22)
