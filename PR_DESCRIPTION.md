# chore: Add Checkstyle Configuration, Improve Dev Tools, and Enhance JWT Security

### 📋 Overview

This PR introduces code quality tooling with Checkstyle integration, improves development workflow with bash scripts, enhances JWT secret handling for better security, and adds comprehensive test scripts for various game scenarios.

### ✨ What's Changed

#### New Features

- **Checkstyle Integration** - Added Checkstyle plugin to Gradle build with custom configuration for code quality checks
- **Bash Backend Runner** - New cross-platform bash script (`run-backend.sh`) for Unix/Linux/macOS environments with Java 21 detection and Supabase management
- **Test Scripts Suite** - Comprehensive bash and Python test scripts for automated testing of game scenarios:
  - Bot move testing (`test-bot-move.sh`)
  - PvP match testing (`test-pvp-match.sh`)
  - PvP WebSocket testing (`test-pvp-websocket.sh`)
  - Bot win and ranking verification (`test-win-bot-and-check-ranking.sh`)
  - WebSocket client utility (`ws_client.py`)

#### Core Components

**New Services**

None

**New Controllers**

None

**New Exceptions**

None

**New DTOs/Models**

None

#### Improvements

**Database & Performance**

- Added Redis port mapping in Docker Compose for external access (default port 6380)

**Error Handling**

None

**Security & Validation**

- **JWT Secret Security Enhancement** - Removed hardcoded default JWT secret from `application.properties` and `docker-compose.yml`
  - JWT secret now must be explicitly set via `JWT_SECRET` environment variable
  - For local development, if not set, a random secret will be generated (but changes on each restart)
  - Updated JWT token provider tests to include Environment mock for proper test isolation

**Code Quality**

- Added Checkstyle plugin to Gradle build (version 10.12.5)
- Configured Checkstyle with custom rules (120 char line length, proper formatting, etc.)
- Checkstyle tasks disabled by default (can be enabled when needed)
- Fixed SCSS syntax error in `game.component.scss` (missing closing brace)

**Development Tools**

- **Enhanced PowerShell Script** (`run-backend.ps1`):
  - Increased Supabase startup timeout from 30 to 300 seconds
  - Added progress monitoring with 10-second intervals
  - Improved Docker image download detection and handling
  - Better timeout handling when Docker images are being downloaded
  - Extended PostgreSQL readiness check timeout from 30 to 60 seconds
  - Added helpful hints for troubleshooting Supabase startup issues

### 🧪 Testing

- Updated `JwtTokenProviderTest` to include Environment mock dependency
- Added Mockito extension for proper test setup
- All test constructors now properly inject Environment mock

### 🗄️ Database Changes

None - no schema changes or migrations

### 📦 Files Changed

- Modified: 6 files (build.gradle, run-backend.ps1, application.properties, JwtTokenProviderTest.java, docker-compose.yml, game.component.scss)
- Added: 10 files (checkstyle config, bash scripts, test scripts, Python WebSocket client, documentation)
- Total: 1718 insertions, 20 deletions

### 🔍 Migration Notes

**Important:** This PR changes JWT secret handling. Before deploying:

1. **For production/docker environments:** Ensure `JWT_SECRET` environment variable is explicitly set
2. **For local development:** If `JWT_SECRET` is not set, a random secret will be generated on each restart (tokens will be invalidated)
3. **For existing deployments:** Set `JWT_SECRET` environment variable before deploying this change to avoid token invalidation

### 🔄 Breaking Changes

**JWT Secret Configuration Change**

- The default hardcoded JWT secret has been removed
- All environments (except local dev with localhost datasource) must now explicitly set `JWT_SECRET` environment variable
- Existing tokens will be invalidated if `JWT_SECRET` is not set or changed

### ✅ Checklist

- [x] Checkstyle configuration added and tested
- [x] JWT secret handling improved for security
- [x] Test scripts added for various game scenarios
- [x] Bash backend runner script created
- [x] PowerShell script enhanced with better timeout handling
- [x] Docker Compose updated with Redis port mapping
- [x] Tests updated to work with new JWT provider signature
- [x] SCSS syntax error fixed
- [x] Documentation updated (migration notes)
