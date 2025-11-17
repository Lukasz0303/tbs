import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Game, PlayerSymbol, BotDifficulty, GameType } from '../models/game.model';
import { SavedGameResponse } from '../models/api.model';
import { GameResponse, CreateGameRequest, MakeMoveRequest, MakeMoveResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

  getSavedGame(): Observable<Game | null> {
    const params = new HttpParams()
      .append('status', 'waiting')
      .append('status', 'in_progress')
      .set('size', 1)
      .set('sort', 'updatedAt,desc');

    return this.http
      .get<SavedGameResponse>(`${this.apiUrl}/v1/games`, { params })
      .pipe(
        map((response) => {
          const [game] = response.content ?? [];
          if (!game) {
            return null;
          }
          return this.mapToGame(game);
        }),
        catchError((error) => {
          if (error.status === 404) {
            return of(null);
          }
          return throwError(() => error);
        })
      );
  }

  getGame(gameId: number): Observable<Game> {
    if (!gameId || gameId <= 0) {
      return throwError(() => new Error('Invalid gameId: must be a positive number'));
    }

    return this.http.get<GameResponse>(`${this.apiUrl}/v1/games/${gameId}`).pipe(
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

    return boardState.map((row: unknown, rowIndex: number) => {
      if (!Array.isArray(row)) {
        return [];
      }
      return row.map((cell: unknown, colIndex: number) => {
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

  createBotGame(difficulty: BotDifficulty = 'easy', boardSize: 3 | 4 | 5 = 3): Observable<Game> {
    if (!difficulty || !['easy', 'medium', 'hard'].includes(difficulty)) {
      return throwError(() => new Error('Invalid difficulty: must be easy, medium, or hard'));
    }

    if (![3, 4, 5].includes(boardSize)) {
      return throwError(() => new Error('Invalid boardSize: must be 3, 4, or 5'));
    }

    const request: CreateGameRequest = {
      gameType: 'vs_bot',
      botDifficulty: difficulty,
      boardSize,
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/v1/games`, request).pipe(
      map((response) => {
        return this.mapToGame(response);
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  createPvpGame(boardSize: 3 | 4 | 5 = 3): Observable<Game> {
    if (![3, 4, 5].includes(boardSize)) {
      return throwError(() => new Error('Invalid boardSize: must be 3, 4, or 5'));
    }

    const request: CreateGameRequest = {
      gameType: 'pvp',
      boardSize,
    };

    return this.http.post<GameResponse>(`${this.apiUrl}/v1/games`, request).pipe(
      map((response) => {
        return this.mapToGame(response);
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  makeMove(gameId: number, row: number, col: number, playerSymbol: PlayerSymbol): Observable<Game> {
    if (!gameId || gameId <= 0) {
      return throwError(() => new Error('Invalid gameId: must be a positive number'));
    }

    if (row < 0 || col < 0) {
      return throwError(() => new Error('Invalid move coordinates: row and col must be non-negative'));
    }

    if (playerSymbol !== 'x' && playerSymbol !== 'o') {
      return throwError(() => new Error('Invalid playerSymbol: must be x or o'));
    }

    const request: MakeMoveRequest = {
      row,
      col,
      playerSymbol,
    };

    return this.http.post<MakeMoveResponse>(`${this.apiUrl}/v1/games/${gameId}/moves`, request).pipe(
      switchMap((response) => {
        if (response.gameId && response.status) {
          return of(this.mapToGame(response));
        }
        
        return this.getGame(gameId);
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  surrenderGame(gameId: number): Observable<void> {
    if (!gameId || gameId <= 0) {
      return throwError(() => new Error('Invalid gameId: must be a positive number'));
    }

    return this.http.put<void>(`${this.apiUrl}/v1/games/${gameId}/status`, {
      status: 'abandoned',
    }).pipe(
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }

  makeBotMove(gameId: number): Observable<Game> {
    if (!gameId || gameId <= 0) {
      return throwError(() => new Error('Invalid gameId: must be a positive number'));
    }

    return this.http.post<void>(`${this.apiUrl}/v1/games/${gameId}/bot-move`, {}).pipe(
      switchMap(() => {
        return this.getGame(gameId);
      }),
      catchError((error) => {
        return throwError(() => error);
      })
    );
  }
}

