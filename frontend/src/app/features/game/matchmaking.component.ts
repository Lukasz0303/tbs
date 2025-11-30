import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { interval, of, timer } from 'rxjs';
import { take, catchError, debounceTime, finalize } from 'rxjs/operators';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { MatchmakingService, MatchmakingQueueResponse } from '../../services/matchmaking.service';
import { GameService } from '../../services/game.service';
import { TranslateService } from '../../services/translate.service';
import { LoggerService } from '../../services/logger.service';
import { Game } from '../../models/game.model';

@Component({
  selector: 'app-matchmaking',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    ProgressSpinnerModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './matchmaking.component.html',
  styleUrls: ['./matchmaking.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchmakingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly matchmakingService = inject(MatchmakingService);
  private readonly gameService = inject(GameService);
  private readonly messageService = inject(MessageService);
  protected readonly translateService = inject(TranslateService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);

  readonly boardSize = signal<3 | 4 | 5>(3);
  readonly estimatedWaitTime = signal<number>(0);
  readonly statusText = signal<string>('matchmaking.searching');
  readonly isCancelling = signal<boolean>(false);
  readonly isInQueue = signal<boolean>(false);
  readonly hasError = signal<boolean>(false);
  readonly errorMessage = signal<string>('');
  readonly isNavigatingToGame = signal<boolean>(false);

  ngOnInit(): void {
    this.route.queryParams.pipe(
      take(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(params => {
      const boardSizeParam = params['boardSize'];
      const size = boardSizeParam ? Number(boardSizeParam) : 3;

      if (!Number.isInteger(size) || ![3, 4, 5].includes(size as 3 | 4 | 5)) {
        this.handleError(new Error('Invalid board size'));
        this.router.navigate(['/']);
        return;
      }
      this.boardSize.set(size as 3 | 4 | 5);
      this.joinQueue();
    });
  }

  private joinQueue(): void {
    this.matchmakingService.joinQueue(this.boardSize()).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError((error) => {
        this.logger.error('Error joining matchmaking queue:', error);
        
        let errorMsg = this.translateService.translate('matchmaking.error.detail');
        if (error instanceof HttpErrorResponse) {
          if (error.status === 401 || error.status === 403) {
            errorMsg = this.translateService.translate('matchmaking.error.auth');
          } else if (error.status === 409) {
            errorMsg = this.translateService.translate('matchmaking.error.alreadyInQueue');
          } else if (error.error?.message) {
            errorMsg = error.error.message;
          }
        } else if (error instanceof Error) {
          errorMsg = error.message;
        }
        
        this.hasError.set(true);
        this.errorMessage.set(errorMsg);
        this.statusText.set('matchmaking.error.status');
        
        this.messageService.add({
          severity: 'error',
          summary: this.translateService.translate('matchmaking.error.title'),
          detail: errorMsg,
          life: 5000,
        });
        
        return of<MatchmakingQueueResponse>({
          message: 'Error joining queue',
          estimatedWaitTime: 0
        });
      })
    ).subscribe({
      next: (response) => {
        if (response.message === 'Error joining queue') {
          return;
        }
        
        this.hasError.set(false);
        this.isInQueue.set(true);
        this.estimatedWaitTime.set(response.estimatedWaitTime || 0);
        
        this.logger.debug('Joined queue. Response:', response);
        
        if (response.message?.includes('Match found')) {
          this.logger.debug('Match found in response, checking for game...');
          this.scheduleDelayedCheck(100, () => this.checkForCreatedGame());
          return;
        }
        
        this.logger.debug('No match found yet, starting polling...');
        this.checkForCreatedGame();
        
        this.scheduleDelayedCheck(300, () => this.checkMatchmakingStatus());
        this.scheduleDelayedCheck(800, () => this.checkMatchmakingStatus());
        this.scheduleDelayedCheck(1500, () => this.checkMatchmakingStatus());
        
        this.startPolling();
        this.startWaitTimeCounter();
      }
    });
  }

  private scheduleDelayedCheck(delayMs: number, action: () => void): void {
    timer(delayMs).pipe(
      takeUntilDestroyed(this.destroyRef),
      take(1)
    ).subscribe(() => {
      if (this.isInQueue() && !this.isNavigatingToGame()) {
        action();
      }
    });
  }

  private isActivePvpGame(game: unknown): game is Game & { gameType: 'pvp'; status: 'waiting' | 'in_progress' } {
    return (
      typeof game === 'object' &&
      game !== null &&
      'gameType' in game &&
      (game as { gameType: unknown }).gameType === 'pvp' &&
      'status' in game &&
      ((game as { status: unknown }).status === 'waiting' || (game as { status: unknown }).status === 'in_progress') &&
      'gameId' in game
    );
  }

  private checkForCreatedGame(): void {
    if (this.isNavigatingToGame()) {
      return;
    }
    
    this.gameService.getActivePvpGame().pipe(
      take(1),
      takeUntilDestroyed(this.destroyRef),
      catchError(() => {
        this.logger.error('Error checking for created game');
        return of<Game | null>(null);
      })
    ).subscribe({
      next: (game: Game | null) => {
        if (this.isNavigatingToGame()) {
          return;
        }
        if (game && this.isActivePvpGame(game)) {
          this.isNavigatingToGame.set(true);
          this.logger.debug('PvP game found! Navigating to game:', game.gameId);
          this.router.navigate(['/game', game.gameId]).then((success) => {
            if (!success) {
              this.logger.error('Navigation failed for game:', game.gameId);
              this.isNavigatingToGame.set(false);
            }
          }).catch((error) => {
            this.logger.error('Error navigating to game:', error);
            this.isNavigatingToGame.set(false);
          });
        } else {
          if (!this.isInQueue()) {
            this.startPolling();
            this.startWaitTimeCounter();
          }
        }
      }
    });
  }

  private startPolling(): void {
    this.checkMatchmakingStatus();
    
    interval(2000).pipe(
      debounceTime(300),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.checkMatchmakingStatus();
    });
  }

  private checkMatchmakingStatus(): void {
    if (!this.isInQueue() || this.isNavigatingToGame()) {
      return;
    }
    
    this.logger.debug('Checking matchmaking status...');
    this.gameService.getActivePvpGame().pipe(
      take(1),
      takeUntilDestroyed(this.destroyRef),
      catchError((error) => {
        this.logger.error('Error checking matchmaking status:', error);
        return of<Game | null>(null);
      })
    ).subscribe({
      next: (game: Game | null) => {
        if (!this.isInQueue() || this.isNavigatingToGame()) {
          return;
        }
        this.logger.debug('Matchmaking status check result:', game ? `Game ${game.gameId} found` : 'No game found');
        if (game && this.isActivePvpGame(game)) {
          this.isNavigatingToGame.set(true);
          this.logger.debug('Match found! Navigating to game:', game.gameId);
          this.isInQueue.set(false);
          this.router.navigate(['/game', game.gameId]).then((success) => {
            if (!success) {
              this.logger.error('Navigation failed for game:', game.gameId);
              this.isNavigatingToGame.set(false);
            }
          }).catch((error) => {
            this.logger.error('Error navigating to game:', error);
            this.isNavigatingToGame.set(false);
          });
        }
      }
    });
  }

  private startWaitTimeCounter(): void {
    interval(1000).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      const currentWaitTime = this.estimatedWaitTime();
      if (currentWaitTime > 0) {
        this.estimatedWaitTime.set(currentWaitTime - 1);
      } else {
        this.statusText.set('matchmaking.searching');
      }
    });
  }

  onCancel(): void {
    if (this.isCancelling()) {
      return;
    }

    this.isCancelling.set(true);
    
    this.matchmakingService.leaveQueue().pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError((error) => {
        this.logger.error('Error leaving queue:', error);
        
        this.messageService.add({
          severity: 'warn',
          summary: this.translateService.translate('matchmaking.leave.error.title'),
          detail: this.translateService.translate('matchmaking.leave.error.detail'),
          life: 5000,
        });
        
        this.isCancelling.set(false);
        return of(null);
      }),
      finalize(() => {
        if (this.isInQueue()) {
          this.isInQueue.set(false);
        }
      })
    ).subscribe({
      next: (result) => {
        if (result !== null) {
          this.messageService.add({
            severity: 'info',
            summary: this.translateService.translate('matchmaking.cancelled.title'),
            detail: this.translateService.translate('matchmaking.cancelled.detail'),
            life: 3000,
          });
        }
        this.router.navigate(['/']).then((success) => {
          if (!success) {
            this.logger.warn('Navigation to home failed');
          }
        }).catch((error) => {
          this.logger.error('Error navigating to home:', error);
        });
      }
    });
  }

  private handleError(error: unknown): void {
    this.logger.error('Error in MatchmakingComponent:', error);
    
    let errorMessage = this.translateService.translate('matchmaking.error.detail');
    
    if (error instanceof HttpErrorResponse) {
      errorMessage = error.error?.message || errorMessage;
    } else if (error instanceof Error) {
      errorMessage = error.message;
    }
    
    this.messageService.add({
      severity: 'error',
      summary: this.translateService.translate('matchmaking.error.title'),
      detail: errorMessage,
      life: 5000,
    });
  }
}

