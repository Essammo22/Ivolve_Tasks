# Lab 26: Structured Configuration Management with Ansible Roles

## Overview

This lab refactors the single playbook from Lab 25 into reusable Ansible roles, one for each tool (Docker, kubectl, Jenkins), and uses a small playbook to run them.

## Objectives

- Create separate Ansible roles for Docker, kubectl, and Jenkins
- Write an Ansible playbook that runs the created roles

## Why Roles?

| Single playbook (Lab 25) | Roles (Lab 26) |
|--------------------------|----------------|
| All tasks in one long file | Each tool has its own folder |
| Hard to reuse in other projects | A role can be reused by any playbook |
| Harder to read and maintain as it grows | Standard, predictable structure |

## Architecture

```
+----------------+        SSH (key-based)        +------------------+
|  Control Node  | ----------------------------> |   Managed Node   |
|  site.yml      |   ansible-playbook            |  Docker          |
|  roles/        |                               |  kubectl         |
+----------------+                               |  Jenkins         |
                                                 +------------------+
```

## Prerequisites

- Completed Lab 24 and Lab 25 (Ansible installed, passwordless SSH to the managed node)
- Inventory file `inventory.ini`:

```ini
[managed]
node1 ansible_host=192.168.40.136 ansible_user=worker
```

## Project Structure

```
lab26/
├── inventory.ini
├── site.yml
└── roles/
    ├── docker/
    │   └── tasks/main.yml
    ├── kubectl/
    │   └── tasks/main.yml
    └── jenkins/
        └── tasks/main.yml
```

> Ansible looks for roles in a folder named `roles` next to the playbook, so the folder name matters.

## Steps

### 1. Create the roles

```bash
mkdir roles
ansible-galaxy init roles/docker
ansible-galaxy init roles/kubectl
ansible-galaxy init roles/jenkins
```

`ansible-galaxy init` generates the standard role skeleton (`tasks`, `handlers`, `defaults`, `meta`, ...). Only `tasks/main.yml` is used in this lab.

### 2. Docker role

`roles/docker/tasks/main.yml`:

```yaml
---
- name: Install Docker
  apt:
    name: docker.io
    state: present
    update_cache: yes
```

### 3. kubectl role

`roles/kubectl/tasks/main.yml`:

```yaml
---
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
```

### 4. Jenkins role

`roles/jenkins/tasks/main.yml`:

```yaml
---
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

### 5. The playbook

`site.yml`:

```yaml
---
- name: Configure managed nodes using roles
  hosts: managed
  become: yes
  roles:
    - docker
    - kubectl
    - jenkins
```

## Running the Playbook

Check the syntax first:

```bash
ansible-playbook -i inventory.ini site.yml --syntax-check
```

Run it (`-K` prompts for the sudo password on the managed node):

```bash
ansible-playbook -i inventory.ini site.yml -K
```

## Verification

```bash
ansible managed -i inventory.ini -a "docker --version"
ansible managed -i inventory.ini -a "kubectl version --client"
ansible managed -i inventory.ini -a "systemctl is-active docker" --become -K
ansible managed -i inventory.ini -a "systemctl is-active jenkins" --become -K
```

Jenkins web interface: `http://192.168.40.136:8080`

To get the initial admin password:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### Idempotency check

Run the playbook a second time. Tasks that are already satisfied should report `ok` instead of `changed`.

## Key Concepts

- **Role**: a self-contained, reusable unit of automation with a fixed folder structure
- **`ansible-galaxy init`**: generates the skeleton of a new role
- **`roles/` directory**: the default location where Ansible searches for roles
- **Role execution order**: roles listed in the playbook run from top to bottom
- **Idempotency**: running the same playbook repeatedly produces the same end state without unnecessary changes

## Author

Essam Mostafa - [GitHub: Essammo22](https://github.com/Essammo22)
