import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { take, switchMap, of, throwError, delay, EMPTY, tap, catchError, finalize } from 'rxjs';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { TranslateService } from '../../services/translate.service';
import { GameService } from '../../services/game.service';
import { LoggerService } from '../../services/logger.service';
import { BotDifficulty } from '../../models/game.model';

type GameMode = BotDifficulty | 'pvp';
type BoardSize = 3 | 4 | 5;

const VALID_GAME_MODES = ['easy', 'medium', 'hard', 'pvp'] as const;
const VALID_BOARD_SIZES = [3, 4, 5] as const;

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ToastModule, DialogModule, ButtonModule],
  providers: [MessageService],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly translateService = inject(TranslateService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly logger = inject(LoggerService);

  showGameOptionsDialog = signal<boolean>(false);
  selectedGameMode = signal<GameMode>('easy');
  selectedBoardSize = signal<BoardSize>(3);
  readonly isCreatingGuestSession = signal<boolean>(false);

  playAsGuest(): void {
    this.showGameOptionsDialog.set(true);
  }

  onGameOptionsConfirm(): void {
    this.showGameOptionsDialog.set(false);
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

  onGameOptionsCancel(): void {
    this.showGameOptionsDialog.set(false);
  }

  goToLogin(): void {
    this.navigateTo(['/auth', 'login']);
  }

  goToRegister(): void {
    this.navigateTo(['/auth', 'register']);
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
            this.logger.warn('Token not available after creating guest session');
            return throwError(() => new Error('Failed to create guest session'));
          }
          this.logger.debug('Guest session created, token available');
          return of(null);
        }),
        finalize(() => this.isCreatingGuestSession.set(false))
      )
      .subscribe({
        next: () => {
          this.resumeOrCreateGame();
        },
        error: (error) => {
          if (error.status === 0) {
            this.logger.error('Connection error when creating guest session - backend may be unavailable', error);
            this.notifyError('home.error.connection');
            this.handleError(error);
            return;
          }
          this.notifyError('home.error.guestSession');
          this.handleError(error);
        },
      });
  }

  selectGameMode(mode: GameMode): void {
    if (!VALID_GAME_MODES.includes(mode as typeof VALID_GAME_MODES[number])) {
      this.logger.warn('Invalid game mode:', mode);
      return;
    }
    this.selectedGameMode.set(mode);
  }

  isGameModeSelected(mode: GameMode): boolean {
    return this.selectedGameMode() === mode;
  }

  selectBoardSize(size: BoardSize): void {
    if (!VALID_BOARD_SIZES.includes(size)) {
      this.logger.warn('Invalid board size:', size);
      return;
    }
    this.selectedBoardSize.set(size);
  }

  isBoardSizeSelected(size: BoardSize): boolean {
    return this.selectedBoardSize() === size;
  }

  private resumeOrCreateGame(): void {
    const selectedMode = this.selectedGameMode();
    const selectedSize = this.selectedBoardSize();
    
    const token = this.authService.getAuthToken();
    if (!token) {
      this.notifyError('home.error.connection');
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
          
          const gameObservable = selectedMode === 'pvp'
            ? this.gameService.createPvpGame(selectedSize)
            : this.gameService.createBotGame(selectedMode as BotDifficulty, selectedSize);
          
          return gameObservable.pipe(
            tap((game) => {
              this.navigateTo(['/game', game.gameId]);
            })
          );
        }),
        catchError((error) => {
          if (error.status === 0) {
            this.logger.error('Connection error - backend may be unavailable', error);
            this.notifyError('home.error.connection');
            this.handleError(error);
            return EMPTY;
          }
          if (error.status === 404) {
            this.logger.debug('No saved game found, creating new game');
            const gameObservable = selectedMode === 'pvp'
              ? this.gameService.createPvpGame(selectedSize)
              : this.gameService.createBotGame(selectedMode as BotDifficulty, selectedSize);
            
            return gameObservable.pipe(
              tap((game) => {
                this.navigateTo(['/game', game.gameId]);
              }),
              catchError((createError) => {
                if (createError.status === 0) {
                  this.logger.error('Connection error when creating game', createError);
                  this.notifyError('home.error.connection');
                  this.handleError(createError);
                  return EMPTY;
                }
                if (createError.status === 403 || createError.status === 401) {
                  this.authService.clearAuthToken();
                  this.authService.updateCurrentUser(null);
                  this.createGuestSession();
                  return EMPTY;
                }
                if (createError.status === 404) {
                  this.notifyError('home.error.connection');
                  this.handleError(createError);
                  return EMPTY;
                }
                this.notifyError('home.error.navigation');
                this.handleError(createError);
                return EMPTY;
              })
            );
          }
          if (error.status === 403 || error.status === 401) {
            this.authService.clearAuthToken();
            this.authService.updateCurrentUser(null);
            this.createGuestSession();
            return EMPTY;
          }
          this.logger.error('Error in resumeOrCreateGame', error);
          this.notifyError('home.error.navigation');
          this.handleError(error);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private navigateTo(commands: Array<string | number>): void {
    this.router.navigate(commands).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  private handleError(error: unknown): void {
    this.logger.error('HomeComponent error:', error);
  }

  private notifyError(messageKey: string): void {
    this.messageService.add({
      key: 'home',
      severity: 'error',
      summary: this.translateService.translate('home.error.title'),
      detail: this.translateService.translate(messageKey),
      life: 5000,
    });
  }
}
