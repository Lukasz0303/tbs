# feat: UI/UX Improvements, Internationalization, and Matchmaking Enhancements

### 📋 Overview

This PR introduces comprehensive UI/UX improvements, full internationalization support (Polish/English), enhanced matchmaking queue functionality with player scores, and various frontend/backend refinements. The changes include language switching, improved styling across components, better state management, responsive design fixes for small screens, and configuration updates for development and production environments.

### ✨ What's Changed

#### New Features

- **Language Selection** - Added language switcher (Polish/English) on home page with flag icons
- **Player Scores in Matchmaking** - Display player scores/ranking in matchmaking queue
- **Audio Settings Quick Toggle** - Simplified navbar with mute/unmute all sounds button
- **Profile Navigation** - Added profile link in navbar replacing logout button
- **Internationalized Legal Pages** - Privacy Policy and Terms of Service now fully translated
- **Win Rate Display** - Added win rate calculation and display in profile component

#### Core Components

**Improvements**

**Database & Performance**

- Added score field to `PlayerQueueStatus` DTO for displaying player rankings
- Enhanced `MatchmakingService` to include player total points in queue status responses
- Optimized matchmaking queue polling with page visibility detection to reduce unnecessary requests

**Error Handling**

- Improved error handling in matchmaking queue component with better state management
- Enhanced queue status change detection to prevent unnecessary UI updates
- Added validation for score formatting and display

**Security & Validation**

- Increased rate limit for profile updates from 10 to 30 requests per minute
- Added default JWT secret for local development (with warning for production)
- Updated CORS configuration to include localhost origins for development

**Code Quality**

- **Refactored Navbar Component** - Removed complex audio settings dialog, simplified to mute/unmute toggle
- **Enhanced Matchmaking Queue** - Added page visibility listener, optimized polling, improved change detection
- **Improved Profile Component** - Migrated from BehaviorSubject to signals, better loading states, added win rate calculation
- **Legal Pages Refactoring** - Converted hardcoded text to use translation service
- **Translation Service Expansion** - Added 100+ new translation keys for legal pages, profile, queue, and UI elements
- **Audio Settings Service** - Added `toggleMuteAll()` method for simplified audio control

**UI/UX Improvements**

- Extensive SCSS updates across game components (board, bot info, user profile)
- Enhanced styling for matchmaking queue, leaderboard, profile, and auth components
- Improved visual feedback and animations in matchmaking queue
- **Responsive Design Fixes** - Fixed layout issues on small screens (mobile devices) with media queries for max-width: 480px, 400px, and 768px breakpoints
- Better responsive design and spacing throughout the application
- Updated game result dialog with new translations
- Enhanced avatar selector and edit username dialog styling

**Configuration Changes**

- Updated `docker-compose.yml` - Changed Redis port mapping from 6379 to 6380
- Updated `frontend/Dockerfile` - Changed description labels to "Kółko i krzyżyk"
- Updated `angular.json` - Increased bundle size budgets (initial: 500kb→1.5mb, component styles: 7kb→15kb)
- Added `flag-icons` package (v7.5.0) for language selector flags
- Updated `application.properties` - Added localhost to CORS origins, default JWT secret for dev, increased update rate limit

### 🧪 Testing

- Updated `MatchingControllerTest` to reflect changes in `PlayerQueueStatus` DTO structure

### 🗄️ Database Changes

No database migrations required. Changes are limited to DTO structure and service logic.

### 📦 Files Changed

- **Modified**: 50 files
- **Total**: 4,271 insertions(+), 603 deletions(-)

**Backend (7 files)**
- `UserController.java` - Rate limit documentation updates (10→30 requests/min)
- `MatchmakingService.java` - Added score to queue status responses
- `PlayerQueueStatus.java` - Added score field to DTO
- `UpdateAvatarRequest.java` - Line ending normalization
- `application.properties` - CORS, JWT secret, rate limit updates
- `MatchingControllerTest.java` - Test updates for new DTO structure
- `UserServiceTest.java` - Line ending normalization

**Frontend (43 files)**
- **Components**: game-board, game-bot-info, game-user-profile, game-result-dialog, user-rank-card, navbar, avatar-selector, edit-username-dialog (styling and functionality improvements)
- **Features**: home (language selector), matchmaking-queue (score display, optimizations), matchmaking, profile (signals migration, win rate), leaderboard, legal pages (internationalization), game-options, auth components (styling)
- **Services**: translate (100+ new keys), matchmaking, audio-settings
- **Layouts**: main-layout
- **Configuration**: angular.json, package.json, Dockerfile, index.html, styles.css

**Docker (2 files)**
- `docker-compose.yml` - Redis port mapping change
- `frontend/Dockerfile` - Description label updates

### 🔍 Migration Notes

**Configuration Updates:**

- **Rate Limits**: Profile update rate limit increased from 10 to 30 requests/minute
- **CORS**: Localhost origins (http://localhost:4200, http://127.0.0.1:4200) added to default allowed origins
- **JWT Secret**: Default value added for local development (production should still use environment variable)
- **Redis Port**: Default port mapping changed from 6379 to 6380 in docker-compose

**Frontend Dependencies:**

- New package: `flag-icons@^7.5.0` for language selector flags
- Bundle size budgets increased to accommodate new styling and features

**Translation Keys:**

- 100+ new translation keys added for legal pages, profile features, queue enhancements, and UI elements
- Application title changed from "World at War: Turn-Based Strategy" to "Tic-Tac-Toe" / "Kółko i krzyżyk"

### 🔄 Breaking Changes

**None** - All changes are backward compatible. The new `score` field in `PlayerQueueStatus` is optional and handled gracefully in the frontend.

### ✅ Checklist

- [x] Language switcher implemented and tested
- [x] Player scores displayed in matchmaking queue
- [x] Translation service expanded with new keys
- [x] Legal pages internationalized
- [x] Navbar simplified with mute/unmute toggle
- [x] Profile component migrated to signals
- [x] Matchmaking queue optimizations implemented
- [x] Styling improvements across components
- [x] Responsive design fixes for small screens implemented
- [x] Rate limits updated
- [x] Configuration files updated
- [x] Bundle size budgets adjusted
- [x] No breaking changes introduced
