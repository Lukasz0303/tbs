# refactor: Migrate JWT Authentication from Authorization Header to HttpOnly Cookies

### 📋 Overview

This PR refactors the authentication system to use httpOnly cookies for JWT token storage instead of Authorization headers. This improves security by preventing XSS attacks and simplifies token management on the frontend. Additionally, it introduces event-driven architecture for WebSocket notifications and optimizes matchmaking queue operations with atomic Redis scripts.

### ✨ What's Changed

#### New Features

- **HttpOnly Cookie Authentication** - JWT tokens are now stored in secure, httpOnly cookies instead of localStorage/Authorization headers
- **Event-Driven WebSocket Notifications** - Move creation notifications now use Spring Application Events with async listeners instead of CompletableFuture
- **Atomic Queue Operations** - Matchmaking queue operations use Redis Lua scripts for atomic check-and-add operations

#### Core Components

**New Services**

- `CookieTokenExtractor` - Utility class for extracting JWT tokens from httpOnly cookies
- `MoveOperationContext` - Context object consolidating move-related service dependencies for better dependency management

**New Events & Listeners**

- `MoveCreatedEvent` - Event published when a move is created in PvP games
- `MoveCreatedEventListener` - Async transactional event listener that handles WebSocket notifications after move commits

**Modified Controllers**

- `AuthController` - Sets httpOnly cookies on login/register, clears cookies on logout
  - `POST /api/v1/auth/login` - Now sets authToken cookie instead of returning token in response
  - `POST /api/v1/auth/register` - Now sets authToken cookie instead of returning token in response
  - `POST /api/v1/auth/logout` - Now clears authToken cookie
- `GuestController` - Sets httpOnly cookies for guest sessions
  - Enhanced Swagger documentation with request/response examples
- `GameController` - Minor updates for cookie-based authentication

**Modified DTOs**

- `LoginResponse` - Removed `authToken` field, changed `userId` from `String` to `Long`
- `RegisterResponse` - Removed `authToken` field, changed `userId` from `String` to `Long`
- `GuestResponse` - Removed `authToken` field

#### Improvements

**Security & Validation**

- JWT tokens now stored in httpOnly cookies with `SameSite=Lax` policy, preventing XSS token theft
- Configurable secure cookie flag via `app.cookie.secure` property
- Enhanced token validation with better error handling in `JwtAuthenticationFilter`
- WebSocket authentication now reads tokens from cookies instead of query parameters/headers

**Database & Performance**

- Redis queue operations optimized with atomic Lua scripts (`addToQueueIfNotActive`)
- Matchmaking service now checks for active games atomically before adding to queue
- Improved error handling for Redis timeouts and connection failures

**Code Quality**

- `MoveService` refactored to use dependency injection via `MoveOperationContext` instead of 10+ individual service dependencies
- WebSocket notifications moved from `CompletableFuture.runAsync()` to Spring Application Events for better transaction handling
- Removed token management logic from frontend services (no more localStorage manipulation)
- Frontend HTTP interceptor simplified - only sets `withCredentials: true`, no Authorization header needed
- WebSocket service simplified - no token parameter required in `connect()` method

**Error Handling**

- Better null checks and validation in `RedisService` methods
- Enhanced error logging in WebSocket authentication interceptor
- Improved error messages for token extraction failures

### 🧪 Testing

- Updated `GuestControllerTest` to reflect cookie-based authentication changes
- Updated `AuthServiceLoginRegisterTest` for new response structure (no token field)
- Updated `GuestServiceTest` - removed token-related assertions
- Frontend test updates in `game-banner.component.spec.ts` and `home.component.spec.ts`

### 🗄️ Database Changes

No database schema changes. All modifications are code-level improvements.

### 📦 Files Changed

- **Modified**: 33 files
- **Added**: 4 files (MoveCreatedEvent, MoveCreatedEventListener, MoveOperationContext, CookieTokenExtractor)
- **Total**: 710 insertions(+), 393 deletions(-)

### 🔍 Migration Notes

**Breaking Changes:**

1. **API Response Changes**: `LoginResponse`, `RegisterResponse`, and `GuestResponse` no longer include `authToken` field. Clients should rely on httpOnly cookies instead.

2. **Frontend Changes Required**: 
   - Remove all `localStorage.getItem('wow-auth-token')` and `localStorage.setItem('wow-auth-token', ...)` calls
   - Ensure all HTTP requests include `withCredentials: true` (handled by interceptor)
   - WebSocket connections no longer require token parameter

3. **Configuration**: Add `app.cookie.secure=true` in production environments with HTTPS

**Migration Steps:**

1. Deploy backend first - it will set cookies for all new login/register requests
2. Deploy frontend - it will stop using localStorage and rely on cookies
3. Existing users will need to log out and log back in to receive cookie-based tokens
4. Update any external API clients to handle cookies instead of Authorization headers

### 🔄 Breaking Changes

- **API Response Structure**: `LoginResponse`, `RegisterResponse`, and `GuestResponse` no longer contain `authToken` field
- **Frontend Token Management**: All localStorage token management removed - cookies are now used automatically
- **WebSocket Connection**: `WebSocketService.connect()` no longer accepts token parameter
- **UserId Type**: Changed from `String` to `Long` in auth response DTOs

### ✅ Checklist

- [x] HttpOnly cookies implemented for all authentication endpoints
- [x] Frontend updated to use cookies instead of localStorage
- [x] WebSocket authentication updated to read from cookies
- [x] Event-driven architecture implemented for move notifications
- [x] Redis atomic operations added for matchmaking
- [x] All tests updated and passing
- [x] Security improvements verified
- [x] Configuration properties added
- [x] Code refactoring completed (MoveService, dependency injection)

