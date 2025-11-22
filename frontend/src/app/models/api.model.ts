import { Game } from './game.model';
import { GameResponse } from './api-response.model';

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
}

export interface SavedGameResponse extends PaginatedResponse<GameResponse> {}

export interface GuestSessionResponse {
  userId: number;
  isGuest: boolean;
  avatar: number;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
}

