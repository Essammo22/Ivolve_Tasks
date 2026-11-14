# Lab 27: Securing Sensitive Data with Ansible Vault

Automate a MySQL installation and configuration with an Ansible role, and protect the database password using **Ansible Vault**.

## Objectives

- Create a `mysql` Ansible role that:
  - Installs MySQL
  - Creates the `ivolve` database
  - Creates the `ivolveuser` user with all privileges on the `ivolve` database
- Encrypt sensitive data (the database user password) with Ansible Vault
- Validate the setup on the managed node by connecting as the new user and listing databases

## Project Structure

```
.
├── ansible.cfg
├── inventory
├── mysql.yml                  # Main playbook
├── group_vars/
│   └── all/
│       └── vault.yml          # Encrypted with Ansible Vault
└── roles/
    └── mysql/
        ├── defaults/
        │   └── main.yml       # db_name, db_user
        └── tasks/
            └── main.yml       # Install, configure, validate
```

## Prerequisites

- Ansible installed on the control node
- A managed node (Ubuntu) reachable over SSH
- The `community.mysql` collection:

```bash
ansible-galaxy collection install community.mysql
```

## How It Works

### 1. The Role

The `mysql` role runs these tasks in order:

1. Install `mysql-server` and `python3-pymysql`
2. Start and enable the MySQL service
3. Create the `ivolve` database
4. Create `ivolveuser` with `ALL` privileges on `ivolve.*`
5. Validate by connecting as `ivolveuser` and running `SHOW DATABASES;`

Default variables (`roles/mysql/defaults/main.yml`):

```yaml
db_name: ivolve
db_user: ivolveuser
```

### 2. Ansible Vault

The password is never stored in plain text. It lives in an encrypted file:

```bash
ansible-vault create group_vars/all/vault.yml
```

Content of the file (before encryption):

```yaml
vault_db_password: "<your-strong-password>"
```

The role reads it as `{{ vault_db_password }}`.

> Tip: tasks that handle the password use `no_log: true` so it never appears in the output.

### 3. The Playbook

```yaml
---
- name: Setup MySQL
  hosts: all
  become: true
  roles:
    - mysql
```

## Usage

Run the playbook and provide the vault password when prompted:

```bash
ansible-playbook -i inventory mysql.yml --ask-vault-pass
```

Useful Vault commands:

```bash
ansible-vault view group_vars/all/vault.yml      # View decrypted content
ansible-vault edit group_vars/all/vault.yml      # Edit the encrypted file
ansible-vault rekey group_vars/all/vault.yml     # Change the vault password
```

## Validation

On the managed node, connect with the new user:

```bash
mysql -u ivolveuser -p
```

Then list the databases:

```sql
SHOW DATABASES;
```

Expected output:

```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| ivolve             |
+--------------------+
```

Seeing `ivolve` confirms that the database, the user, and the privileges were created correctly.

## Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user` | Make sure the password matches the one in `vault.yml` and the user was created with `host: localhost` |
| `No module named pymysql` | Install `python3-pymysql` on the managed node |
| Vault decryption failed | Check that you typed the correct vault password |

## Security Notes

- Never commit a plain-text password or a vault password file.
- If you use a `.vault_pass` file, add it to `.gitignore`.
- Commit only the **encrypted** `vault.yml`.

## Author

Essam Mostafa — NTI HireReady Cloud DevOps Accelerator
