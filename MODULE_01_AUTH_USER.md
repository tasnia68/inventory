# Module 1: Authentication & User Management

## 1. Module Overview

**Name:** Authentication & User Management  
**Description:** This module handles user authentication via JWT, manages the user lifecycle (creation, invitation, updates, deletion), and enforces Role-Based Access Control (RBAC) through roles and permissions.

---

## 2. Required Headers

> **IMPORTANT:** All API calls require these headers:

| Header | Value | Required For |
|--------|-------|--------------|
| `Content-Type` | `application/json` | All POST/PUT requests |
| `Authorization` | `Bearer <token>` | All protected endpoints |
| `X-Tenant-ID` | e.g., `default-tenant` | **All endpoints EXCEPT** `/api/v1/auth/**` |

---

## 3. Frontend Pages Required

### 3.1 Public Pages (No Authentication)

| Page | Route | Purpose |
|------|-------|---------|
| **Login Page** | `/login` | User authentication |
| **Accept Invitation Page** | `/accept-invitation?token=<uuid>` | New user registration via invite link |
| **Forgot Password** | `/forgot-password` | Password reset request (if implemented) |

### 3.2 Protected Pages (Requires Authentication)

| Page | Route | Access | Purpose |
|------|-------|--------|---------|
| **Dashboard** | `/dashboard` | All Users | Main landing after login |
| **User List** | `/users` | Admin/Manager | View all users |
| **User Details** | `/users/:id` | Admin/Manager | View/Edit specific user |
| **Invite User** | `/users/invite` | Admin/Manager | Send user invitations |
| **My Profile** | `/profile` | All Users | View/Edit own profile |
| **Role Management** | `/roles` | Admin | View available roles |

---

## 4. API Contract

### 4.1 Auth Controller
**Base URL:** `/api/v1/auth`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/login` | ❌ No | Authenticate user and get JWT tokens |

### 4.2 User Controller  
**Base URL:** `/api/v1/users`

| Method | Endpoint | Auth | Access | Description |
|--------|----------|------|--------|-------------|
| GET | `/` | ✅ Yes | Admin/Manager | List all users |
| POST | `/` | ✅ Yes | Admin | Create user directly |
| GET | `/{id}` | ✅ Yes | Admin/Manager | Get user by ID |
| PUT | `/{id}` | ✅ Yes | Admin | Update user |
| DELETE | `/{id}` | ✅ Yes | Admin | Delete user |
| GET | `/me` | ✅ Yes | All Users | Get own profile |
| PUT | `/me` | ✅ Yes | All Users | Update own profile |
| POST | `/invite` | ✅ Yes | Admin/Manager | Send invitation email |
| POST | `/invite/accept` | ❌ No | Public | Accept invitation (uses token) |

### 4.3 Role Controller
**Base URL:** `/api/v1/roles`

| Method | Endpoint | Auth | Access | Description |
|--------|----------|------|--------|-------------|
| GET | `/` | ✅ Yes | Admin/Manager | List all roles |

---

## 5. User Invitation Flow

### 5.1 Business Flow Diagram

```
Admin/Manager                         System                          New User
     |                                   |                                |
     |  1. POST /users/invite            |                                |
     |  {email, roleName}                |                                |
     |---------------------------------->|                                |
     |                                   |  2. Create invitation record   |
     |                                   |  3. Send email with token      |
     |                                   |-------------------------------->|
     |  4. Return success                |                                |
     |<----------------------------------|                                |
     |                                   |                                |
     |                                   |  5. User clicks link           |
     |                                   |  /accept-invitation?token=xxx  |
     |                                   |<-------------------------------|
     |                                   |                                |
     |                                   |  6. POST /users/invite/accept  |
     |                                   |  {token, password, firstName,  |
     |                                   |   lastName}                    |
     |                                   |<-------------------------------|
     |                                   |                                |
     |                                   |  7. Create user account        |
     |                                   |  8. Mark invitation accepted   |
     |                                   |  9. Return success             |
     |                                   |-------------------------------->|
     |                                   |                                |
     |                                   |  10. User can now login        |
     |                                   |  POST /auth/login              |
     |                                   |<-------------------------------|
```

### 5.2 Step-by-Step Process

1. **Admin sends invitation:**
   - Admin calls `POST /api/v1/users/invite` with email and role
   - System creates invitation record with unique token (expires in 7 days)
   - System sends email to invitee with invitation link

2. **New user receives email:**
   - Email contains link: `https://yourapp.com/accept-invitation?token=<uuid>`
   - Link directs to frontend "Accept Invitation" page

3. **New user accepts invitation:**
   - Frontend shows form for: password, firstName, lastName
   - Frontend calls `POST /api/v1/users/invite/accept`
   - System creates user account with provided details
   - System assigns the role from the invitation

4. **User logs in:**
   - User navigates to login page
   - User enters email + password
   - System returns JWT tokens

### 5.3 Invitation Curl Examples

**Step 1: Admin sends invitation**
```bash
curl -X POST http://localhost:8080/api/v1/users/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "roleName": "ROLE_ADMIN"
  }'
```

**Response:**
```json
{
  "status": 200,
  "message": "Invitation sent successfully"
}
```

**Step 2: New user accepts invitation**
```bash
curl -X POST http://localhost:8080/api/v1/users/invite/accept \
  -H "X-Tenant-ID: default-tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "1c0d07ad-17be-4005-a673-6c6bac3371dc",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Response:**
```json
{
  "status": 200,
  "message": "Invitation accepted successfully"
}
```

**Step 3: New user logs in**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePass123!"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 6. All API Curl Examples

### 6.1 Authentication

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"Admin123!"}'
```

### 6.2 User Management

**List all users:**
```bash
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant"
```

**Create user directly:**
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "direct@example.com",
    "password": "Password123!",
    "firstName": "Direct",
    "lastName": "User",
    "roles": ["ROLE_ADMIN"]
  }'
```

**Get user by ID:**
```bash
curl http://localhost:8080/api/v1/users/{userId} \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant"
```

**Update user:**
```bash
curl -X PUT http://localhost:8080/api/v1/users/{userId} \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name",
    "enabled": true,
    "roles": ["ROLE_ADMIN"]
  }'
```

**Delete user:**
```bash
curl -X DELETE http://localhost:8080/api/v1/users/{userId} \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant"
```

**Get my profile:**
```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant"
```

**Update my profile:**
```bash
curl -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "MyNew",
    "lastName": "Name"
  }'
```

### 6.3 Role Management

**List all roles:**
```bash
curl http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant"
```

---

## 7. Business Rules

### 7.1 Authentication
- JWT access tokens expire in 24 hours
- JWT refresh tokens expire in 7 days  
- Failed login attempts should be logged
- Passwords must meet security requirements

### 7.2 User Management
- Users are scoped to tenants (multi-tenancy)
- An admin in Tenant A cannot see users in Tenant B
- Users can only update their own profile via `/me` endpoints
- Only Admins can create, update, or delete other users

### 7.3 Roles & Permissions
| Role | Description | Permissions |
|------|-------------|-------------|
| `ROLE_ADMIN` | Full system access | All operations |
| `ROLE_MANAGER` | Operational access | Manage users, view reports |
| `ROLE_USER` | Basic access | View data, perform tasks |

### 7.4 Invitation Rules
- Invitations expire after 7 days
- Each email can only have one active invitation
- Once accepted, invitation cannot be reused
- Only Admin/Manager can send invitations

---

## 8. Bootstrap Admin User

On application startup, an admin user is automatically created:

| Field | Value |
|-------|-------|
| Email | `admin@test.com` |
| Password | `Admin123!` |
| Role | `ROLE_ADMIN` |
| Tenant | `default-tenant` |

---

## 9. Quick Start Script

```bash
# 1. Login and store token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"Admin123!"}' | jq -r '.accessToken')

echo "Token: $TOKEN"

# 2. Use token for any protected API
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default-tenant" | jq .
```
