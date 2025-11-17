import { Game, GameStatus, GameType, BotDifficulty, PlayerSymbol } from './game.model';

export interface GameResponse {
  gameId: number;
  gameType: GameType;
  boardSize: 3 | 4 | 5;
  status: GameStatus;
  boardState: string | number[][] | { state: (PlayerSymbol | null)[][] };
  player1Id?: number;
  player2Id?: number | null;
  player1?: { userId: number };
  player2?: { userId: number } | null;
  botDifficulty?: BotDifficulty | null;
  currentPlayerSymbol: PlayerSymbol | null;
  winnerId?: number | null;
  winner?: { userId: number; username: string } | null;
  lastMoveAt?: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt?: string | null;
  totalMoves: number;
}

export interface CreateGameRequest {
  gameType: GameType;
  botDifficulty?: BotDifficulty;
  boardSize: 3 | 4 | 5;
}

export interface MakeMoveRequest {
  row: number;
  col: number;
  playerSymbol: PlayerSymbol;
}

export interface MakeMoveResponse extends GameResponse {}

