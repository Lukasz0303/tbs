## feat: Implement User Information Management API

### 📋 Overview

Implements a comprehensive user information management system with profile retrieval, update capabilities, rate limiting, and caching. Adds user profile endpoints with proper authorization, rate limiting protection, and Redis-based caching for improved performance.

### ✨ What's Changed

#### New Features

- **User Profile Management API** - Full CRUD operations for user profiles with public/private access control
- **Rate Limiting System** - Redis-based rate limiting for API endpoints to prevent abuse
- **User Profile Caching** - Redis caching for user profiles to improve response times
- **IP Address Detection** - Service for extracting client IP addresses from requests (supports proxy headers)
- **Last Seen Tracking** - Endpoint for updating user's last seen timestamp for matchmaking

#### Core Components

**New Services**

- `UserService` - Handles user profile retrieval, updates, and caching with guest/registered user distinction
- `RateLimitingService` - Redis-based rate limiting with configurable limits and time windows
- `IpAddressService` - Extracts client IP addresses from HTTP requests, supporting X-Forwarded-For and X-Real-IP headers

**New Controllers**

- `UserController` - REST endpoints for user profile management
  - `GET /api/users/{userId}` - Retrieve user profile (public for registered users, private for guests)
  - `POST /api/users/{userId}/last-seen` - Update last seen timestamp (authenticated users only)
  - `PUT /api/users/{userId}` - Update user profile (username updates with validation)

**Enhanced Controllers**

- `AuthController` - Added endpoint for current user profile
  - `GET /api/auth/profile` - Get current authenticated user's profile

**New Exceptions**

- `RateLimitExceededException` - Rate limit violations with remaining requests and reset time information
- `ConflictException` - Conflict errors (e.g., username already exists)

#### Improvements

**Database & Performance**

- Redis caching for user profiles with automatic cache invalidation on updates
- Optimized user profile queries with proper indexing
- Rate limiting stored in Redis for distributed systems support

**Error Handling**

- Enhanced GlobalExceptionHandler with rate limit exception handling
- Better validation messages for username conflicts
- Proper error responses with rate limit information (remaining requests, reset time)

**Security & Validation**

- Rate limiting per endpoint with configurable limits (profile: 100/min, last-seen: 30/min, update: 10/min)
- Authorization checks ensuring users can only update their own profiles
- Guest profile access control (only accessible by owner)
- IP-based rate limiting for public endpoints

**Code Quality**

- Clean separation of concerns with dedicated services
- Proper transaction management for user updates
- Cache eviction on profile updates
- Comprehensive OpenAPI documentation for all endpoints

### 🧪 Testing

- No new unit tests added in this PR (existing test coverage maintained)

### 🗄️ Database Changes

**New Migrations**

- `20251101150000_add_email_and_password_to_users.sql` - Adds email and password_hash columns to users table for local development authentication
- `20251101160000_fix_users_registered_check_constraint.sql` - Updates constraint to support registered users without Supabase Auth
- `20251101170000_change_ip_address_to_text.sql` - Changes ip_address column type from inet to text for Hibernate compatibility

**Schema Updates**

- Added `email` and `password_hash` columns to `users` table
- Changed `ip_address` column type from `inet` to `text`
- Updated `users_registered_check` constraint to support local authentication
- Added index on `email` column for registered users

### 📦 Files Changed

- **Added**: 3 new services (UserService, RateLimitingService, IpAddressService)
- **Added**: 1 new controller (UserController)
- **Added**: 2 new exceptions (RateLimitExceededException, ConflictException)
- **Added**: 6 new DTOs (UserProfileResponse, UpdateUserRequest, UpdateUserResponse, LastSeenResponse, PlayerInfo, WinnerInfo)
- **Modified**: AuthController (added profile endpoint)
- **Modified**: 3 database migrations
- **Total**: 130 files changed, 13,009 insertions(+), 82 deletions(-)

### 🔍 Migration Notes

**Database migrations required** - Run the following migrations in order:

1. `20251101150000_add_email_and_password_to_users.sql` - Adds email and password fields
2. `20251101160000_fix_users_registered_check_constraint.sql` - Updates constraint
3. `20251101170000_change_ip_address_to_text.sql` - Changes IP address type

**Configuration updates**:

- Add rate limiting configuration to `application.properties`:
  - `app.rate-limit.profile=100` (requests per minute)
  - `app.rate-limit.last-seen=30` (requests per minute)
  - `app.rate-limit.update=10` (requests per minute)

**Redis requirements**:

- Redis must be running for rate limiting and caching to work
- Cache configuration: `userProfile` cache with automatic expiration

### 🔄 Breaking Changes

None - This is a new feature addition that doesn't break existing functionality.

### ✅ Checklist

- [x] User profile management API implemented
- [x] Rate limiting system integrated
- [x] Redis caching configured
- [x] Authorization checks implemented
- [x] Guest/registered user distinction handled
- [x] Database migrations created and tested
- [x] OpenAPI documentation updated
- [x] Error handling enhanced
- [x] IP address extraction implemented
- [x] Configuration properties added

