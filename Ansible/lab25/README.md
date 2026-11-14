# Lab 25: Automated Configuration Using Ansible Playbooks

## Overview

This lab automates the installation of Docker, kubectl, and Jenkins on a managed node using a single Ansible playbook, replacing the one-off ad-hoc commands from the previous lab.

## Objectives

- Write an Ansible playbook that installs Docker, kubectl, and Jenkins
- Run the playbook against the managed node
- Verify the configuration on the managed node

## Architecture

```
+----------------+        SSH (key-based)        +------------------+
|  Control Node  | ----------------------------> |   Managed Node   |
|  (Ansible)     |   ansible-playbook            |  Docker          |
+----------------+                               |  kubectl         |
                                                 |  Jenkins         |
                                                 +------------------+
```

## Prerequisites

- Completed Lab 24 (Ansible installed, passwordless SSH to the managed node)
- Inventory file `inventory.ini`:

```ini
[managed]
node1 ansible_host=192.168.40.136 ansible_user=worker
```

- The managed node has internet access and at least 2 GB of RAM (Jenkins requirement)

## Project Structure

```
lab25/
├── inventory.ini
└── install_tools.yml
```

## The Playbook

`install_tools.yml`:

```yaml
---
- name: Install Docker, Kubernetes (kubectl) and Jenkins
  hosts: managed
  become: yes
  tasks:
    - name: Install Docker
      apt:
        name: docker.io
        state: present
        update_cache: yes

    - name: Install prerequisites
      apt:
        name:
          - ca-certificates
          - curl
          - gnupg
          - fontconfig
          - openjdk-21-jre
        state: present
        update_cache: yes

    - name: Create keyrings directory
      file:
        path: /etc/apt/keyrings
        state: directory
        mode: '0755'

    - name: Download Kubernetes repository key
      get_url:
        url: https://pkgs.k8s.io/core:/stable:/v1.37/deb/Release.key
        dest: /etc/apt/keyrings/kubernetes-apt-keyring.asc
        mode: '0644'

    - name: Add Kubernetes repository
      apt_repository:
        repo: "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.asc] https://pkgs.k8s.io/core:/stable:/v1.37/deb/ /"
        state: present
        filename: kubernetes

    - name: Install kubectl
      apt:
        name: kubectl
        state: present
        update_cache: yes

    - name: Download Jenkins repository key
      get_url:
        url: https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key
        dest: /etc/apt/keyrings/jenkins-keyring.asc
        mode: '0644'

    - name: Add Jenkins repository
      apt_repository:
        repo: "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/"
        state: present
        filename: jenkins

    - name: Install Jenkins
      apt:
        name: jenkins
        state: present
        update_cache: yes
```

### What each section does

| Section | Purpose |
|---------|---------|
| `become: yes` | Runs all tasks with sudo privileges |
| Docker task | Installs Docker from the Ubuntu repositories |
| Prerequisites | Installs the tools needed to add external repositories, plus Java 21 (required by Jenkins) |
| Keyrings directory | Creates `/etc/apt/keyrings` to store repository signing keys |
| Kubernetes tasks | Download the signing key, add the official Kubernetes apt repository, install `kubectl` |
| Jenkins tasks | Download the signing key, add the official Jenkins apt repository, install Jenkins |

The Docker and Jenkins packages start their services automatically after installation.

## Running the Playbook

Check the syntax first:

```bash
ansible-playbook -i inventory.ini install_tools.yml --syntax-check
```

Run it (`-K` prompts for the sudo password on the managed node):

```bash
ansible-playbook -i inventory.ini install_tools.yml -K
```

A successful run ends with a recap like:

```
PLAY RECAP *********************************************************
node1 : ok=11   changed=10   unreachable=0   failed=0   skipped=0
```

## Verification

On the managed node (via Ansible):

```bash
ansible managed -i inventory.ini -a "docker --version"
ansible managed -i inventory.ini -a "kubectl version --client"
ansible managed -i inventory.ini -a "systemctl is-active docker" --become -K
ansible managed -i inventory.ini -a "systemctl is-active jenkins" --become -K
```

Or directly on the managed node:

```bash
docker --version
kubectl version --client
systemctl status jenkins
```

Jenkins web interface: `http://192.168.40.136:8080`

To get the initial admin password:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### Idempotency check

Run the playbook a second time. Tasks that are already satisfied should report `ok` instead of `changed`, which shows the playbook is idempotent.

## Key Concepts

- **Playbook**: a YAML file describing the desired state of managed nodes
- **Play**: a set of tasks mapped to a group of hosts
- **Task**: a single action that calls an Ansible module
- **Idempotency**: running the same playbook repeatedly produces the same end state without unnecessary changes
- **Signed apt repositories**: external repositories are trusted through a downloaded GPG key referenced with `signed-by`

## Author

Essam Mostafa - [GitHub: Essammo22](https://github.com/Essammo22)
