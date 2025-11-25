## feat: Harden auth limits and move timers

### 📋 Overview

Introduces Redis-backed throttling for login and registration endpoints and aligns the frontend PvP countdown with backend timestamps so reconnecting players see accurate timers.

### ✨ What's Changed

#### New Features

- Login and register endpoints now call `RateLimitingService` using client IPs from `IpAddressService`, blocking after 5/15 min (login) or 3/h (register).
- PvP countdown reuses a shared `MOVE_TIMEOUT_SECONDS` constant and derives `lastMoveAt` from `nextMoveAt`, keeping the UI clock in sync with the server.

#### Core Components

**New Services**

- None.

**New Controllers**

- `AuthController` injects rate limiting and IP services, reads `HttpServletRequest`, validates token generation via `Objects.requireNonNull`, and returns 429 with remaining quota metadata.

**New Exceptions**

- None.

**New DTOs/Models**

- None.

#### Improvements

**Database & Performance**

- None.

**Error Handling**

- Throws `RateLimitExceededException` with retry hints whenever limits are hit.

**Security & Validation**

- Adds `app.rate-limit.*` toggles to `application.properties` to adjust quotas per IP/account.

**Code Quality**

- Removes duplicated timeout literals from `GameComponent`.
- Replaces obsolete `.ai/prompts/19.txt` with refreshed auth documentation, prompts, and diagram brief.

### 🧪 Testing

- Not run (manual verification pending).

### 🗄️ Database Changes

- None.

### 📦 Files Changed

- `git diff HEAD --stat`: 5 files changed, 88 insertions(+), 174 deletions(-)
- Modified: `backend/src/main/java/com/tbs/controller/AuthController.java`, `backend/src/main/resources/application.properties`, `frontend/src/app/features/game/game.component.ts`
- Deleted: `.ai/prompts/19.txt`
- Added (untracked): `.ai/auth-spec.md`, `.ai/diagrams/auth.md`, `.ai/prompts/19_comfyui.txt`, `.ai/prompts/20_authorization_spec.txt`, `.ai/prompts/21_authorization_spec_verification.txt`, `.cursor/rules/mermaid-diagram-auth.mdc`

### 🔍 Migration Notes

- No Flyway migrations introduced.

### 🔄 Breaking Changes

- None.

### ✅ Checklist

- [x] Rate limiting wired to auth endpoints
- [x] Move timer sync in frontend
- [ ] Automated tests updated

