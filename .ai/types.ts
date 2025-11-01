import type { Database } from './database.types';

type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[];

type DatabasePublic = Database['public'];
type UsersTable = DatabasePublic['Tables']['users'];
type GamesTable = DatabasePublic['Tables']['games'];
type MovesTable = DatabasePublic['Tables']['moves'];
type PlayerRankingsView = DatabasePublic['Views']['player_rankings'];

type GameTypeEnum = DatabasePublic['Enums']['game_type_enum'];
type GameStatusEnum = DatabasePublic['Enums']['game_status_enum'];
type BotDifficultyEnum = DatabasePublic['Enums']['bot_difficulty_enum'];
type PlayerSymbolEnum = DatabasePublic['Enums']['player_symbol_enum'];

type UsersRow = UsersTable['Row'];
type GamesRow = GamesTable['Row'];
type MovesRow = MovesTable['Row'];
type PlayerRankingsRow = PlayerRankingsView['Row'];

type BoardSize = 3 | 4 | 5;
type BoardState = string[][];

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}

interface ApiErrorResponse {
  error: {
    code: string;
    message: string;
    details?: Json;
  };
  timestamp: string;
  status: 'error';
}

interface ApiSuccessResponse<T> {
  data: T;
  timestamp: string;
  status: 'success';
}

interface MessageResponse {
  message: string;
}

export namespace AuthDTOs {
  export interface RegisterRequest {
    email: string;
    password: string;
    username: string;
  }

  export interface RegisterResponse {
    userId: string;
    username: string;
    email: string;
    isGuest: false;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    authToken: string;
  }

  export interface LoginRequest {
    email: string;
    password: string;
  }

  export interface LoginResponse {
    userId: string;
    username: string;
    email: string;
    isGuest: false;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    authToken: string;
  }

  export interface LogoutResponse extends MessageResponse {}

  export interface UserProfileResponse {
    userId: number;
    username: string | null;
    isGuest: boolean;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    createdAt: string;
    lastSeenAt: string | null;
  }
}

export namespace GuestDTOs {
  export interface GuestRequest {
    ipAddress?: string;
  }

  export interface GuestResponse {
    userId: number;
    isGuest: true;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    createdAt: string;
  }
}

export namespace UserDTOs {
  export interface UpdateUserRequest {
    username?: string;
  }

  export interface UpdateUserResponse {
    userId: number;
    username: string | null;
    isGuest: boolean;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    updatedAt: string;
  }

  export interface LastSeenResponse extends MessageResponse {
    lastSeenAt: string;
  }

  export interface UserProfileResponse {
    userId: number;
    username: string | null;
    isGuest: boolean;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    createdAt: string;
  }

  export interface PlayerInfo {
    userId: number;
    username: string | null;
    isGuest: boolean;
  }

  export interface WinnerInfo {
    userId: number;
    username: string | null;
  }
}

export namespace GameDTOs {
  export interface CreateGameRequest {
    gameType: GameTypeEnum;
    boardSize: BoardSize;
    botDifficulty?: BotDifficultyEnum;
  }

  export interface CreateGameResponse {
    gameId: number;
    gameType: GameTypeEnum;
    boardSize: BoardSize;
    player1Id: number;
    player2Id: number | null;
    botDifficulty: BotDifficultyEnum | null;
    status: GameStatusEnum;
    currentPlayerSymbol: PlayerSymbolEnum | null;
    createdAt: string;
    boardState: BoardState;
  }

  export interface GameListItem {
    gameId: number;
    gameType: GameTypeEnum;
    boardSize: BoardSize;
    status: GameStatusEnum;
    player1Username: string | null;
    player2Username: string | null;
    winnerUsername: string | null;
    botDifficulty: BotDifficultyEnum | null;
    totalMoves: number;
    createdAt: string;
    lastMoveAt: string | null;
    finishedAt: string | null;
  }

  export interface GameListResponse extends PaginatedResponse<GameListItem> {}

  export interface GameDetailResponse {
    gameId: number;
    gameType: GameTypeEnum;
    boardSize: BoardSize;
    player1: UserDTOs.PlayerInfo;
    player2: UserDTOs.PlayerInfo | null;
    winner: UserDTOs.WinnerInfo | null;
    botDifficulty: BotDifficultyEnum | null;
    status: GameStatusEnum;
    currentPlayerSymbol: PlayerSymbolEnum | null;
    lastMoveAt: string | null;
    createdAt: string;
    updatedAt: string;
    finishedAt: string | null;
    boardState: BoardState;
    totalMoves: number;
    moves: MoveDTOs.MoveListItem[];
  }

  export interface UpdateGameStatusRequest {
    status: 'abandoned' | 'finished' | 'draw';
  }

  export interface UpdateGameStatusResponse {
    gameId: number;
    status: GameStatusEnum;
    updatedAt: string;
  }

  export interface BoardStateResponse {
    boardState: BoardState;
    boardSize: BoardSize;
    totalMoves: number;
    lastMove: {
      row: number;
      col: number;
      playerSymbol: PlayerSymbolEnum;
      moveOrder: number;
    } | null;
  }
}

export namespace MoveDTOs {
  export interface CreateMoveRequest {
    row: number;
    col: number;
    playerSymbol: PlayerSymbolEnum;
  }

  export interface CreateMoveResponse {
    moveId: number;
    gameId: number;
    row: number;
    col: number;
    playerSymbol: PlayerSymbolEnum;
    moveOrder: number;
    createdAt: string;
    boardState: BoardState;
    gameStatus: GameStatusEnum;
    winner: UserDTOs.WinnerInfo | null;
  }

  export interface BotMoveResponse {
    moveId: number;
    gameId: number;
    row: number;
    col: number;
    playerSymbol: PlayerSymbolEnum;
    moveOrder: number;
    createdAt: string;
    boardState: BoardState;
    gameStatus: GameStatusEnum;
    winner: UserDTOs.WinnerInfo | null;
  }

  export interface MoveListItem {
    moveId: number;
    row: number;
    col: number;
    playerSymbol: PlayerSymbolEnum;
    moveOrder: number;
    playerId: number | null;
    playerUsername: string | null;
    createdAt: string;
  }

  export type MoveListResponse = MoveListItem[];
}

export namespace MatchmakingDTOs {
  export interface MatchmakingQueueRequest {
    boardSize: BoardSize;
  }

  export interface MatchmakingQueueResponse extends MessageResponse {
    estimatedWaitTime: number;
  }

  export interface LeaveQueueResponse extends MessageResponse {}

  export interface ChallengeRequest {
    boardSize: BoardSize;
  }

  export interface ChallengeResponse {
    gameId: number;
    gameType: 'pvp';
    boardSize: BoardSize;
    player1Id: number;
    player2Id: number;
    status: GameStatusEnum;
    createdAt: string;
  }
}

export namespace RankingDTOs {
  export interface RankingItem {
    rankPosition: number;
    userId: number;
    username: string;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    createdAt: string;
  }

  export interface RankingListResponse extends PaginatedResponse<RankingItem> {}

  export interface RankingDetailResponse {
    rankPosition: number;
    userId: number;
    username: string;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
    createdAt: string;
  }

  export interface RankingAroundItem {
    rankPosition: number;
    userId: number;
    username: string;
    totalPoints: number;
    gamesPlayed: number;
    gamesWon: number;
  }

  export type RankingAroundResponse = RankingAroundItem[];
}

export namespace WebSocketDTOs {
  export type WebSocketMessageType =
    | 'MOVE'
    | 'SURRENDER'
    | 'PING'
    | 'PONG'
    | 'MOVE_ACCEPTED'
    | 'MOVE_REJECTED'
    | 'OPPONENT_MOVE'
    | 'GAME_UPDATE'
    | 'TIMER_UPDATE'
    | 'GAME_ENDED';

  export interface BaseWebSocketMessage<T extends WebSocketMessageType> {
    type: T;
    payload: Json;
  }

  export interface MoveMessage extends BaseWebSocketMessage<'MOVE'> {
    payload: {
      row: number;
      col: number;
      playerSymbol: PlayerSymbolEnum;
    };
  }

  export interface SurrenderMessage
    extends BaseWebSocketMessage<'SURRENDER'> {
    payload: Record<string, never>;
  }

  export interface PingMessage extends BaseWebSocketMessage<'PING'> {
    payload: {
      timestamp: string;
    };
  }

  export interface PongMessage extends BaseWebSocketMessage<'PONG'> {
    payload: {
      timestamp: string;
    };
  }

  export interface MoveAcceptedMessage
    extends BaseWebSocketMessage<'MOVE_ACCEPTED'> {
    payload: {
      moveId: number;
      row: number;
      col: number;
      playerSymbol: PlayerSymbolEnum;
      boardState: BoardState;
      currentPlayerSymbol: PlayerSymbolEnum | null;
      nextMoveAt: string;
    };
  }

  export interface MoveRejectedMessage
    extends BaseWebSocketMessage<'MOVE_REJECTED'> {
    payload: {
      reason: string;
      code: string;
    };
  }

  export interface OpponentMoveMessage
    extends BaseWebSocketMessage<'OPPONENT_MOVE'> {
    payload: {
      row: number;
      col: number;
      playerSymbol: PlayerSymbolEnum;
      boardState: BoardState;
      currentPlayerSymbol: PlayerSymbolEnum | null;
      nextMoveAt: string;
    };
  }

  export interface GameUpdateMessage
    extends BaseWebSocketMessage<'GAME_UPDATE'> {
    payload: {
      gameId: number;
      status: GameStatusEnum;
      winner: UserDTOs.WinnerInfo | null;
      boardState: BoardState;
    };
  }

  export interface TimerUpdateMessage
    extends BaseWebSocketMessage<'TIMER_UPDATE'> {
    payload: {
      remainingSeconds: number;
      currentPlayerSymbol: PlayerSymbolEnum | null;
    };
  }

  export interface GameEndedMessage
    extends BaseWebSocketMessage<'GAME_ENDED'> {
    payload: {
      gameId: number;
      status: GameStatusEnum;
      winner: UserDTOs.WinnerInfo | null;
      finalBoardState: BoardState;
      totalMoves: number;
    };
  }

  export type WebSocketMessage =
    | MoveMessage
    | SurrenderMessage
    | PingMessage
    | PongMessage
    | MoveAcceptedMessage
    | MoveRejectedMessage
    | OpponentMoveMessage
    | GameUpdateMessage
    | TimerUpdateMessage
    | GameEndedMessage;
}

export namespace HealthDTOs {
  export type HealthStatus = 'UP' | 'DOWN';

  export interface HealthComponent {
    status: HealthStatus;
  }

  export interface HealthResponse {
    status: HealthStatus;
    components: {
      db: HealthComponent;
      redis: HealthComponent;
      websocket: HealthComponent;
    };
  }
}

export type {
  BoardSize,
  BoardState,
  GameTypeEnum,
  GameStatusEnum,
  BotDifficultyEnum,
  PlayerSymbolEnum,
  PaginatedResponse,
  ApiErrorResponse,
  ApiSuccessResponse,
  MessageResponse,
};

