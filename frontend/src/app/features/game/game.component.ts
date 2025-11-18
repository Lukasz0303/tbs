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
import { Game } from '../../models/game.model';
import { GameBoardComponent } from '../../components/game/game-board.component';
import { GameInfoComponent } from '../../components/game/game-info.component';
import { GameTimerComponent } from '../../components/game/game-timer.component';
import { AuthService } from '../../services/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { GameBotIndicatorComponent } from '../../components/game/game-bot-indicator.component';
import { GameResultDialogComponent } from '../../components/game/game-result-dialog.component';
import { WebSocketDTOs } from '../../models/websocket.model';

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
    GameInfoComponent,
    GameTimerComponent,
    GameBotIndicatorComponent,
    GameResultDialogComponent,
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

  readonly isBotThinking = signal<boolean>(false);
  readonly showResult = signal<boolean>(false);
  readonly remainingSeconds = signal<number>(10);
  readonly isPlayerTurn = signal<boolean>(true);
  readonly winningCells = signal<Array<{ row: number; col: number }>>([]);
  readonly currentUser$ = this.authService.getCurrentUser();
  private readonly currentUserSignal = toSignal(this.authService.getCurrentUser(), { initialValue: null });

  private gameId: number | null = null;

  private readonly gameState$ = new BehaviorSubject<{ loading: boolean; game: Game | null }>({
    loading: true,
    game: null,
  });

  readonly vm = toSignal(this.gameState$, { initialValue: { loading: true, game: null as Game | null } });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const gameId = Number(params.get('gameId'));
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
        error: () => {},
      });
  }

  private updateGameState(game: Game | null, loading: boolean = false): void {
    this.gameState$.next({ loading, game });
  }

  private loadGame(): void {
    if (!this.gameId) {
      return;
    }

    this.gameService
      .getGame(this.gameId)
      .pipe(
        switchMap((game) => {
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

    interval(2000)
      .pipe(
        switchMap(() => {
          if (!this.gameId) {
            return EMPTY;
          }
          return this.gameService.getGame(this.gameId!);
        }),
        map((game) => {
          const currentGame = this.vm()?.game;
          if (currentGame && this.hasGameChanged(currentGame, game)) {
            return game;
          }
          return null;
        }),
        filter((game): game is Game => game !== null),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (game) => {
          this.updateLocalGame(game);
          this.updateTurnLock(game);
          this.updateWinningCells(game);
          this.handleGameStatusChange(game);

          if (game.gameType === 'vs_bot' && (game.status === 'in_progress' || game.status === 'waiting')) {
            this.authService
              .getCurrentUser()
              .pipe(take(1), takeUntilDestroyed(this.destroyRef))
              .subscribe((user) => {
                if (user && game.currentPlayerSymbol === 'o' && !this.isBotThinking()) {
                  this.makeBotMove();
                }
              });
          }
        },
        error: () => {},
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
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.websocketService
            .getMessages()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (message) => this.handleWebSocketMessage(message),
              error: (error) => this.handleWebSocketError(error),
            });
        },
        error: (error) => {
          this.handleWebSocketError(error);
        },
      });
  }

  private startTimer(): void {
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const state = this.vm();
        const game = state?.game;
        if (game && game.gameType === 'pvp' && game.status === 'in_progress' && game.lastMoveAt) {
          const elapsed = (Date.now() - new Date(game.lastMoveAt).getTime()) / 1000;
          const remaining = Math.max(0, 10 - elapsed);
          this.remainingSeconds.set(Math.floor(remaining));

          if (remaining <= 0) {
            this.checkTimeout();
          }
        }
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
        error: () => {},
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
      return;
    }

    if (this.isMoveDisabled(game)) {
      this.messageService.add({
        severity: 'warn',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.moveNotAllowed'),
      });
      return;
    }

    if (game.boardState[event.row]?.[event.col] !== null) {
      this.messageService.add({
        severity: 'warn',
        summary: this.translate.translate('game.error.title'),
        detail: this.translate.translate('game.error.cellOccupied'),
      });
      return;
    }

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
          if (user && updated.gameType === 'vs_bot' && updated.status === 'in_progress') {
            if (updated.currentPlayerSymbol && updated.currentPlayerSymbol === 'o' && !this.isBotThinking()) {
              this.makeBotMove();
            }
          }
        },
        error: (error) => this.handleMoveError(error),
      });
  }

  private sendMoveViaWebSocket(move: { row: number; col: number }, game: Game): void {
    const user = this.currentUserSignal();
    if (!user) {
      return;
    }

    const playerSymbol = game.player1Id === user.userId ? 'x' : 'o';
    this.websocketService
      .sendMove(move.row, move.col, playerSymbol)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (error) => this.handleMoveError(error),
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
    switch (message.type) {
      case 'MOVE_ACCEPTED':
        if (this.isMoveAcceptedPayload(message.payload)) {
          this.updateGameFromMove(message.payload);
        }
        break;
      case 'MOVE_REJECTED':
        if (this.isMoveRejectedPayload(message.payload)) {
          this.messageService.add({
            severity: 'error',
            summary: this.translate.translate('game.error.title'),
            detail: message.payload.reason || this.translate.translate('game.error.move'),
          });
        }
        break;
      case 'OPPONENT_MOVE':
        if (this.isOpponentMovePayload(message.payload)) {
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

  private updateGameFromMove(payload: WebSocketDTOs.MoveAcceptedMessage['payload'] | WebSocketDTOs.OpponentMoveMessage['payload']): void {
    const state = this.vm();
    const game = state?.game;
    if (game) {
      const updated: Game = {
        ...game,
        boardState: payload.boardState,
        currentPlayerSymbol: payload.currentPlayerSymbol,
        lastMoveAt: payload.nextMoveAt,
      };
      this.updateLocalGame(updated);
      this.updateTurnLock(updated);
      this.updateWinningCells(updated);
    }
  }

  private updateGameFromPayload(payload: WebSocketDTOs.GameUpdateMessage['payload'] | WebSocketDTOs.GameEndedMessage['payload']): void {
    const state = this.vm();
    const game = state?.game;
    if (game) {
      const updated: Game = {
        ...game,
        status: payload.status,
        winnerId: payload.winner?.userId || null,
        boardState: 'finalBoardState' in payload ? payload.finalBoardState : payload.boardState,
      };
      this.updateLocalGame(updated);
      this.updateTurnLock(updated);
      this.updateWinningCells(updated);
    }
  }

  private handleGameStatusChange(game: Game): void {
    if (game.status === 'finished' || game.status === 'draw' || game.status === 'abandoned') {
      setTimeout(() => {
        this.showResult.set(true);
      }, 400);
    }
  }

  private hasGameChanged(oldGame: Game, newGame: Game): boolean {
    return (
      oldGame.status !== newGame.status ||
      oldGame.currentPlayerSymbol !== newGame.currentPlayerSymbol ||
      JSON.stringify(oldGame.boardState) !== JSON.stringify(newGame.boardState)
    );
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
  }

  isMoveDisabled(game: Game): boolean {
    const isWaitingVsBot = game.gameType === 'vs_bot' && game.status === 'waiting';
    const isInProgressNoMoves = game.gameType === 'vs_bot' && game.status === 'in_progress' && game.totalMoves === 0;

    if (isWaitingVsBot || isInProgressNoMoves) {
      return false;
    }

    if (game.status !== 'in_progress') {
      return true;
    }

    return !this.isPlayerTurn();
  }

  getGameTitle(game: Game): string {
    if (game.gameType === 'vs_bot') {
      return `${this.translate.translate('game.title.bot')} (${game.botDifficulty || 'easy'})`;
    }
    return this.translate.translate('game.title.pvp');
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

    if (game.status !== 'in_progress' || !game.currentPlayerSymbol) {
      this.isPlayerTurn.set(false);
      return;
    }

    if (game.gameType === 'vs_bot') {
      const isPlayerTurn = game.currentPlayerSymbol === 'x';
      this.isPlayerTurn.set(isPlayerTurn);
      return;
    }

    const expectedSymbol = game.player1Id === user.userId ? 'x' : game.player2Id === user.userId ? 'o' : null;
    const isPlayerTurn = expectedSymbol === game.currentPlayerSymbol;
    this.isPlayerTurn.set(isPlayerTurn);
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


