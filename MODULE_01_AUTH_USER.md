# Module 1: Authentication & User Management

## 1. Module Identification
**Name:** Authentication & User Management
**Description:** This module serves as the entry point for security and user administration. It handles user authentication via JWT, manages the user lifecycle (creation, invitation, updates, deletion), and enforces Role-Based Access Control (RBAC) through roles and permissions.

## 2. API Contract for Frontend (Bootstrap)

### Auth Controller
**Base URL:** `/api/v1/auth`

| Method | Endpoint | Request Body | Response Body | Headers | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/login` | `{"email": "string", "password": "string"}` | `{"accessToken": "string", "refreshToken": "string"}` | - | **JWT Based Login.** Returns access/refresh tokens. |

### User Controller
**Base URL:** `/api/v1/users`

| Method | Endpoint | Request Body | Response Body | Headers | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/invite` | `{"email": "user@example.com", "roleName": "MANAGER"}` | `{"status": 200, "message": "Invitation sent successfully", "timestamp": "2023-10-27T10:00:00"}` | `Authorization: Bearer <token>` | **Admin/Manager Only.** Sends an email invitation. Can invite new Admins by specifying `ROLE_ADMIN`. |
| **POST** | `/invite/accept` | `{"token": "uuid", "password": "newPass", "firstName": "John", "lastName": "Doe"}` | `{"status": 200, "message": "Invitation accepted successfully", "timestamp": "..."}` | - | Public endpoint for accepting invites. |
| **POST** | `/` | `{"email": "admin@sys.com", "password": "pass", "firstName": "Admin", "lastName": "User", "roles": ["ADMIN"]}` | `{"status": 201, "message": "User created successfully", "data": { "id": "uuid", "email": "...", "roles": ["ADMIN"] }}` | `Authorization: Bearer <token>` | **Admin Only.** Direct user creation without invite flow. |
| **GET** | `/` | - | `{"status": 200, "data": [{ "id": "uuid", "email": "...", "firstName": "...", "lastName": "...", "roles": ["USER"], "enabled": true }]}` | `Authorization: Bearer <token>` | **Admin/Manager Only.** List all users in tenant. |
| **GET** | `/{id}` | - | `{"status": 200, "data": { "id": "uuid", "email": "...", "firstName": "...", "roles": [...] }}` | `Authorization: Bearer <token>` | **Admin/Manager Only.** Get details of a specific user. |
| **PUT** | `/{id}` | `{"firstName": "Updated", "lastName": "Name", "enabled": true, "roles": ["MANAGER"]}` | `{"status": 200, "message": "User updated successfully", "data": { ... }}` | `Authorization: Bearer <token>` | **Admin Only.** Update user details/roles. |
| **DELETE** | `/{id}` | - | `{"status": 200, "message": "User deleted successfully"}` | `Authorization: Bearer <token>` | **Admin Only.** Remove a user. |
| **GET** | `/me` | - | `{"status": 200, "data": { "id": "uuid", "email": "...", "roles": [...] }}` | `Authorization: Bearer <token>` | Get profile of currently logged-in user. |
| **PUT** | `/me` | `{"firstName": "My", "lastName": "Name"}` | `{"status": 200, "message": "Profile updated successfully", "data": { ... }}` | `Authorization: Bearer <token>` | Update own profile information. |

### Role Controller
**Base URL:** `/api/v1/roles`

| Method | Endpoint | Request Body | Response Body | Headers | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/` | - | `{"status": 200, "data": [{ "name": "ADMIN", "description": "...", "permissions": [{ "name": "USER_CREATE" }] }]}` | `Authorization: Bearer <token>` | **Admin/Manager Only.** List all available roles. |

## 3. Business Logic / Implementation Notes

### Authentication (JWT Based)
- **Mechanism:** The system uses purely **stateless JWT (JSON Web Token)** authentication.
- **Login:** Returns an `accessToken` (short-lived) and `refreshToken` (long-lived).
- **Authorization Header:** All protected endpoints **must** include the following header:
  ```
  Authorization: Bearer <your_access_token>
  ```

### Initial Admin Creation (Tenant Bootstrapping)
- **The "First" Admin:** The initial Administrator account is **automatically created** when a new Tenant is registered via the **Tenant Management Module** (Module 2).
- **Process:**
  1. User calls `POST /api/v1/tenants/register` (Public Endpoint).
  2. Request body includes `adminEmail` and `adminPassword`.
  3. System creates the Tenant, creates the `ROLE_ADMIN`, and creates the initial User with that role.
  4. This user can then log in via `/api/v1/auth/login` to manage the system.

### User Invitation & Management
- **Creating Additional Admins:** An existing Admin can create other Admins by:
  - **Invitation:** Sending an invite with `roleName: "ROLE_ADMIN"`.
  - **Direct Creation:** Using `POST /api/v1/users` with `roles: ["ROLE_ADMIN"]`.
- **Flow:**
  1.  **Invite:** Admin sends invite -> Email with Token sent.
  2.  **Accept:** User clicks link -> Frontend calls `/invite/accept` with token & password.
  3.  **Active:** User is now active and can login.

### Multi-Tenancy & RBAC
- **Context:** All user operations are strictly scoped to the tenant. An Admin in Tenant A **cannot** see or manage users in Tenant B.
- **RBAC:** `@PreAuthorize` annotations enforce permissions.
  - `ROLE_ADMIN`: Full access to tenant resources.
  - `ROLE_MANAGER`: Operational access (manage users, stock), usually restricted from destructive actions or system settings.
  - `ROLE_USER`: Basic access (view, perform tasks).
