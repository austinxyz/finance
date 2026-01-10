# Authorization Design

## Overview

Finance App uses **JWT-based authentication** with **role-based authorization**. All API endpoints are protected by authorization checks that verify user identity and access permissions.

**Key Principles**:
- JWT tokens contain user identity (userId, familyId, role)
- Authorization enforced at controller level before data access
- Family-based data isolation (users only see their family's data)
- Admin users have full access to all data

## User Model

```
User {
  id: Long
  username: String
  password: String (BCrypt encrypted)
  familyId: Long      ← Links to Family
  role: Role          ← ADMIN or USER
}

Family {
  id: Long
  familyName: String
}

AssetAccount {
  id: Long
  userId: Long        ← Links to User (NOT familyId!)
  accountName: String
}

ExpenseRecord {
  id: Long
  familyId: Long      ← Direct family reference
  amount: BigDecimal
}
```

**Authorization Flow**:
```
1. Extract JWT token from Authorization header
2. Decode token to get userId, familyId, role
3. For account operations:
   - Get account.userId
   - Query User to get user.familyId
   - Compare with authenticated user's familyId
4. For family operations:
   - Compare record.familyId with authenticated user's familyId
5. Admin bypass:
   - Admin users can access all data
```

## AuthHelper API

**Core Methods**:

```java
// Extract claims from JWT
Long getUserIdFromAuth(String authHeader)
Long getFamilyIdFromAuth(String authHeader)
boolean isAdmin(String authHeader)

// Authorization checks (throw UnauthorizedException if fails)
void requireAdmin(String authHeader)
void requireFamilyAccess(String authHeader, Long recordFamilyId)
void requireAccountAccess(String authHeader, Long accountUserId)
```

## Authorization Patterns

### Pattern 1: Account-Level Authorization

**Use for**: AssetController, LiabilityController, PropertyRecordController

Account ownership verified through `User → Family` relationship.

```java
@GetMapping("/accounts/{id}")
public ApiResponse<AssetAccount> getAccount(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    AssetAccount account = service.getAccountById(id);
    authHelper.requireAccountAccess(authHeader, account.getUserId());

    return ApiResponse.success(account);
}

@PostMapping("/accounts")
public ApiResponse<AssetAccount> createAccount(
        @RequestBody AssetAccount account,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Set userId from authenticated user (prevent tampering)
    Long userId = authHelper.getUserIdFromAuth(authHeader);
    account.setUserId(userId);

    AssetAccount created = service.createAccount(account);
    return ApiResponse.success("Account created successfully", created);
}
```

### Pattern 2: Family-Level Authorization

**Use for**: ExpenseController, IncomeController, AnalysisController

FamilyId extracted from JWT token, request parameter ignored.

```java
@GetMapping("/expenses/records")
public ApiResponse<List<ExpenseRecord>> getRecords(
        @RequestParam(required = false) Long familyId,  // Ignored!
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Use authenticated user's family_id
    Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

    List<ExpenseRecord> records = service.getRecords(authenticatedFamilyId);
    return ApiResponse.success(records);
}

@PostMapping("/expenses/records")
public ApiResponse<ExpenseRecord> createRecord(
        @RequestBody ExpenseRecord record,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Set familyId from authenticated user (prevent tampering)
    Long familyId = authHelper.getFamilyIdFromAuth(authHeader);
    record.setFamilyId(familyId);

    ExpenseRecord created = service.createRecord(record);
    return ApiResponse.success("Record created successfully", created);
}
```

### Pattern 3: Admin-Only Authorization

**Use for**: ExchangeRateController (write ops), DataMigrationController

Only admin users can access.

```java
@PostMapping("/admin/migrate")
public ApiResponse<Result> adminOperation(
        @RequestBody Request request,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    authHelper.requireAdmin(authHeader);

    Result result = service.performOperation(request);
    return ApiResponse.success(result);
}
```

### Pattern 4: Self-or-Admin Authorization

**Use for**: UserController, UserProfileController

Users can access their own data, admin can access any user's data.

```java
@PutMapping("/users/{id}")
public ApiResponse<User> updateUser(
        @PathVariable Long id,
        @RequestBody User userDetails,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    Long authenticatedUserId = authHelper.getUserIdFromAuth(authHeader);

    // Allow if updating self OR if admin
    if (!authenticatedUserId.equals(id) && !authHelper.isAdmin(authHeader)) {
        throw new UnauthorizedException("只能修改自己的信息");
    }

    User updated = service.updateUser(id, userDetails);
    return ApiResponse.success("User updated successfully", updated);
}
```

## Frontend Integration

### Axios Request Interceptor

All API requests automatically include `Authorization` header:

```javascript
// frontend/src/api/request.js
const token = localStorage.getItem('token')
if (token) {
  config.headers.Authorization = `Bearer ${token}`
}
```

### Router Guards

Protected routes require authentication:

```javascript
// frontend/src/router/index.js
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const isAuthenticated = !!token

  const requiresAuth = to.matched.some(record =>
    record.meta.requiresAuth !== false
  )

  if (requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    next('/dashboard')
  } else {
    next()
  }
})
```

### 401 Response Handling

Automatic logout on token expiration:

```javascript
// Response interceptor
if (error.response?.status === 401) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}
```

## Test Credentials

```
Admin User:
  Username: admin
  Password: admin123
  Role: ADMIN
  Access: All families, admin-only endpoints

Regular Users:
  Username: AustinXu
  Password: password
  Role: USER
  Access: Own family data only

Other Users:
  LorrianeChen / abc123
  demo_user / demo123
```

## Common Patterns

### Batch Operations

Validate ALL accounts in batch:

```java
for (BatchRecordUpdateDTO.AccountUpdate accountUpdate : batchUpdate.getAccounts()) {
    AssetAccount account = service.getAccountById(accountUpdate.getAccountId());
    authHelper.requireAccountAccess(authHeader, account.getUserId());
}
```

### Service Methods for Authorization

When authorization needs to check records, add getter methods to services:

```java
// In service class
public ExpenseRecord getRecordById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new RuntimeException("记录不存在"));
}

// In controller
ExpenseRecord record = service.getRecordById(id);
authHelper.requireFamilyAccess(authHeader, record.getFamilyId());
```

## Security Guarantees

1. **Parameter Tampering Prevention**: FamilyId from JWT token used, not request params
2. **Cross-Family Access Prevention**: Users cannot access other families' data
3. **Account Ownership Verification**: Account operations verify through User→Family chain
4. **Admin Privilege Enforcement**: Admin-only endpoints reject regular users
5. **Token Validation**: Invalid/expired tokens rejected with clear error messages

## Known Issues & Solutions

### Issue: Accounts have userId, not familyId

**Problem**: Controllers assumed `account.familyId` exists, but accounts link to users via `userId`

**Solution**: Use `requireAccountAccess()` which validates through User→Family relationship:
```java
AssetAccount account = service.getAccountById(accountId);
authHelper.requireAccountAccess(authHeader, account.getUserId());
```

### Issue: Password encryption

**Problem**: Database had plain text passwords

**Solution**: Use `/auth/admin/encrypt-passwords` endpoint to migrate passwords to BCrypt

**Prevention**: All new passwords automatically encrypted via BCrypt in AuthController

## Files Reference

**Backend**:
- `backend/src/main/java/com/finance/app/security/AuthHelper.java` - Core authorization logic
- `backend/src/main/java/com/finance/app/controller/AuthController.java` - Login, password encryption

**Frontend**:
- `frontend/src/stores/auth.js` - Pinia auth store
- `frontend/src/api/auth.js` - Auth API methods
- `frontend/src/api/request.js` - Axios interceptors
- `frontend/src/router/index.js` - Route guards
- `frontend/src/views/Login.vue` - Login page

## Implementation Checklist

When adding authorization to a new controller:

- [ ] Add `AuthHelper` dependency to constructor
- [ ] Add `@RequestHeader(value = "Authorization", required = false) String authHeader` to endpoints
- [ ] Determine authorization pattern (Account, Family, Admin, Self-or-admin)
- [ ] For POST/PUT: Set familyId/userId from auth token (prevent tampering)
- [ ] For GET: Filter by authenticated user's familyId
- [ ] For DELETE: Verify ownership before deletion
- [ ] Test with admin user (should access all data)
- [ ] Test with regular user (should only access own family data)
- [ ] Test cross-family access attempts (should be rejected)
