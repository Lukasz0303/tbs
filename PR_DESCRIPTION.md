# feat: Implement Rankings API Versioning and Cache Management

## 📋 Overview

This PR implements API versioning for the rankings endpoints by adding automatic redirects from `/api/rankings` to `/api/v1/rankings`, introduces a cache management endpoint for clearing rankings cache, and adds a comprehensive backend automation script. The changes ensure backward compatibility while establishing a clear API versioning strategy.

## ✨ What's Changed

### New Features

- **API Versioning** - Automatic redirect from `/api/rankings` to `/api/v1/rankings` for backward compatibility
- **Cache Management Endpoint** - `DELETE /api/v1/rankings/cache` for clearing Redis cache entries
- **Backend Automation Script** - PowerShell script (`run-backend.ps1`) for automated backend startup, restart, and status checks
- **Cache Integration** - Automatic cache clearing on backend startup/restart

### Core Components

#### New Configuration

- `RankingApiRedirectInterceptor` - Interceptor that redirects old `/api/rankings` endpoints to `/api/v1/rankings`
- `WebMvcConfig` - Web MVC configuration that registers the redirect interceptor

#### Enhanced Services

- `RankingServiceImpl` - Added `clearRankingsCache()` method with `@CacheEvict` annotation
- `PointsService` - Enhanced with async ranking refresh after point awards

#### Enhanced Controllers

- `RankingController` - Added `DELETE /api/v1/rankings/cache` endpoint for cache management

#### Enhanced Exception Handling

- `GlobalExceptionHandler` - Added handling for `UserNotInRankingException` and improved error responses

### Improvements

#### API & Backward Compatibility

- Automatic redirect from legacy `/api/rankings` endpoints to versioned `/api/v1/rankings` endpoints
- HTTP 301 (Moved Permanently) redirects for proper API versioning
- Query parameters and path variables preserved during redirect

#### Cache Management

- Redis cache clearing endpoint for administrative operations
- Automatic cache clearing on backend startup/restart via PowerShell script
- Cache invalidation for all ranking-related cache entries (`rankings`, `rankingDetail`, `rankingsAround`)
- Idempotent cache clearing operation

#### Developer Experience

- Comprehensive PowerShell script (`run-backend.ps1`) with multiple commands:
  - `start` - Automated backend startup with Supabase, Redis, and Spring Boot
  - `restart` - Backend restart with database migrations and cache clearing
  - `status` - Status check for all services (Supabase, Redis, Backend, Java)
  - `logs` - Display recent backend logs
  - `stop` - Stop backend processes
- Automatic service health checks and port verification
- Detailed error messages and troubleshooting hints
- Java 21 detection and environment setup

#### Documentation

- Updated ranking API documentation with versioned endpoints
- Added cache management endpoint documentation
- Updated README with new API endpoints and cache management information

## 🧪 Testing

### Unit Tests

- `RankingControllerTest` - Tests for ranking endpoints including cache clearing
- `RankingServiceImplTest` - Tests for ranking service methods including cache eviction

### Integration Tests

- PowerShell test script (`test-win-bot-and-check-ranking.ps1`) for ranking verification
- Cache clearing integration in backend startup script

## 🗄️ Database Changes

No database migrations required. All changes are code-level improvements and configuration updates.

## 📦 Files Changed

- **9 files changed**
- **2,686 insertions(+), 10 deletions(-)**

### New Files

- `backend/src/main/java/com/tbs/config/RankingApiRedirectInterceptor.java` - Redirect interceptor
- `backend/src/main/java/com/tbs/config/WebMvcConfig.java` - Web MVC configuration
- `backend/run-backend.ps1` - Backend automation script

### Modified Files

- `backend/src/main/java/com/tbs/controller/RankingController.java` - Added cache clearing endpoint
- `backend/src/main/java/com/tbs/service/RankingServiceImpl.java` - Added cache clearing method
- `backend/src/main/java/com/tbs/service/PointsService.java` - Enhanced with async ranking refresh
- `backend/src/main/java/com/tbs/exception/GlobalExceptionHandler.java` - Added exception handling
- `README.md` - Updated with new API endpoints and cache management
- `.ai/implementation-plans/ranking/*.md` - Updated API documentation

## 🔍 Migration Notes

No database migrations required. The changes are backward compatible:

1. **API Redirects**: Old `/api/rankings` endpoints automatically redirect to `/api/v1/rankings`
2. **Cache**: Existing cache entries remain valid until manually cleared or TTL expires
3. **Scripts**: The new `run-backend.ps1` script can be used immediately without configuration changes

## 🔄 Breaking Changes

**None** - All changes are backward compatible:

- Old `/api/rankings` endpoints automatically redirect to `/api/v1/rankings`
- Existing clients continue to work with automatic redirects
- Cache clearing is an optional administrative operation
- Backend script is an addition, not a replacement for existing workflows

## ✅ Checklist

- [x] API versioning with redirect interceptor implemented
- [x] Cache management endpoint added
- [x] Backend automation script created
- [x] Automatic cache clearing on startup/restart
- [x] Exception handling enhanced
- [x] Documentation updated
- [x] Unit tests added
- [x] Backward compatibility maintained
- [x] Error handling improved
- [x] Code quality maintained
