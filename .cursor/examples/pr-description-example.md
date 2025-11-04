# Example PR Description Format

## feat: Implement Move Management and Bot Gameplay System

### 📋 Overview

Implements a complete move management system for Tic-Tac-Toe gameplay with AI bot integration, win/draw detection, move validation, and enhanced error handling.

### ✨ What's Changed

#### New Features

- **Move Management API** - Full CRUD operations for game moves with comprehensive validation
- **AI Bot System** - Three difficulty levels (Easy, Medium, Hard) with strategic algorithms
- **Game Logic Service** - Centralized validation, win/draw detection, and turn management
- **Bot Move Generation** - Intelligent move selection for optimal gameplay
- **Database Optimization** - Pessimistic locking for concurrent-safe move operations

#### Core Components

**New Services**

- `MoveService` - Handles move creation, validation, and game state updates
- `BotService` - AI move generation with difficulty-based strategies
- `GameLogicService` - Core game rules and validation logic

**New Controller**

- `MoveController` - REST endpoints for move operations
  - `GET /api/games/{gameId}/moves` - Retrieve move history
  - `POST /api/games/{gameId}/moves` - Create player move
  - `POST /api/games/{gameId}/bot-move` - Trigger bot move

**New Exceptions**

- `GameNotInProgressException` - Game state validation
- `InvalidMoveException` - Move validation errors
- `InvalidGameTypeException` - Game type validation

#### Improvements

**Database & Performance**

- Optimized queries with LEFT JOIN FETCH to eliminate N+1 problems
- Batch move counting for game lists
- Pessimistic locking for atomic move order assignment
- Removed unnecessary EAGER fetch strategies

**Error Handling**

- Enhanced exception handling in GlobalExceptionHandler
- Better validation messages for board size constraints
- Improved data integrity error messages (username/email conflicts)
- Added logging for edge cases in board state generation

**Security & Validation**

- Enhanced JWT token validation with null/empty checks
- Improved move validation with boundary checks
- Game status transition validation
- Proper authorization checks for move operations

**Code Quality**

- Introduced MoveMapper for clean DTO conversions
- Centralized game logic in dedicated service
- Improved transaction boundaries
- Better separation of concerns

### 🧪 Testing

- Added PowerShell test script (test-bot-move.ps1) for bot move scenarios
- Tests cover registration, game creation, player moves, and bot responses

### 🗄️ Database Changes

MoveRepository enhanced with:

- Pessimistic write locking for move ordering
- Batch query methods for move counts
- Optimized queries with explicit joins

### 📦 Files Changed

- Modified: 9 files (GameService, AuthService, repositories, security config, etc.)
- Added: 7 files (controllers, services, exceptions, mappers)
- Total: 147 insertions, 41 deletions

### 🔍 Migration Notes

No database migrations required. All changes are code-level improvements.

### ✅ Checklist

- [x] New features implemented and tested
- [x] Error handling enhanced
- [x] Code quality improved
- [x] Repository methods optimized
- [x] Security validations added
- [x] Documentation updated

