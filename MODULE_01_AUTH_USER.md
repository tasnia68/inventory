# Module 1: Authentication & User Management

## 1. Module Identification
**Name:** Authentication & User Management
**Description:** This module serves as the entry point for security and user administration. It handles user authentication via JWT, manages the user lifecycle (creation, invitation, updates, deletion), and enforces Role-Based Access Control (RBAC) through roles and permissions.

## 2. API Contract for Frontend (Bootstrap)

### Auth Controller
**Base URL:** `/api/v1/auth`

| Method | Endpoint | Request Body | Response Body | Headers | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/login` | `{"email": "string", "password": "string"}` | `{"accessToken": "string", "refreshToken": "string"}` | - | Standard login. Returns JWT tokens. |

### User Controller
**Base URL:** `/api/v1/users`

| Method | Endpoint | Request Body | Response Body | Headers | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/invite` | `{"email": "user@example.com", "roleName": "MANAGER"}` | `{"status": 200, "message": "Invitation sent successfully", "timestamp": "2023-10-27T10:00:00"}` | `Authorization: Bearer <token>` | **Admin/Manager Only.** Sends an email invitation. |
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

### Authentication
- **JWT (JSON Web Token):** The system uses stateless authentication. Upon successful login, an `accessToken` (short-lived) and `refreshToken` (long-lived) are issued.
- **Security:** Passwords must be hashed (BCrypt) before storage. All protected endpoints require a valid `Authorization` header with `Bearer <token>`.

### User Invitation Flow
1.  **Invite:** An Admin or Manager sends an invitation to an email address with a specific role.
2.  **Email:** The system sends an email containing a unique link with a `token`.
3.  **Accept:** The user clicks the link (frontend route) which calls the `accept` API with the token and their desired password/details.
4.  **Activation:** Upon acceptance, the user is created/activated in the system and linked to the tenant.

### Role-Based Access Control (RBAC)
- **Roles:** Defined entities like `ADMIN`, `MANAGER`, `USER`.
- **Permissions:** Granular rights (e.g., `USER_CREATE`, `STOCK_VIEW`) assigned to roles.
- **Enforcement:** usage of `@PreAuthorize` annotations (e.g., `@PreAuthorize("hasRole('ADMIN')")`) ensures only authorized users can access sensitive endpoints.

### Multi-Tenancy
- **Context:** All user operations are scoped to the current tenant. The `UserService` must ensure that users are only created/queried within the context of the logged-in user's tenant (or the tenant identified during registration).
