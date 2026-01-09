# Authorization Implementation Plan

## Current Status: Phase 1 Complete ✅

**Last Updated**: 2026-01-09

---

## Completed Work

### Phase 1: Core Authorization Infrastructure (✅ Complete)

#### 1. Foundation Components
- ✅ **JWT Authentication** - Token generation and validation working
- ✅ **AuthHelper Utility** - Centralized authorization methods
  - `getFamilyIdFromAuth()` - Extract family_id from JWT token
  - `getUserIdFromAuth()` - Extract user_id from JWT token
  - `requireAdmin()` - Verify admin role
  - `requireFamilyAccess()` - Verify family-level access
  - `requireAccountAccess()` - Verify account ownership through User->Family chain
- ✅ **Password Encryption** - BCrypt hashing for all user passwords
- ✅ **Admin Endpoint** - `/auth/admin/encrypt-passwords` for password migration

#### 2. Controllers with Authorization (4/18 Complete)

| Controller | Status | Authorization Pattern |
|------------|--------|----------------------|
| **AssetController** | ✅ Complete | Account-level via `requireAccountAccess()` |
| **LiabilityController** | ✅ Complete | Account-level via `requireAccountAccess()` |
| **IncomeController** | ✅ Complete | Family-level via `requireFamilyAccess()` |
| **ExchangeRateController** | ✅ Complete | Admin-only via `requireAdmin()` |

#### 3. Testing Completed
- ✅ Login endpoint with encrypted passwords
- ✅ JWT token generation and validation
- ✅ AssetController family-based data isolation
- ✅ LiabilityController family-based data isolation
- ✅ Admin access to all family data
- ✅ Regular user restricted to own family data

#### 4. Test Credentials Available
```
Admin:  admin / admin123
Users:  AustinXu / password
        LorrianeChen / abc123
        demo_user / demo123
```

---

## Remaining Work

### Phase 2: Complete Authorization for All Controllers (14/18 Remaining)

#### Controllers Pending Authorization

1. **ExpenseController** - Family-level authorization
   - GET `/expenses/records` → Family expenses
   - POST `/expenses/records` → Set familyId from auth
   - PUT/DELETE → Verify family ownership

2. **NetAssetController** - Family-level authorization
   - GET `/net-assets/summary` → Family net assets
   - GET `/net-assets/trend` → Family trends

3. **UserController** - User/Family-level authorization
   - GET `/users` → Admin only
   - GET `/users/{id}` → Self or family members
   - POST `/users` → Admin only
   - PUT `/users/{id}` → Self or admin
   - DELETE `/users/{id}` → Admin only

4. **FamilyController** - Family-level authorization
   - GET `/families` → Admin only
   - GET `/families/{id}` → Own family or admin
   - POST `/families` → Admin only
   - PUT `/families/{id}` → Own family or admin

5. **BudgetController** - Family-level authorization
   - All endpoints restricted to own family data

6. **DashboardController** - Family-level authorization
   - GET `/dashboard/summary` → Own family summary

7. **ReportController** - Family-level authorization
   - All report endpoints restricted to own family data

8. **GoogleSheetsController** - User-level authorization
   - Sync operations restricted to own family data

9. **AccountLinkController** - Account ownership authorization
   - Verify both accounts belong to user's family

10. **AssetRecordController** - Account ownership authorization
    - Verify account belongs to user's family

11. **LiabilityRecordController** - Account ownership authorization
    - Verify account belongs to user's family

12. **IncomeRecordController** - Family-level authorization
    - Income records scoped to family

13. **ExpenseMinorCategoryController** - Family-level authorization
    - Category management scoped to family

14. **ExpenseMajorCategoryController** - Family-level authorization
    - Category management scoped to family

---

## Authorization Patterns Reference

### Pattern 1: Account-Level Authorization
**Use for**: AssetController, LiabilityController, AccountLinkController

```java
@GetMapping("/accounts/{id}")
public ApiResponse<AssetAccount> getAccount(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    AssetAccount account = service.getAccountById(id);

    // Verify account access (userId → User → familyId)
    authHelper.requireAccountAccess(authHeader, account.getUserId());

    return ApiResponse.success(account);
}

@PostMapping("/accounts")
public ApiResponse<AssetAccount> createAccount(
        @RequestBody AssetAccount account,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Set userId from authenticated user
    Long userId = authHelper.getUserIdFromAuth(authHeader);
    account.setUserId(userId);

    AssetAccount created = service.createAccount(account);
    return ApiResponse.success("Account created successfully", created);
}
```

### Pattern 2: Family-Level Authorization
**Use for**: ExpenseController, NetAssetController, IncomeController, BudgetController

```java
@GetMapping("/expenses/records")
public ApiResponse<List<ExpenseRecord>> getRecords(
        @RequestParam(required = false) Long familyId,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Get authenticated user's family_id
    Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

    // Use authenticated family_id (ignore query params for security)
    List<ExpenseRecord> records = service.getRecords(authenticatedFamilyId);
    return ApiResponse.success(records);
}

@PostMapping("/expenses/records")
public ApiResponse<ExpenseRecord> createRecord(
        @RequestBody ExpenseRecord record,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Set familyId from authenticated user
    Long familyId = authHelper.getFamilyIdFromAuth(authHeader);
    record.setFamilyId(familyId);

    ExpenseRecord created = service.createRecord(record);
    return ApiResponse.success("Record created successfully", created);
}
```

### Pattern 3: Admin-Only Authorization
**Use for**: ExchangeRateController, UserController (some endpoints), FamilyController (some endpoints)

```java
@PostMapping("/admin/operation")
public ApiResponse<Result> adminOperation(
        @RequestBody Request request,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // Verify admin role
    authHelper.requireAdmin(authHeader);

    Result result = service.performOperation(request);
    return ApiResponse.success(result);
}
```

### Pattern 4: Self or Admin Authorization
**Use for**: UserController PUT/GET endpoints

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

---

## Implementation Checklist

For each controller:

### Pre-Implementation
- [ ] Read controller file and identify all endpoints
- [ ] Determine which authorization pattern applies to each endpoint
- [ ] Check if service methods need modification (e.g., adding familyId filters)

### Implementation
- [ ] Add `AuthHelper` dependency to controller constructor
- [ ] Add `@RequestHeader(value = "Authorization", required = false) String authHeader` to all endpoints
- [ ] Implement authorization checks at the start of each endpoint
- [ ] For POST/PUT: Set familyId/userId from authenticated user (prevent tampering)
- [ ] For GET: Filter by authenticated user's familyId
- [ ] For DELETE: Verify ownership before deletion

### Testing
- [ ] Test with admin user (should access all data)
- [ ] Test with regular user (should only access own family data)
- [ ] Test cross-family access attempts (should be rejected)
- [ ] Test unauthorized access (should be rejected)

---

## Data Model Reference

### User → Family Relationship
```
User {
  id: Long
  familyId: Long  ← Links to Family
  username: String
  role: Role (ADMIN or USER)
}

Family {
  id: Long
  familyName: String
}

AssetAccount {
  id: Long
  userId: Long  ← Links to User (NOT familyId!)
  accountName: String
}

ExpenseRecord {
  id: Long
  familyId: Long  ← Direct family reference
  amount: BigDecimal
}
```

### Authorization Flow
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

---

## Known Issues & Solutions

### Issue 1: Password Encryption
**Problem**: Database had plain text passwords
**Solution**: Created `/auth/admin/encrypt-passwords` endpoint that uses BCrypt to hash all plain text passwords
**Status**: ✅ Resolved

### Issue 2: Account Model Mismatch
**Problem**: Controllers assumed accounts have `familyId` field, but they only have `userId`
**Solution**: Created `requireAccountAccess()` helper that validates through User->Family relationship
**Status**: ✅ Resolved

### Issue 3: Batch Operations Authorization
**Problem**: BatchRecordUpdateDTO contains multiple accounts that need validation
**Solution**: Loop through all accounts and validate each one
```java
for (BatchRecordUpdateDTO.AccountUpdate accountUpdate : batchUpdate.getAccounts()) {
    AssetAccount account = service.getAccountById(accountUpdate.getAccountId());
    authHelper.requireAccountAccess(authHeader, account.getUserId());
}
```
**Status**: ✅ Resolved

---

## Next Steps

### Immediate (Today)
1. Continue implementing authorization for remaining 14 controllers
2. Follow the patterns documented above
3. Test each controller as it's completed

### Short-term (This Week)
1. Complete all controller authorization
2. Write comprehensive integration tests
3. Update API documentation with authorization requirements
4. Document any edge cases discovered during testing

### Long-term (Future)
1. Consider adding Spring Security method-level annotations (`@PreAuthorize`)
2. Implement refresh token mechanism
3. Add audit logging for sensitive operations
4. Consider adding rate limiting for API endpoints

---

## Testing Matrix

| User Type | Asset Access | Liability Access | Expense Access | User Management | Exchange Rates |
|-----------|--------------|------------------|----------------|-----------------|----------------|
| Admin | All families | All families | All families | Full control | Full control |
| Regular User | Own family only | Own family only | Own family only | Self only | Read only |
| Unauthenticated | None | None | None | None | None |

---

## Files Modified

### Core Authorization
- `backend/src/main/java/com/finance/app/security/AuthHelper.java` - Added `requireAccountAccess()` method
- `backend/src/main/java/com/finance/app/controller/AuthController.java` - Added `/admin/encrypt-passwords` endpoint

### Controllers Updated
- `backend/src/main/java/com/finance/app/controller/AssetController.java` - Complete authorization
- `backend/src/main/java/com/finance/app/controller/LiabilityController.java` - Complete authorization
- `backend/src/main/java/com/finance/app/controller/IncomeController.java` - Complete authorization (previous session)
- `backend/src/main/java/com/finance/app/controller/ExchangeRateController.java` - Complete authorization (previous session)

### Database
- All user passwords encrypted with BCrypt
- Test credentials documented above

---

## Notes for Future Sessions

1. **Resume Pattern**: When resuming work, start with one controller at a time, implement authorization, test, then move to next
2. **Testing Approach**: Use curl commands with JWT tokens to verify authorization
3. **Common Mistakes to Avoid**:
   - Don't assume accounts have `familyId` - they have `userId`
   - Don't forget to validate ALL accounts in batch operations
   - Don't use query params for familyId - always extract from JWT token
   - Remember to set familyId/userId from auth token in POST/PUT operations
4. **Password Management**: Use `/auth/admin/encrypt-passwords` if more users are added to database with plain text passwords
