import { AsyncPipe, CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, BehaviorSubject, EMPTY, switchMap, map, catchError, of, take, filter, timer } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { GameService } from '../../services/game.service';
import { TranslateService } from '../../services/translate.service';
import { Game, PlayerSymbol } from '../../models/game.model';
import { GameBoardComponent } from '../../components/game/game-board.component';
import { GameTimerComponent } from '../../components/game/game-timer.component';
import { AuthService } from '../../services/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { GameResultDialogComponent } from '../../components/game/game-result-dialog.component';
import { GameUserProfileComponent } from '../../components/game/game-user-profile.component';
import { GameBotInfoComponent } from '../../components/game/game-bot-info.component';
import { WebSocketDTOs } from '../../models/websocket.model';
import { LoggerService } from '../../services/logger.service';
import { normalizeBoardState } from '../../shared/utils/board-state.util';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    ButtonModule,
    ProgressSpinnerModule,
    ToastModule,
    GameBoardComponent,
    GameTimerComponent,
    GameResultDialogComponent,
    GameUserProfileComponent,
    GameBotInfoComponent,
  ],
  providers: [MessageService],
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly gameService = inject(GameService);
  private readonly messageService = inject(MessageService);
  readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);
  private readonly websocketService = inject(WebSocketService);
  private readonly logger = inject(LoggerService);
  private readonly userService = inject(UserService);

  readonly isBotThinking = signal<boolean>(false);
  readonly showResult = signal<boolean>(false);
  readonly remainingSeconds = signal<number>(20);
  readonly isPlayerTurn = signal<boolean>(true);
  readonly isMovePending = signal<boolean>(false);
  readonly winningCells = signal<Array<{ row: number; col: number }>>([]);
  readonly currentUser$ = this.authService.getCurrentUser();
  private readonly currentUserSignal = toSignal(this.authService.getCurrentUser(), { initialValue: null });
  readonly opponentUser = signal<User | null>(null);

  private gameId: number | null = null;

  private readonly gameState$ = new BehaviorSubject<{ loading: boolean; game: Game | null }>({
    loading: true,
    game: null,
  });

  readonly vm = toSignal(this.gameState$, { initialValue: { loading: true, game: null as Game | null } });

  ngOnInit(): void {
    this.authService.loadCurrentUser().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const gameId = Number(params.get('gameId'));
        this.logger.debug('GameComponent.ngOnInit route param gameId', gameId);
        if (gameId) {
          this.gameId = gameId;
          this.loadGame();
          this.setupGameUpdates();
          this.checkBotTurnAfterUserLoad();
        }
      });
  }

  private checkBotTurnAfterUserLoad(): void {
    this.authService
      .getCurrentUser()
      .pipe(
        filter((user) => user !== null),
        take(1),
        switchMap((user) => {
          if (!this.gameId) {
            return EMPTY;
          }
          return this.gameService.getGame(this.gameId);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (game) => {
          if (game.gameType === 'vs_bot' && (game.status === 'in_progress' || game.status === 'waiting')) {
            if (game.currentPlayerSymbol === 'o' && !this.isBotThinking()) {
              this.makeBotMove();
            }
          }
        },
        error: (error) => {
          this.logger.error('Error in checkBotTurnAfterUserLoad:', error);
        },
      });
  }

  private updateGameState(game: Game | null, loading: boolean = false): void {
    this.gameState$.next({ loading, game });
  }

  private loadGame(): void {
    if (!this.gameId) {
      return;
    }

    this.logger.debug('GameComponent.loadGame', { gameId: this.gameId });

    this.gameService
      .getGame(this.gameId)
      .pipe(
        switchMap((game) => {
          this.logger.debug('GameComponent.loadGame response', {
            gameId: game.gameId,
            gameType: game.gameType,
            status: game.status,
            player1Id: game.player1Id,
            player2Id: game.player2Id,
            currentPlayerSymbol: game.currentPlayerSymbol,
            totalMoves: game.totalMoves,
          });
          this.updateLocalGame(game);
          this.updateTurnLock(game);
          this.updateWinningCells(game);

          if (game.gameType === 'vs_bot' && game.status === 'waiting') {
            this.updateTurnLock(game);
            return of(game);
          }

          if (game.gameType === 'vs_bot' && game.status === 'in_progress' && game.totalMoves === 0) {
            this.updateTurnLock(game);
          }

          return of(game);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (game) => {
          if (game.gameType === 'pvp' && game.status === 'in_progress') {
            this.connectWebSocket();
            this.startTimer();
          }

          if (game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned') {
            this.showResult.set(true);
          }
        },
        error: (error) => {
          this.handleError(error);
        },
      });
  }

  private setupGameUpdates(): void {
    if (!this.gameId) {
      return;
    }

    interval(10000)
      .pipe(
        switchMap(() => {
          if (!this.gameId) {
            return EMPTY;
          }
          return this.gameService.getGame(this.gameId!).pipe(
            catchError((error) => {
              this.logger.error('Error polling game updates:', error);
              return EMPTY;
            })
          );
        }),
        map((game) => {
          const currentGame = this.vm()?.game;
          if (currentGame && this.hasGameChanged(currentGame, game)) {
            return game;
          }
          return null;
        }),
        filter((game): game is Game => game !== null),
        switchMap((game) => {
          this.updateLocalGame(game);
          this.updateTurnLock(game);
          this.updateWinningCells(game);
          this.handleGameStatusChange(game);

          if (game.gameType === 'vs_bot' && (game.status === 'in_progress' || game.status === 'waiting')) {
            return this.authService.getCurrentUser().pipe(
              take(1),
              map((user) => ({ game, user }))
            );
          }
          return of({ game, user: null });
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ game, user }) => {
          if (user && game.gameType === 'vs_bot' && game.currentPlayerSymbol === 'o' && !this.isBotThinking()) {
            this.makeBotMove();
          }
        },
        error: (error) => {
          this.logger.error('Error in setupGameUpdates:', error);
        },
      });
  }

  private connectWebSocket(): void {
    if (!this.gameId) {
      return;
    }

    const token = this.authService.getAuthToken();
    if (!token) {
      this.messageService.add({
        severity: 'error',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.missingToken'),
      });
      return;
    }

    this.websocketService
      .connect(this.gameId, token)
      .pipe(
        switchMap(() => this.websocketService.getMessages()),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (message) => this.handleWebSocketMessage(message),
        error: (error) => this.handleWebSocketError(error),
      });
  }

  private startTimer(): void {
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          const state = this.vm();
          const game = state?.game;
          if (game && game.gameType === 'pvp' && game.status === 'in_progress' && game.lastMoveAt) {
            const elapsed = (Date.now() - new Date(game.lastMoveAt).getTime()) / 1000;
            const remaining = Math.max(0, 20 - elapsed);
            this.remainingSeconds.set(Math.floor(remaining));

            if (remaining <= 0) {
              this.checkTimeout();
            }
          }
        },
        error: (error) => {
          this.logger.error('Error in startTimer:', error);
        },
      });
  }

  private checkTimeout(): void {
    const state = this.vm();
    const game = state?.game;
    if (game && game.gameType === 'pvp' && game.status === 'in_progress') {
      this.loadGame();
    }
  }

  private pollUntilGameStarts(): void {
    if (!this.gameId) {
      return;
    }

    const maxPolls = 10;

    interval(1000)
      .pipe(
        take(maxPolls),
        switchMap(() => {
          if (!this.gameId) {
            return EMPTY;
          }
          return this.gameService.getGame(this.gameId);
        }),
        filter((game) => game.status !== 'waiting'),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (game) => {
          this.updateLocalGame(game);
          this.updateTurnLock(game);
          this.updateWinningCells(game);

          if (game.gameType === 'pvp' && game.status === 'in_progress') {
            this.connectWebSocket();
            this.startTimer();
          }

          if (game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned') {
            this.showResult.set(true);
          }
        },
        error: (error) => {
          this.logger.error('Error in pollUntilGameStarts:', error);
          const state = this.vm();
          const game = state?.game;
          if (game && game.status === 'waiting') {
            this.messageService.add({
              severity: 'warn',
              summary: this.translate.translate('game.error.title'),
              detail: this.translate.translate('game.error.timeout'),
            });
          }
        },
        complete: () => {
          const state = this.vm();
          const game = state?.game;
          if (game && game.status === 'waiting') {
            this.messageService.add({
              severity: 'warn',
              summary: this.translate.translate('game.error.title'),
              detail: this.translate.translate('game.error.timeout'),
            });
          }
        },
      });
  }

  onMove(event: { row: number; col: number }, game: Game): void {
    if (!this.gameId) {
      this.logger.error('Cannot make move: gameId is missing');
      return;
    }

    if (!game) {
      this.logger.error('Cannot make move: game is null');
      this.messageService.add({
        severity: 'error',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.load'),
      });
      return;
    }

    if (event.row < 0 || event.col < 0 || event.row >= game.boardSize || event.col >= game.boardSize) {
      this.logger.error('Invalid move coordinates', { 
        row: event.row, 
        col: event.col, 
        boardSize: game.boardSize 
      });
      this.messageService.add({
        severity: 'error',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.move'),
      });
      return;
    }

    this.logger.debug('GAME_ON_MOVE', {
      row: event.row,
      col: event.col,
      gameType: game.gameType,
      status: game.status,
      currentPlayerSymbol: game.currentPlayerSymbol,
      isMoveDisabled: this.isMoveDisabled(game),
      isMovePending: this.isMovePending()
    });

    if (this.isMovePending()) {
      this.logger.debug('GAME_MOVE_BLOCKED', {
        reason: 'move_pending',
        gameType: game.gameType,
        status: game.status
      });
      this.messageService.add({
        severity: 'warn',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.waitingForResponse'),
      });
      return;
    }

    if (this.isMoveDisabled(game)) {
      this.logger.debug('GAME_MOVE_BLOCKED', {
        reason: 'move_disabled',
        gameType: game.gameType,
        status: game.status,
        currentPlayerSymbol: game.currentPlayerSymbol,
        isPlayerTurn: this.isPlayerTurn()
      });
      this.messageService.add({
        severity: 'warn',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.moveNotAllowed'),
      });
      return;
    }

    const cell = game.boardState[event.row]?.[event.col];
    if (cell === 'x' || cell === 'o') {
      this.logger.debug('GAME_MOVE_BLOCKED', {
        reason: 'cell_occupied',
        row: event.row,
        col: event.col,
        cellValue: cell
      });
      this.messageService.add({
        severity: 'warn',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.cellOccupied'),
      });
      return;
    }

    this.isMovePending.set(true);

    if (game.gameType === 'pvp') {
      this.sendMoveViaWebSocket(event, game);
    } else {
      this.sendMoveViaREST(event, game);
    }
  }

  private sendMoveViaREST(move: { row: number; col: number }, game: Game): void {
    if (!this.gameId) {
      return;
    }

    this.messageService.add({
      severity: 'info',
      summary: this.translate.translate('game.info.title'),
      detail: this.translate.translate('game.info.makingMove'),
    });

    this.authService
      .getCurrentUser()
      .pipe(
        take(1),
        switchMap((user) => {
          if (!user) {
            return EMPTY;
          }

          let playerSymbol: 'x' | 'o';
          if (game.gameType === 'vs_bot') {
            playerSymbol = 'x';
          } else {
            playerSymbol = game.player1Id === user.userId ? 'x' : 'o';
          }

          return this.gameService.makeMove(this.gameId!, move.row, move.col, playerSymbol);
        }),
        switchMap((updated) => {
          this.updateLocalGame(updated);
          this.updateTurnLock(updated);
          this.updateWinningCells(updated);
          this.handleMoveResponse(updated);

          if (updated.gameType === 'vs_bot' && updated.status === 'in_progress') {
            return this.authService.getCurrentUser().pipe(
              take(1),
              map((user) => ({ updated, user }))
            );
          }
          return of({ updated, user: null });
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ updated, user }) => {
          this.isMovePending.set(false);
          if (user && updated.gameType === 'vs_bot' && updated.status === 'in_progress') {
            if (updated.currentPlayerSymbol && updated.currentPlayerSymbol === 'o' && !this.isBotThinking()) {
              this.makeBotMove();
            }
          }
        },
        error: (error) => {
          this.isMovePending.set(false);
          this.handleMoveError(error);
        },
      });
  }

  private sendMoveViaWebSocket(move: { row: number; col: number }, game: Game): void {
    const user = this.currentUserSignal();
    if (!user) {
      this.logger.error('Cannot send move: user not found');
      this.isMovePending.set(false);
      this.messageService.add({
        severity: 'error',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.unauthorized'),
      });
      return;
    }

    this.websocketService.isConnected$.pipe(
      take(1),
      switchMap((isConnected) => {
        this.logger.debug('GAME_WS_CONNECTION_STATUS', {
          isConnected,
          gameId: this.gameId,
        });
        
        if (!isConnected) {
          this.logger.debug('GAME_MOVE_BLOCKED', {
            reason: 'websocket_not_connected',
            gameId: this.gameId
          });
          this.messageService.add({
            severity: 'warn',
            summary: this.translate.translate('game.error.title'),
            detail: this.translate.translate('game.error.websocketReconnecting'),
          });
          
          const token = this.authService.getAuthToken();
          if (token && this.gameId) {
            return this.websocketService.connect(this.gameId, token).pipe(
              catchError((error) => {
                this.logger.error('Failed to reconnect WebSocket:', error);
                this.isMovePending.set(false);
                this.messageService.add({
                  severity: 'error',
                  summary: this.translate.translate('game.error.title'),
                  detail: this.translate.translate('game.error.websocketConnectionFailed'),
                });
                return EMPTY;
              })
            );
          }
          this.isMovePending.set(false);
          return EMPTY;
        }

        const playerSymbol = Number(game.player1Id) === Number(user.userId) ? 'x' : 'o';
        
        this.logger.debug('Sending move via WebSocket', {
          row: move.row,
          col: move.col,
          playerSymbol,
          userId: user.userId,
          player1Id: game.player1Id,
          player2Id: game.player2Id,
          isConnected
        });

        return this.websocketService.sendMove(move.row, move.col, playerSymbol);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.logger.debug('Move sent successfully via WebSocket');
      },
      error: (error) => {
        this.isMovePending.set(false);
        this.logger.error('Error sending move via WebSocket:', error);
        this.handleMoveError(error);
      },
    });
  }

  private makeBotMove(): void {
    if (!this.gameId) {
      return;
    }

    if (this.isBotThinking()) {
      return;
    }

    this.isBotThinking.set(true);

    timer(200)
      .pipe(
        switchMap(() => {
          if (!this.gameId) {
            this.isBotThinking.set(false);
            return EMPTY;
          }
          return this.gameService.makeBotMove(this.gameId!);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (updated) => {
          this.updateLocalGame(updated);
          this.updateTurnLock(updated);
          this.updateWinningCells(updated);
          this.handleMoveResponse(updated);
          this.isBotThinking.set(false);
        },
        error: (error) => {
          this.isBotThinking.set(false);
          this.handleError(error);
        },
      });
  }

  private handleMoveResponse(game: Game): void {
    if (game.status === 'finished' || game.status === 'draw') {
      setTimeout(() => {
        this.showResult.set(true);
      }, 400);
      
      let detail = '';
      let summary = '';
      if (game.status === 'finished') {
        summary = this.translate.translate('game.result.gameFinished');
        if (game.gameType === 'vs_bot') {
          detail = game.winnerId === game.player1Id 
            ? this.translate.translate('game.result.youWon')
            : this.translate.translate('game.result.youLost');
        } else {
          detail = this.translate.translate('game.result.gameFinished');
        }
      } else {
        summary = this.translate.translate('game.status.draw');
        detail = this.translate.translate('game.result.gameDraw');
      }
      
      this.messageService.add({
        severity: game.status === 'finished' ? 'success' : 'info',
        summary,
        detail,
      });
    }
  }

  private handleWebSocketMessage(message: WebSocketDTOs.WebSocketMessage): void {
    this.logger.debug('GAME_WS_MESSAGE', {
      type: message.type,
      payload: message.payload
    });
    this.logger.debug('GAME_WS_MESSAGE_PAYLOAD', JSON.stringify(message.payload, null, 2));
    switch (message.type) {
      case 'MOVE_ACCEPTED':
        if (this.isMoveAcceptedPayload(message.payload)) {
          this.isMovePending.set(false);
          this.updateGameFromMove(message.payload);
        }
        break;
      case 'MOVE_REJECTED':
        if (this.isMoveRejectedPayload(message.payload)) {
          this.isMovePending.set(false);
          this.messageService.add({
            severity: 'error',
            summary: this.translate.translate('game.error.title'),
            detail: message.payload.reason || this.translate.translate('game.error.move'),
          });
        }
        break;
      case 'OPPONENT_MOVE':
        if (this.isOpponentMovePayload(message.payload)) {
          this.isMovePending.set(false);
          this.updateGameFromMove(message.payload);
        }
        break;
      case 'GAME_UPDATE':
        if (this.isGameUpdatePayload(message.payload)) {
          this.updateGameFromPayload(message.payload);
        }
        break;
      case 'TIMER_UPDATE':
        if (this.isTimerUpdatePayload(message.payload)) {
          this.remainingSeconds.set(message.payload.remainingSeconds);
        }
        break;
      case 'GAME_ENDED':
        if (this.isGameEndedPayload(message.payload)) {
          this.isMovePending.set(false);
          this.updateGameFromPayload(message.payload);
          setTimeout(() => {
            this.showResult.set(true);
          }, 400);
        }
        break;
    }
  }

  private isMoveAcceptedPayload(payload: unknown): payload is WebSocketDTOs.MoveAcceptedMessage['payload'] {
    return (
      typeof payload === 'object' &&
      payload !== null &&
      'boardState' in payload &&
      'currentPlayerSymbol' in payload &&
      'nextMoveAt' in payload
    );
  }

  private isMoveRejectedPayload(payload: unknown): payload is WebSocketDTOs.MoveRejectedMessage['payload'] {
    return (
      typeof payload === 'object' &&
      payload !== null &&
      'reason' in payload &&
      'code' in payload
    );
  }

  private isOpponentMovePayload(payload: unknown): payload is WebSocketDTOs.OpponentMoveMessage['payload'] {
    return (
      typeof payload === 'object' &&
      payload !== null &&
      'boardState' in payload &&
      'currentPlayerSymbol' in payload &&
      'nextMoveAt' in payload
    );
  }

  private isGameUpdatePayload(payload: unknown): payload is WebSocketDTOs.GameUpdateMessage['payload'] {
    return (
      typeof payload === 'object' &&
      payload !== null &&
      'gameId' in payload &&
      'status' in payload &&
      'boardState' in payload
    );
  }

  private isTimerUpdatePayload(payload: unknown): payload is WebSocketDTOs.TimerUpdateMessage['payload'] {
    return (
      typeof payload === 'object' &&
      payload !== null &&
      'remainingSeconds' in payload &&
      typeof (payload as { remainingSeconds: unknown }).remainingSeconds === 'number'
    );
  }

  private isGameEndedPayload(payload: unknown): payload is WebSocketDTOs.GameEndedMessage['payload'] {
    return (
      typeof payload === 'object' &&
      payload !== null &&
      'gameId' in payload &&
      'status' in payload &&
      'finalBoardState' in payload &&
      'totalMoves' in payload
    );
  }

  private normalizeBoardStateFromPayload(boardState: unknown): (PlayerSymbol | null)[][] {
    if (boardState && typeof boardState === 'object' && !Array.isArray(boardState) && 'state' in boardState) {
      boardState = (boardState as { state: (PlayerSymbol | null)[][] }).state;
    }
    return normalizeBoardState(boardState);
  }

  private normalizePlayerSymbol(symbol: unknown): PlayerSymbol | null {
    if (!symbol) {
      return null;
    }
    const normalized = String(symbol).toLowerCase();
    return normalized === 'x' || normalized === 'o' ? normalized as PlayerSymbol : null;
  }

  private updateGameFromMove(payload: WebSocketDTOs.MoveAcceptedMessage['payload'] | WebSocketDTOs.OpponentMoveMessage['payload']): void {
    const state = this.vm();
    const game = state?.game;
    if (game) {
      const boardState = this.normalizeBoardStateFromPayload(payload.boardState);
      const currentPlayerSymbol = this.normalizePlayerSymbol(payload.currentPlayerSymbol);
      
      const updated: Game = {
        ...game,
        boardState,
        currentPlayerSymbol,
        lastMoveAt: payload.nextMoveAt,
        totalMoves: game.totalMoves + 1,
      };
      
      this.logger.debug('updateGameFromMove', {
        payloadCurrentPlayerSymbol: payload.currentPlayerSymbol,
        normalizedCurrentPlayerSymbol: currentPlayerSymbol,
        totalMoves: updated.totalMoves,
        gameType: updated.gameType
      });
      
      this.updateLocalGame(updated);
      this.updateTurnLock(updated);
      this.updateWinningCells(updated);
    }
  }

  private updateGameFromPayload(payload: WebSocketDTOs.GameUpdateMessage['payload'] | WebSocketDTOs.GameEndedMessage['payload']): void {
    const state = this.vm();
    const game = state?.game;
    if (game) {
      const boardState = this.normalizeBoardStateFromPayload(
        'finalBoardState' in payload ? payload.finalBoardState : payload.boardState
      );

      const updated: Game = {
        ...game,
        status: payload.status,
        winnerId: payload.winner?.userId || null,
        boardState: boardState as (PlayerSymbol | null)[][],
      };
      this.logger.debug('GAME_UPDATE_FROM_PAYLOAD', {
        status: updated.status,
        boardState: updated.boardState,
      });
      this.updateLocalGame(updated);
      this.updateTurnLock(updated);
      this.updateWinningCells(updated);
    }
  }

  private handleGameStatusChange(game: Game): void {
    if (game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned') {
      this.authService.loadCurrentUser().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
      setTimeout(() => {
        this.showResult.set(true);
      }, 400);
    }
  }

  private hasGameChanged(oldGame: Game, newGame: Game): boolean {
    if (oldGame.status !== newGame.status) {
      return true;
    }
    if (oldGame.currentPlayerSymbol !== newGame.currentPlayerSymbol) {
      return true;
    }
    
    if (oldGame.boardState.length !== newGame.boardState.length) {
      return true;
    }
    
    for (let i = 0; i < oldGame.boardState.length; i++) {
      if (oldGame.boardState[i].length !== newGame.boardState[i].length) {
        return true;
      }
      for (let j = 0; j < oldGame.boardState[i].length; j++) {
        if (oldGame.boardState[i][j] !== newGame.boardState[i][j]) {
          return true;
        }
      }
    }
    
    return false;
  }

  surrender(gameId: number): void {
    if (!gameId) {
      return;
    }

    const state = this.vm();
    const game = state?.game;

    if (game?.gameType === 'pvp') {
      this.websocketService
        .sendSurrender()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          error: (error) => this.handleError(error),
        });
    } else {
      this.gameService
        .surrenderGame(gameId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.messageService.add({
              severity: 'info',
              summary: this.translate.translate('game.info.title'),
              detail: this.translate.translate('game.info.surrendered'),
            });
            this.loadGame();
          },
          error: (error) => this.handleError(error),
        });
    }
  }

  private updateLocalGame(nextGame: Game): void {
    this.updateGameState(nextGame, false);
    this.loadOpponentProfile(nextGame);
  }

  private loadOpponentProfile(game: Game | null): void {
    if (!game || game.gameType !== 'pvp' || !game.player2Id) {
      this.opponentUser.set(null);
      return;
    }

    const currentUser = this.currentUserSignal();
    if (!currentUser) {
      return;
    }

    const opponentId = Number(game.player1Id) === Number(currentUser.userId) 
      ? game.player2Id 
      : game.player1Id;

    if (!opponentId) {
      this.opponentUser.set(null);
      return;
    }

    if (this.opponentUser()?.userId === opponentId) {
      return;
    }

    this.userService.getUserProfile(opponentId).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError((error) => {
        this.logger.error('Error loading opponent profile:', error);
        return of(null);
      })
    ).subscribe({
      next: (user) => {
        this.opponentUser.set(user);
      }
    });
  }

  isMoveDisabled(game: Game): boolean {
    const isWaitingVsBot = game.gameType === 'vs_bot' && game.status === 'waiting';
    const isInProgressNoMoves = game.gameType === 'vs_bot' && game.status === 'in_progress' && game.totalMoves === 0;

    if (isWaitingVsBot || isInProgressNoMoves) {
      return false;
    }

    if (game.status !== 'in_progress') {
      this.logger.debug('GAME_IS_MOVE_DISABLED', {
        reason: 'status_not_in_progress',
        status: game.status,
        gameType: game.gameType
      });
      return true;
    }

    if (this.isMovePending()) {
      this.logger.debug('GAME_IS_MOVE_DISABLED', {
        reason: 'move_pending',
        gameType: game.gameType,
        status: game.status
      });
      return true;
    }

    const playerTurn = this.isPlayerTurn();
    this.logger.debug('GAME_IS_MOVE_DISABLED', {
      reason: playerTurn ? 'allowed' : 'not_player_turn',
      gameType: game.gameType,
      status: game.status,
      currentPlayerSymbol: game.currentPlayerSymbol,
      isPlayerTurn: playerTurn,
      disabled: !playerTurn
    });
    
    return !playerTurn;
  }


  onResultDialogClose(): void {
    this.showResult.set(false);
    this.router.navigate(['/game-options']);
  }

  ngOnDestroy(): void {
    this.websocketService.disconnect();
  }

  private handleError(error: unknown): void {
    const errorMessage = this.getErrorMessage(error);
    this.messageService.add({
      severity: 'error',
      summary: this.translate.translate('game.error.title'),
      detail: errorMessage,
    });
  }

  private handleMoveError(error: unknown): void {
    const errorMessage = this.getErrorMessage(error);
    this.messageService.add({
      severity: 'error',
      summary: this.translate.translate('game.error.title'),
      detail: errorMessage || this.translate.translate('game.error.move'),
    });
  }

  private handleWebSocketError(error: unknown): void {
    this.isMovePending.set(false);
    this.messageService.add({
      severity: 'warn',
      summary: this.translate.translate('game.error.title'),
      detail: this.translate.translate('game.error.websocketConnectionFailed'),
    });
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof Error) {
      return error.message;
    }
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const err = error as { error?: { message?: string } };
      return err.error?.message || this.translate.translate('game.error.unknown');
    }
    return this.translate.translate('game.error.unknown');
  }

  isCurrentPlayerTurn(game: Game): boolean {
    if (game.gameType !== 'pvp' || game.status !== 'in_progress') {
      return false;
    }
    const user = this.currentUserSignal();
    if (!user || !game.currentPlayerSymbol) {
      return false;
    }
    const isPlayer1 = Number(game.player1Id) === Number(user.userId);
    return (isPlayer1 && game.currentPlayerSymbol === 'x') || (!isPlayer1 && game.currentPlayerSymbol === 'o');
  }

  isOpponentTurn(game: Game): boolean {
    if (game.gameType !== 'pvp' || game.status !== 'in_progress') {
      return false;
    }
    return !this.isCurrentPlayerTurn(game);
  }

  getPointsAtStake(game: Game): number {
    let basePoints = 0;
    
    if (game.gameType === 'pvp') {
      basePoints = 1000;
    } else if (game.gameType === 'vs_bot' && game.botDifficulty) {
      switch (game.botDifficulty) {
        case 'easy':
          basePoints = 100;
          break;
        case 'medium':
          basePoints = 500;
          break;
        case 'hard':
          basePoints = 1000;
          break;
        default:
          return 0;
      }
    } else {
      return 0;
    }
    
    const boardSizeMultiplier = game.boardSize === 3 ? 1 : game.boardSize === 4 ? 1.5 : 2.0;
    return Math.round(basePoints * boardSizeMultiplier);
  }

  getDrawPoints(game: Game): number {
    const pointsAtStake = this.getPointsAtStake(game);
    return Math.round(pointsAtStake / 10);
  }

  private updateTurnLock(game: Game): void {
    const user = this.currentUserSignal();
    if (!user) {
      this.isPlayerTurn.set(false);
      return;
    }

    if (game.gameType === 'vs_bot' && game.status === 'waiting') {
      this.isPlayerTurn.set(true);
      return;
    }

    if (game.gameType === 'vs_bot' && game.status === 'in_progress' && game.totalMoves === 0) {
      this.isPlayerTurn.set(true);
      return;
    }

    if (game.status !== 'in_progress') {
      this.isPlayerTurn.set(false);
      return;
    }

    if (!game.currentPlayerSymbol) {
      this.logger.warn('Game is in_progress but currentPlayerSymbol is null', game);
      this.isPlayerTurn.set(false);
      return;
    }

    if (game.gameType === 'vs_bot') {
      const isPlayerTurn = game.currentPlayerSymbol === 'x';
      this.isPlayerTurn.set(isPlayerTurn);
      return;
    }

    if (game.gameType === 'pvp') {
      const isPlayer1 = Number(game.player1Id) === Number(user.userId);
      const isPlayer2 = game.player2Id !== null && Number(game.player2Id) === Number(user.userId);
      
      if (!isPlayer1 && !isPlayer2) {
        this.logger.warn('User is not a participant of this game', {
          userId: user.userId,
          player1Id: game.player1Id,
          player2Id: game.player2Id
        });
        this.isPlayerTurn.set(false);
        return;
      }

      const expectedSymbol = isPlayer1 ? 'x' : 'o';
      const currentSymbol = String(game.currentPlayerSymbol).toLowerCase();
      const isPlayerTurn = expectedSymbol === currentSymbol;
      
      this.logger.debug('updateTurnLock PvP', {
        userId: user.userId,
        isPlayer1,
        isPlayer2,
        expectedSymbol,
        currentSymbol,
        currentPlayerSymbol: game.currentPlayerSymbol,
        isPlayerTurn
      });
      
      this.isPlayerTurn.set(isPlayerTurn);
      return;
    }

    this.isPlayerTurn.set(false);
  }

  private updateWinningCells(game: Game): void {
    const size = game.boardSize;
    const board = game.boardState;
    const lines: Array<Array<{ row: number; col: number }>> = [];
    for (let r = 0; r < size; r++) {
      lines.push(Array.from({ length: size }, (_, c) => ({ row: r, col: c })));
    }
    for (let c = 0; c < size; c++) {
      lines.push(Array.from({ length: size }, (_, r) => ({ row: r, col: c })));
    }
    lines.push(Array.from({ length: size }, (_, i) => ({ row: i, col: i })));
    lines.push(Array.from({ length: size }, (_, i) => ({ row: i, col: size - 1 - i })));
    for (const line of lines) {
      const symbols = line.map((p) => board[p.row]?.[p.col] ?? null);
      if (symbols.every((s) => s != null && s === symbols[0])) {
        this.winningCells.set(line);
        return;
      }
    }
    this.winningCells.set([]);
  }
}


