import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { take, switchMap, of, throwError, delay, EMPTY, tap, catchError, finalize, Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { TranslateService } from '../../services/translate.service';
import { GameService } from '../../services/game.service';
import { LoggerService } from '../../services/logger.service';
import { BotDifficulty, Game } from '../../models/game.model';

type GameMode = BotDifficulty | 'pvp';
type BoardSize = 3 | 4 | 5;

const VALID_GAME_MODES: readonly GameMode[] = ['easy', 'medium', 'hard', 'pvp'] as const;
const VALID_BOARD_SIZES: readonly BoardSize[] = [3, 4, 5] as const;

@Component({
  selector: 'app-game-options',
  standalone: true,
  imports: [CommonModule, ToastModule, ButtonModule],
  providers: [MessageService],
  templateUrl: './game-options.component.html',
  styleUrls: ['./game-options.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameOptionsComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly translateService = inject(TranslateService);
  private readonly messageService = inject(MessageService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);

  selectedGameMode = signal<GameMode>('easy');
  selectedBoardSize = signal<BoardSize>(3);
  readonly isCreatingGuestSession = signal<boolean>(false);
  readonly isStartingGame = signal<boolean>(false);

  selectGameMode(mode: GameMode): void {
    if (!VALID_GAME_MODES.includes(mode as typeof VALID_GAME_MODES[number])) {
      return;
    }
    this.selectedGameMode.set(mode);
  }

  isGameModeSelected(mode: GameMode): boolean {
    return this.selectedGameMode() === mode;
  }

  selectBoardSize(size: BoardSize): void {
    if (!VALID_BOARD_SIZES.includes(size)) {
      return;
    }
    this.selectedBoardSize.set(size);
  }

  isBoardSizeSelected(size: BoardSize): boolean {
    return this.selectedBoardSize() === size;
  }

  onCancel(): void {
    this.router.navigate(['/']).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  onStartGame(): void {
    if (this.isStartingGame() || this.isCreatingGuestSession()) {
      return;
    }

    this.isStartingGame.set(true);
    this.authService
      .getCurrentUser()
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => {
          if (user) {
            const token = this.authService.getAuthToken();
            if (!token) {
              this.createGuestSession();
              return;
            }
            this.resumeOrCreateGame();
            return;
          }
          this.createGuestSession();
        },
        error: () => {
          this.createGuestSession();
        },
      });
  }

  private createGuestSession(): void {
    this.isCreatingGuestSession.set(true);
    this.authService
      .createGuestSession()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        delay(0),
        switchMap(() => {
          const token = this.authService.getAuthToken();
          if (!token) {
            return throwError(() => new Error('Failed to create guest session'));
          }
          return of(null);
        }),
        finalize(() => this.isCreatingGuestSession.set(false))
      )
      .subscribe({
        next: () => {
          this.resumeOrCreateGame();
        },
        error: (error) => {
          this.isStartingGame.set(false);
          if (error.status === 0) {
            this.notifyError('home.error.connection');
            this.handleError(error);
            return;
          }
          this.notifyError('home.error.guestSession');
          this.handleError(error);
        },
      });
  }

  private resumeOrCreateGame(): void {
    const selectedMode = this.selectedGameMode();
    const selectedSize = this.selectedBoardSize();
    
    const token = this.authService.getAuthToken();
    if (!token) {
      this.isStartingGame.set(false);
      this.notifyError('home.error.connection');
      return;
    }
    
    const shouldCreateNew = this.route.snapshot.queryParams['new'] === 'true';
    
    if (shouldCreateNew) {
      this.createGame(selectedMode, selectedSize)
        .pipe(
          catchError((error) => this.handleGameCreationError(error, selectedMode, selectedSize)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe();
      return;
    }
    
    this.gameService
      .getSavedGame()
      .pipe(
        take(1),
        switchMap((existing) => {
          if (existing && existing.status === 'waiting' && existing.totalMoves === 0) {
            this.navigateTo(['/game', existing.gameId]);
            return EMPTY;
          }
          
          return this.createGame(selectedMode, selectedSize);
        }),
        catchError((error) => this.handleGameCreationError(error, selectedMode, selectedSize)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private createGame(selectedMode: GameMode, selectedSize: BoardSize): Observable<Game> {
    if (selectedMode === 'pvp') {
      this.navigateTo(['/game/matchmaking'], { queryParams: { boardSize: selectedSize } });
      return EMPTY;
    }
    
    const gameObservable = this.gameService.createBotGame(selectedMode as BotDifficulty, selectedSize);
    
    return gameObservable.pipe(
      tap((game) => {
        this.navigateTo(['/game', game.gameId]);
      }),
      catchError((error) => this.handleGameCreationError(error, selectedMode, selectedSize))
    );
  }

  private handleGameCreationError(error: unknown, selectedMode: GameMode, selectedSize: BoardSize): Observable<Game> {
    this.isStartingGame.set(false);
    
    if (this.isConnectionError(error)) {
      this.notifyError('home.error.connection');
      this.handleError(error);
      return EMPTY;
    }
    
    if (this.isAuthError(error)) {
      this.authService.clearAuthToken();
      this.authService.updateCurrentUser(null);
      this.createGuestSession();
      return EMPTY;
    }
    
    if (this.isNotFoundError(error)) {
      return this.createGame(selectedMode, selectedSize).pipe(
        catchError((createError) => this.handleGameCreationError(createError, selectedMode, selectedSize))
      );
    }
    
    this.notifyError('home.error.navigation');
    this.handleError(error);
    return EMPTY;
  }

  private isConnectionError(error: unknown): boolean {
    return error instanceof HttpErrorResponse && (error.status === 0 || error.status === 404);
  }

  private isAuthError(error: unknown): boolean {
    return error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);
  }

  private isNotFoundError(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 404;
  }

  private navigateTo(commands: Array<string | number>, extras?: { queryParams?: Record<string, any> }): void {
    this.router.navigate(commands, extras).catch((error) => {
      this.isStartingGame.set(false);
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  private handleError(error: unknown): void {
    this.logger.error('Error in GameOptionsComponent:', error);
  }

  private notifyError(messageKey: string): void {
    this.messageService.add({
      key: 'game-options',
      severity: 'error',
      summary: this.translateService.translate('home.error.title'),
      detail: this.translateService.translate(messageKey),
      life: 5000,
    });
  }
}

