import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Game, PlayerSymbol } from '../models/game.model';
import { GameResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class MatchmakingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

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
    
    const normalized = this.normalizeBoardState(boardState);
    
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

  private normalizeBoardState(boardState: unknown): (PlayerSymbol | null)[][] {
    if (!Array.isArray(boardState)) {
      return [];
    }

    if (boardState.length === 0) {
      return [];
    }

    return boardState.map((row: unknown) => {
      if (!Array.isArray(row)) {
        return [];
      }
      return row.map((cell: unknown) => {
        if (cell === null || cell === undefined || cell === '') {
          return null;
        }
        if (cell === 'x' || cell === 'o') {
          return cell as PlayerSymbol;
        }
        return null;
      });
    });
  }
}

