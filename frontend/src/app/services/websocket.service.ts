import { inject, Injectable, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, BehaviorSubject, throwError, EMPTY } from 'rxjs';
import { catchError, filter, map, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { WebSocketDTOs } from '../models/websocket.model';
import { LoggerService } from './logger.service';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private ws: WebSocket | null = null;
  private readonly messages$ = new Subject<WebSocketDTOs.WebSocketMessage>();
  private readonly connectionStatus$ = new BehaviorSubject<boolean>(false);
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 20;
  private reconnectTimeout: number | null = null;
  private gameId: number | null = null;
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isConnected$ = this.connectionStatus$.asObservable();

  connect(gameId: number, token: string): Observable<void> {
    if (!gameId || gameId <= 0) {
      return throwError(() => new Error('Invalid gameId: must be a positive number'));
    }

    if (!token || token.trim().length === 0) {
      return throwError(() => new Error('Invalid token: token cannot be empty'));
    }
    if (this.ws?.readyState === WebSocket.OPEN && this.gameId === gameId) {
      return new Observable((observer) => {
        observer.next();
        observer.complete();
      });
    }

    this.disconnect();
    this.gameId = gameId;

    const baseUrl = environment.apiBaseUrl.replace('http', 'ws');
    const url = `${baseUrl}/ws/game/${gameId}?token=${encodeURIComponent(token)}`;
    const connectionTimeout = 10000;

    this.logger.debug('WebSocket: Attempting to connect', {
      gameId,
      url: url.replace(/\?token=.*$/, '?token=***'),
      baseUrl,
      apiBaseUrl: environment.apiBaseUrl
    });

    return new Observable((observer) => {
      let timeoutId: number | null = null;

      try {
        this.ws = new WebSocket(url);

        timeoutId = window.setTimeout(() => {
          if (this.ws?.readyState !== WebSocket.OPEN) {
            this.logger.error('WebSocket: Connection timeout', {
              readyState: this.ws?.readyState,
              gameId
            });
            if (this.ws) {
              this.ws.close();
            }
            observer.error(new Error('WebSocket connection timeout'));
          }
        }, connectionTimeout);

        this.ws.onopen = () => {
          this.logger.debug('WebSocket: Connection opened successfully', { gameId });
          if (timeoutId !== null) {
            clearTimeout(timeoutId);
          }
          this.connectionStatus$.next(true);
          this.reconnectAttempts = 0;
          observer.next();
          observer.complete();
        };

        this.ws.onmessage = (event) => {
          try {
            const parsed = JSON.parse(event.data);
            if (this.isValidWebSocketMessage(parsed)) {
              this.logger.debug('WebSocket: Message received', { type: parsed.type, gameId });
              this.messages$.next(parsed);
            } else {
              this.logger.warn('WebSocket: Invalid message format', { parsed, gameId });
            }
          } catch (error) {
            this.logger.error('WebSocket: Failed to parse message', { error, gameId });
          }
        };

        this.ws.onerror = (error) => {
          this.logger.error('WebSocket: Connection error', {
            error,
            gameId,
            readyState: this.ws?.readyState,
            url: url.replace(/\?token=.*$/, '?token=***')
          });
          if (timeoutId !== null) {
            clearTimeout(timeoutId);
          }
          observer.error(error);
        };

        this.ws.onclose = (event) => {
          this.logger.warn('WebSocket: Connection closed', {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean,
            gameId,
            readyState: this.ws?.readyState
          });
          if (timeoutId !== null) {
            clearTimeout(timeoutId);
          }
          this.connectionStatus$.next(false);
          
          if (event.code !== 1000 && event.code !== 1001) {
            this.handleReconnect(gameId, token);
          }
        };
      } catch (error) {
        if (timeoutId !== null) {
          clearTimeout(timeoutId);
        }
        observer.error(error);
      }
    });
  }

  disconnect(): void {
    if (this.reconnectTimeout !== null) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.gameId = null;
    this.connectionStatus$.next(false);
  }

  private isValidWebSocketMessage(data: unknown): data is WebSocketDTOs.WebSocketMessage {
    return (
      typeof data === 'object' &&
      data !== null &&
      'type' in data &&
      'payload' in data &&
      typeof (data as { type: unknown }).type === 'string'
    );
  }

  sendMove(row: number, col: number, playerSymbol: 'x' | 'o'): Observable<void> {
    return new Observable((observer) => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
        const readyState = this.ws?.readyState;
        const stateNames: Record<number, string> = {
          [WebSocket.CONNECTING]: 'CONNECTING',
          [WebSocket.OPEN]: 'OPEN',
          [WebSocket.CLOSING]: 'CLOSING',
          [WebSocket.CLOSED]: 'CLOSED'
        };
        const stateName = readyState !== undefined ? (stateNames[readyState] || 'UNKNOWN') : 'NULL';
        this.logger.error('WebSocket: Cannot send move - not connected', {
          readyState,
          stateName,
          gameId: this.gameId
        });
        observer.error(new Error(`WebSocket is not connected (state: ${stateName})`));
        return;
      }

      try {
        const message: WebSocketDTOs.MoveMessage = {
          type: 'MOVE',
          payload: {
            row,
            col,
            playerSymbol,
          },
        };
        const json = JSON.stringify(message);
        this.logger.debug('WebSocket: Sending move', { json, row, col, playerSymbol, gameId: this.gameId });
        this.ws.send(json);
        observer.next();
        observer.complete();
      } catch (error) {
        this.logger.error('WebSocket: Error sending move', { error, gameId: this.gameId });
        observer.error(error);
      }
    });
  }

  sendSurrender(): Observable<void> {
    return new Observable((observer) => {
      if (this.ws?.readyState !== WebSocket.OPEN) {
        observer.error(new Error('WebSocket is not connected'));
        return;
      }

      try {
        const message: WebSocketDTOs.SurrenderMessage = {
          type: 'SURRENDER',
          payload: {},
        };
        this.ws.send(JSON.stringify(message));
        observer.next();
        observer.complete();
      } catch (error) {
        observer.error(error);
      }
    });
  }

  getMessages(): Observable<WebSocketDTOs.WebSocketMessage> {
    return this.messages$.asObservable();
  }

  private handleReconnect(gameId: number, token: string): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.messages$.error(new Error('Max reconnect attempts reached'));
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 10000);

    this.reconnectTimeout = window.setTimeout(() => {
      this.connect(gameId, token)
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          catchError(() => {
            this.handleReconnect(gameId, token);
            return EMPTY;
          })
        )
        .subscribe();
    }, delay);
  }
}

