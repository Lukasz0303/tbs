export type GameModeId = 'guest' | 'bot' | 'pvp';

export interface GameMode {
  id: GameModeId;
  labelKey: string;
  descriptionKey: string;
  icon: string;
  route: string | null;
  availableForGuest: boolean;
  availableForRegistered: boolean;
}

export type GameType = 'vs_bot' | 'pvp';

export type BotDifficulty = 'easy' | 'medium' | 'hard';

export type GameStatus =
  | 'waiting'
  | 'in_progress'
  | 'finished'
  | 'abandoned'
  | 'draw';

export type PlayerSymbol = 'x' | 'o';

export interface Game {
  gameId: number;
  gameType: GameType;
  boardSize: 3 | 4 | 5;
  status: GameStatus;
  boardState: (PlayerSymbol | null)[][];
  player1Id: number;
  player2Id: number | null;
  botDifficulty: BotDifficulty | null;
  currentPlayerSymbol: PlayerSymbol | null;
  winnerId: number | null;
  lastMoveAt: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
  totalMoves: number;
}

