# Lab 28: Automated Host Discovery with Ansible Dynamic Inventory

## Overview

In this lab, I used the Ansible **AWS EC2 dynamic inventory plugin** (`amazon.aws.aws_ec2`) to automatically discover EC2 instances by tag, instead of maintaining a static list of IP addresses. Then I ran a playbook against the discovered host to install and start Nginx, and verified the result directly on the instance.

## Objectives

- Create an AWS EC2 instance with the tag `server: ivolve`.
- Set up an Ansible dynamic inventory that discovers running EC2 instances with the tag `server: ivolve`.
- List the target hosts using the `ansible-inventory` command.
- Run a playbook on the EC2 instance.
- Verify the configuration on the EC2 instance.

## Why Dynamic Inventory?

A static inventory file breaks as soon as an instance is replaced or gets a new IP. A dynamic inventory asks AWS "which instances match this filter right now?" every time Ansible runs, so the host list is always up to date.

## Project Structure

```
lab28/
├── aws_ec2.yml        # Dynamic inventory configuration
├── lab-aws.yaml       # Playbook
└── README.md
```

## Prerequisites

- Ansible installed inside a Python virtual environment (`ansible-venv`)
- `boto3` and `botocore` Python packages
- The `amazon.aws` Ansible collection
- AWS credentials configured on the control node (`aws configure` or environment variables)
- The EC2 key pair (`ivolve-key.pem`) on the control node with permissions `400`

```bash
ansible-galaxy collection install amazon.aws
pip install boto3 botocore
chmod 400 /home/controlplane/.ssh/ivolve-key.pem
```

## Steps

### 1. Create the EC2 Instance

Launched an Ubuntu EC2 instance in `eu-north-1` from the AWS Console with:

- **Tag:** `server = ivolve`
- **Security Group:** inbound SSH (port 22) allowed from my IP
- **Key pair:** `ivolve-key`

> 📸 Screenshot: EC2 instance with the `server: ivolve` tag
>
> `<add screenshot here>`

### 2. Configure the Dynamic Inventory

File: `aws_ec2.yml`

```yaml
plugin: amazon.aws.aws_ec2

regions:
  - eu-north-1

filters:
  "tag:server": ivolve
  instance-state-name: running

hostnames:
  - ip-address

compose:
  ansible_host: public_ip_address
  ansible_user: "'ubuntu'"
  ansible_ssh_private_key_file: "'/home/controlplane/.ssh/ivolve-key.pem'"
```

Key points:

- `plugin: amazon.aws.aws_ec2` tells Ansible to use the AWS EC2 inventory plugin.
- `regions` limits the search to the region where the instance lives.
- `filters` selects only **running** instances that have the tag `server: ivolve`.
- `hostnames` uses the public IP as the host name.
- `compose` sets the connection variables for every discovered host: the SSH user (`ubuntu`) and the private key path. The extra quotes make these literal strings instead of Jinja2 expressions.

### 3. List the Target Hosts

```bash
ansible-inventory -i aws_ec2.yml --graph
```

**Output:**

```
@all:
  |--@ungrouped:
  |--@aws_ec2:
  |  |--13.53.40.154
```

The instance was discovered automatically and placed in the `aws_ec2` group, with no manual host entry.

### 4. Run the Playbook

File: `lab-aws.yaml`

```yaml
---
- name: Configure AWS EC2 using Ansible Dynamic Inventory
  hosts: aws_ec2
  become: true

  tasks:
    - name: Update apt cache
      apt:
        update_cache: yes

    - name: Install nginx
      apt:
        name: nginx
        state: present

    - name: Start and enable nginx
      service:
        name: nginx
        state: started
        enabled: yes

    - name: Verify nginx is running
      command: systemctl is-active nginx
      register: nginx_status
      changed_when: false

    - name: Display nginx status
      debug:
        msg: "Nginx status is {{ nginx_status.stdout }}"
```

Run it:

```bash
ansible-playbook -i aws_ec2.yml lab-aws.yaml
```

**Output:**

```
PLAY [Configure AWS EC2 using Ansible Dynamic Inventory] ***********************

TASK [Gathering Facts] *********************************************************
ok: [13.53.40.154]

TASK [Update apt cache] ********************************************************
changed: [13.53.40.154]

TASK [Install nginx] ***********************************************************
changed: [13.53.40.154]

TASK [Start and enable nginx] **************************************************
ok: [13.53.40.154]

TASK [Verify nginx is running] *************************************************
ok: [13.53.40.154]

TASK [Display nginx status] ****************************************************
ok: [13.53.40.154] => {
    "msg": "Nginx status is active"
}

PLAY RECAP *********************************************************************
13.53.40.154   : ok=6    changed=2    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0
```

### 5. Verify the Configuration

Connected to the EC2 instance over SSH and checked the Nginx service:

```bash
systemctl status nginx.service
```

**Output:**

```
● nginx.service - A high performance web server and a reverse proxy server
     Loaded: loaded (/usr/lib/systemd/system/nginx.service; enabled; preset: enabled)
     Active: active (running) since Sat 2026-11-14 21:43:22 UTC; 9min ago
       Docs: man:nginx(8)
    Process: 2440 ExecStartPre=/usr/sbin/nginx -t -q -g daemon on; master_process on; (code=exited, status=0/SUCCESS)
    Process: 2442 ExecStart=/usr/sbin/nginx -g daemon on; master_process on; (code=exited, status=0/SUCCESS)
   Main PID: 2471 (nginx)
      Tasks: 3 (limit: 627)
     Memory: 2.4M (peak: 5.2M)
        CPU: 25ms
     CGroup: /system.slice/nginx.service
             ├─2471 "nginx: master process /usr/sbin/nginx -g daemon on; master_process on;"
             ├─2474 "nginx: worker process"
             └─2475 "nginx: worker process"
```

Nginx is **active (running)** and **enabled** to start on boot, which confirms the playbook applied the configuration.

## Troubleshooting

### `Permission denied (publickey)`

The first run failed at `Gathering Facts` with:

```
UNREACHABLE! => Failed to connect to the host via ssh: controlplane@13.53.40.154: Permission denied (publickey).
```

**Cause:** the network connection worked (port 22 was open), but the server rejected the credentials. Ansible was trying to log in as my local user (`controlplane`) with the default SSH keys, instead of the EC2 user and the key pair of the instance.

**Fix:** set the SSH user and private key for the discovered hosts in the inventory file using `compose`:

```yaml
compose:
  ansible_host: public_ip_address
  ansible_user: "'ubuntu'"
  ansible_ssh_private_key_file: "'/home/controlplane/.ssh/ivolve-key.pem'"
```

The default user depends on the AMI: `ubuntu` for Ubuntu, `ec2-user` for Amazon Linux.

### EC2 Instance Connect: "Failed to connect to your instance"

The Security Group allowed SSH only from **My IP**. The Console's **Connect** button connects from AWS's own IP range, so it was blocked. SSH from the control node worked fine, so the Security Group was left unchanged.

### Warnings in the output

```
Collection amazon.aws does not support Ansible version 2.16.14
The 'tags' host variable is deprecated. Use 'ec2_tags' instead.
```

Both are warnings from the plugin, not errors. They did not affect the result. If you use instance tags as host variables in your own inventory, use `ec2_tags` instead of `tags`.

## Key Takeaways

- Dynamic inventory removes the need to track IP addresses manually.
- Tags are a clean way to group EC2 instances for Ansible.
- Ansible needs the correct SSH **user** and **private key** for the AMI in use, and `compose` lets you set them once for all discovered hosts.
- `ansible-inventory --graph` is a quick way to confirm what Ansible discovered before running any playbook.
