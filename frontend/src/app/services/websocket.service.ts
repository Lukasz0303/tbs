import { inject, Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject, throwError, EMPTY } from 'rxjs';
import { catchError, filter, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { WebSocketDTOs } from '../models/websocket.model';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private ws: WebSocket | null = null;
  private readonly messages$ = new Subject<WebSocketDTOs.WebSocketMessage>();
  private readonly connectionStatus$ = new BehaviorSubject<boolean>(false);
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 20;
  private reconnectTimeout: number | null = null;
  private gameId: number | null = null;

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

    const wsUrl = environment.apiBaseUrl.replace('http', 'ws');
    const url = `${wsUrl}/ws/game/${gameId}?token=${encodeURIComponent(token)}`;
    const connectionTimeout = 10000;

    return new Observable((observer) => {
      let timeoutId: number | null = null;

      try {
        this.ws = new WebSocket(url);

        timeoutId = window.setTimeout(() => {
          if (this.ws?.readyState !== WebSocket.OPEN) {
            if (this.ws) {
              this.ws.close();
            }
            observer.error(new Error('WebSocket connection timeout'));
          }
        }, connectionTimeout);

        this.ws.onopen = () => {
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
              this.messages$.next(parsed);
            } else {
              console.warn('Invalid WebSocket message format:', parsed);
            }
          } catch (error) {
            console.error('Failed to parse WebSocket message:', error);
          }
        };

        this.ws.onerror = (error) => {
          if (timeoutId !== null) {
            clearTimeout(timeoutId);
          }
          observer.error(error);
        };

        this.ws.onclose = () => {
          if (timeoutId !== null) {
            clearTimeout(timeoutId);
          }
          this.connectionStatus$.next(false);
          this.handleReconnect(gameId, token);
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
      if (this.ws?.readyState !== WebSocket.OPEN) {
        observer.error(new Error('WebSocket is not connected'));
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
        this.ws.send(JSON.stringify(message));
        observer.next();
        observer.complete();
      } catch (error) {
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
    const delay = Math.min(1000 * this.reconnectAttempts, 10000);

    this.reconnectTimeout = window.setTimeout(() => {
      this.connect(gameId, token)
        .pipe(
          catchError(() => {
            this.handleReconnect(gameId, token);
            return EMPTY;
          })
        )
        .subscribe();
    }, delay);
  }
}

