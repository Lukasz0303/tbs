import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, timeout } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Game, PlayerSymbol } from '../models/game.model';
import { GameResponse } from '../models/api-response.model';
import { normalizeBoardState } from '../shared/utils/board-state.util';

export interface MatchmakingQueueRequest {
  boardSize: 3 | 4 | 5;
}

export interface MatchmakingQueueResponse {
  message: string;
  estimatedWaitTime: number;
}

export interface LeaveQueueResponse {
  message: string;
}

export type BoardSizeEnum = 'THREE' | 'FOUR' | 'FIVE';
export type QueuePlayerStatusEnum = 'WAITING' | 'MATCHED' | 'PLAYING';

export interface PlayerQueueStatus {
  userId: number;
  username: string;
  boardSize: BoardSizeEnum;
  status: QueuePlayerStatusEnum;
  joinedAt: string;
  matchedWith?: number | null;
  matchedWithUsername?: string | null;
  gameId?: number | null;
  isMatched: boolean;
}

export interface QueueStatusResponse {
  players: PlayerQueueStatus[];
  totalCount: number;
}

@Injectable({ providedIn: 'root' })
export class MatchmakingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

  joinQueue(boardSize: 3 | 4 | 5): Observable<MatchmakingQueueResponse> {
    if (![3, 4, 5].includes(boardSize)) {
      return throwError(() => new Error('Invalid boardSize: must be 3, 4, or 5'));
    }

    const request: MatchmakingQueueRequest = { boardSize };

    return this.http
      .post<MatchmakingQueueResponse>(`${this.apiUrl}/v1/matching/queue`, request)
      .pipe(
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }

  leaveQueue(): Observable<LeaveQueueResponse> {
    return this.http
      .delete<LeaveQueueResponse>(`${this.apiUrl}/v1/matching/queue`)
      .pipe(
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }

  getQueueStatus(boardSize?: 3 | 4 | 5): Observable<QueueStatusResponse> {
    let params = new HttpParams();
    
    if (boardSize) {
      const boardSizeMap: Record<number, string> = {
        3: 'THREE',
        4: 'FOUR',
        5: 'FIVE',
      };
      params = params.set('boardSize', boardSizeMap[boardSize]);
    }

    return this.http
      .get<QueueStatusResponse>(`${this.apiUrl}/v1/matching/queue`, { params })
      .pipe(
        timeout(10000),
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }

  challengePlayer(userId: number, boardSize: 3 | 4 | 5 = 3): Observable<Game> {
    if (!userId || userId <= 0) {
      return throwError(() => new Error('Invalid userId: must be a positive number'));
    }

    if (![3, 4, 5].includes(boardSize)) {
      return throwError(() => new Error('Invalid boardSize: must be 3, 4, or 5'));
    }

    return this.http
      .post<GameResponse>(`${this.apiUrl}/v1/matching/challenge/${userId}`, { boardSize })
      .pipe(
        map((response) => this.mapToGame(response)),
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }

  private mapToGame(response: GameResponse): Game {
    let boardState: unknown = response.boardState;
    
    if (boardState && typeof boardState === 'object' && !Array.isArray(boardState) && 'state' in boardState) {
      boardState = (boardState as { state: (PlayerSymbol | null)[][] }).state;
    }
    
    if (!Array.isArray(boardState)) {
      boardState = [];
    }
    
    const normalized = normalizeBoardState(boardState);
    
    const player1Id = response.player1?.userId || response.player1Id || 0;
    const player2Id = response.player2?.userId || response.player2Id || null;
    const winnerId = response.winner?.userId || response.winnerId || null;
    
    return {
      gameId: response.gameId,
      gameType: response.gameType,
      boardSize: response.boardSize,
      status: response.status,
      boardState: normalized,
      player1Id,
      player2Id,
      botDifficulty: response.botDifficulty ?? null,
      currentPlayerSymbol: response.currentPlayerSymbol,
      winnerId,
      lastMoveAt: response.lastMoveAt ?? null,
      createdAt: response.createdAt,
      updatedAt: response.updatedAt,
      finishedAt: response.finishedAt ?? null,
      totalMoves: response.totalMoves,
    };
  }
}

