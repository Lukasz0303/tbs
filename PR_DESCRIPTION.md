## Basic Implementation: Game State Transfer Using WebSocket

### Overview
Implements WebSocket-based real-time game state transfer for PvP matches, including a matchmaking system with Redis queue management and comprehensive game session handling.

### Key Features

#### WebSocket Infrastructure
- WebSocket endpoint at `/ws/game/{gameId}` with JWT authentication
- Real-time bidirectional communication for game moves, ping/keep-alive, and surrender
- Session management with reconnection support and timeout handling
- Message storage service for offline message delivery

#### Matchmaking System
- REST API endpoints for queue management (`POST/DELETE /api/matching/queue`)
- Direct player challenges (`POST /api/matching/challenge/{userId}`)
- Redis-based queue with board size matching and estimated wait time
- Queue status tracking with player status enumeration

#### Service Layer Improvements
- `MatchmakingService` - Queue management and player matching logic
- `RedisService` - Redis operations for matchmaking queues
- `WebSocketGameService` - Game state management over WebSocket
- `WebSocketMessageStorageService` - Offline message persistence
- `GameValidationService` - Game state validation
- `TurnDeterminationService` & `TurnValidationService` - Turn management
- `MoveCreationService` - Move creation logic extraction
- `BotUserService` - Bot user management

#### Architecture Changes
- WebSocket configuration with CORS support and authentication interceptor
- Enhanced JWT authentication filter for WebSocket connections
- New exception types for matchmaking scenarios
- String-to-enum converters for request parameters
- Database migration fixing `vs_bot` check constraint

#### Testing & Documentation
- Unit tests for matchmaking, Redis service, and converters
- Test scripts for PvP match and WebSocket scenarios
- Comprehensive WebSocket usage examples and documentation
- WebSocket documentation and test controllers for API exploration

### Technical Details
- **60 files changed**: 5,792 insertions, 172 deletions
- Spring WebSocket integration with STOMP alternative
- Redis integration for scalable matchmaking
- Message types: MOVE, PING, SURRENDER with JSON payloads
- Session timeout handling (60s ping, 10s move timeout)
- Automatic cleanup of expired sessions

### Breaking Changes
None

