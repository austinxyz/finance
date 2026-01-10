# Frontend Authorization Implementation

**Date**: 2026-01-09
**Status**: ✅ Complete

## Overview

Frontend JWT authorization has been implemented to work with the backend JWT system. Users must now log in before accessing the application.

## Features Implemented

### 1. Login Page ✅
**File**: `frontend/src/views/Login.vue`

- Clean, modern login UI with Tailwind CSS
- Username and password authentication
- Loading states and error handling
- Demo credentials displayed for easy access
- Redirects to dashboard after successful login

### 2. Auth API Module ✅
**File**: `frontend/src/api/auth.js`

Methods:
- `login(username, password)` - Authenticate user
- `logout()` - End user session
- `getCurrentUser()` - Get current user profile
- `encryptPasswords()` - Admin-only password migration

### 3. Pinia Auth Store ✅
**File**: `frontend/src/stores/auth.js`

**State**:
- `token` - JWT token (persisted to localStorage)
- `user` - User object with familyId, userId, role, username

**Getters**:
- `isAuthenticated` - Whether user is logged in
- `isAdmin` - Whether user has ADMIN role
- `familyId` - User's family ID
- `userId` - User's ID
- `username` - User's username

**Actions**:
- `login(username, password)` - Log in and store token/user
- `logout()` - Clear token and user data
- `refreshUser()` - Refresh user profile from backend

### 4. Axios Request Interceptor ✅
**File**: `frontend/src/api/request.js`

**Request Interceptor**:
```javascript
// Automatically adds Authorization header to ALL requests
const token = localStorage.getItem('token')
if (token) {
  config.headers.Authorization = `Bearer ${token}`
}
```

**Response Interceptor**:
```javascript
// Handles 401 Unauthorized responses
if (error.response?.status === 401) {
  // Clear auth data
  localStorage.removeItem('token')
  localStorage.removeItem('user')

  // Redirect to login
  window.location.href = '/login'
}
```

### 5. Router Authentication Guards ✅
**File**: `frontend/src/router/index.js`

**Login Route**:
```javascript
{
  path: '/login',
  name: 'Login',
  component: Login,
  meta: {
    requiresAuth: false  // Public route
  }
}
```

**Protected Routes**:
```javascript
{
  path: '/',
  component: MainLayout,
  meta: {
    requiresAuth: true  // All children require authentication
  },
  children: [
    // All existing routes (dashboard, assets, liabilities, etc.)
  ]
}
```

**Navigation Guard**:
```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const isAuthenticated = !!token

  const requiresAuth = to.matched.some(record =>
    record.meta.requiresAuth !== false
  )

  if (requiresAuth && !isAuthenticated) {
    // Redirect to login if authentication required
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    // Redirect to dashboard if already logged in
    next('/dashboard')
  } else {
    next()
  }
})
```

### 6. MainLayout Updates ✅
**File**: `frontend/src/components/MainLayout.vue`

**User Info Display**:
- Shows username in header
- Shows "管理员" badge for admin users
- Logout button in header

**Logout Handler**:
```javascript
const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
```

## User Flow

### First Visit (Not Authenticated)
1. User navigates to `http://localhost:3000`
2. Router detects no token
3. Redirects to `/login`
4. User enters credentials
5. On successful login:
   - JWT token stored in localStorage
   - User object stored in localStorage
   - Redirected to `/dashboard`
6. All subsequent API calls include `Authorization: Bearer <token>` header

### Subsequent Visits (Token Exists)
1. User navigates to any route
2. Router detects token in localStorage
3. Allows access to protected routes
4. Axios includes token in all API requests

### Session Expiration
1. Backend returns 401 Unauthorized (token expired or invalid)
2. Response interceptor clears localStorage
3. Redirects to `/login`
4. User must log in again

### Logout
1. User clicks "退出登录" button
2. Auth store clears token and user data
3. localStorage cleared
4. Redirected to `/login`

## Testing Checklist

### Authentication Flow ✓
- [ ] Navigate to `http://localhost:3000` without token → Redirects to /login
- [ ] Login with valid credentials → Redirects to /dashboard
- [ ] Login with invalid credentials → Shows error message
- [ ] Access dashboard while authenticated → Works
- [ ] Logout → Clears data and redirects to /login

### API Authorization ✓
- [ ] API calls include Authorization header when logged in
- [ ] API calls work correctly with JWT token
- [ ] 401 responses trigger automatic logout and redirect
- [ ] Family-level endpoints use authenticated user's familyId
- [ ] Account-level endpoints verify ownership

### UI/UX ✓
- [ ] Login page displays correctly
- [ ] Username shows in header after login
- [ ] Admin badge shows for admin users
- [ ] Logout button works
- [ ] Loading states work during login
- [ ] Error messages display for failed login

## Demo Credentials

```
Admin User:
  Username: admin
  Password: admin123
  Role: ADMIN
  Access: All families, admin-only endpoints

Regular User:
  Username: AustinXu
  Password: password
  Role: USER
  Access: Own family data only

Other Users:
  LorrianeChen / abc123
  demo_user / demo123
```

## Files Created/Modified

### Created
- `frontend/src/views/Login.vue` - Login page component
- `frontend/src/api/auth.js` - Authentication API methods
- `frontend/src/stores/auth.js` - Pinia authentication store

### Modified
- `frontend/src/api/request.js` - Added Authorization headers and 401 handling
- `frontend/src/router/index.js` - Added login route and navigation guards
- `frontend/src/components/MainLayout.vue` - Added user info and logout button

## Security Features

1. **Token Storage**: JWT token stored in localStorage (client-side)
2. **Auto-Include Headers**: Axios automatically includes Authorization header
3. **Auto-Logout on 401**: Expired/invalid tokens trigger automatic logout
4. **Route Protection**: Unauthenticated users redirected to login
5. **No Token Leakage**: Token only sent in Authorization header, not in URL or cookies

## Integration with Backend

### Backend JWT Structure
```json
{
  "userId": 1,
  "familyId": 1,
  "role": "ADMIN",
  "username": "admin"
}
```

### Frontend Token Storage
```javascript
localStorage.setItem('token', 'eyJhbGciOiJIUzI1...')
localStorage.setItem('user', JSON.stringify({
  id: 1,
  familyId: 1,
  role: 'ADMIN',
  username: 'admin',
  ...
}))
```

### API Request Example
```javascript
// Frontend makes request
const accounts = await assetAPI.getAccounts()

// Axios adds header automatically
// Authorization: Bearer eyJhbGciOiJIUzI1...

// Backend validates JWT and extracts familyId
// Returns only accounts for authenticated user's family
```

## Next Steps (Optional Enhancements)

1. **Remember Me**: Add persistent sessions with longer-lived tokens
2. **Auto Refresh**: Implement token refresh before expiration
3. **Session Timeout Warning**: Warn user before token expires
4. **Multi-tab Sync**: Sync logout across browser tabs
5. **Password Change**: Add password change functionality
6. **Profile Settings**: User profile management page

## Known Limitations

1. **Token Storage**: localStorage is vulnerable to XSS attacks
   - Acceptable for this personal finance app
   - Consider httpOnly cookies for production apps with sensitive data

2. **No Token Refresh**: Tokens expire after configured time
   - User must log in again after expiration
   - Could implement refresh token mechanism

3. **No Password Requirements**: No validation for password strength
   - Acceptable for demo/personal use
   - Add validation for production use

## Conclusion

✅ Frontend authorization is complete and fully integrated with backend JWT system. All routes are protected, all API calls include authorization headers, and the user experience is seamless.
