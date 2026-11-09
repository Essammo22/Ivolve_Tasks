# Lab 20: Role-based Authorization in Jenkins

## Objective

Control who can do what in Jenkins using the **Role-based Authorization Strategy** plugin:

- Create two users: `user1` and `user2`
- Assign the **admin** role to `user1`
- Assign a **read-only** role to `user2`

## Prerequisites

- A running Jenkins server with admin access
- Security realm set to **Jenkins' own user database** (Manage Jenkins → Security)

## Steps

### 1. Install the plugin

1. Go to **Manage Jenkins → Plugins → Available plugins**
2. Search for **Role-based Authorization Strategy**
3. Install it and restart Jenkins if prompted

### 2. Enable role-based authorization

1. Go to **Manage Jenkins → Security**
2. Under **Authorization**, select **Role-Based Strategy**
3. Click **Save**

> Make sure you are logged in as an admin before doing this, so you don't lock yourself out.

### 3. Create the users

1. Go to **Manage Jenkins → Users → Create User**
2. Create `user1` (set a password, full name, and email)
3. Create `user2` the same way

### 4. Define the roles

1. Go to **Manage Jenkins → Manage and Assign Roles → Manage Roles**
2. Under **Global roles**, add two roles:

| Role        | Permissions                                                                 |
|-------------|-----------------------------------------------------------------------------|
| `admin`     | All permissions (including **Overall → Administer**)                        |
| `read-only` | **Overall → Read**, **Job → Read**, **View → Read** (read permissions only) |

3. Click **Save**

### 5. Assign the roles

1. Go to **Manage Jenkins → Manage and Assign Roles → Assign Roles**
2. Under **Global roles**, add `user1` and `user2`
3. Tick the boxes:
   - `user1` → `admin`
   - `user2` → `read-only`
4. Click **Save**

> Keep your original admin user assigned to the `admin` role too, so you always have a way back in.

## Verification

| User    | Expected behavior                                                       |
|---------|-------------------------------------------------------------------------|
| `user1` | Sees **Manage Jenkins**, can create/configure/delete jobs, manage users |
| `user2` | Can view jobs and builds only. No **Manage Jenkins**, no **New Item**, no **Build Now**, no **Configure** |

1. Log in as `user1` and confirm full access.
2. Log out, log in as `user2`, and confirm that only read access is available.

## Key Takeaways

- Roles are a cleaner way to manage permissions than setting them user by user.
- Follow the **principle of least privilege**: give users only the access they need.
- Always keep at least one admin assigned to avoid being locked out.
