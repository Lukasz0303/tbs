import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { interval, of } from 'rxjs';
import { take, catchError, debounceTime } from 'rxjs/operators';
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
        this.handleError(error);
        this.router.navigate(['/']);
        throw error;
      })
    ).subscribe({
      next: (response) => {
        this.isInQueue.set(true);
        this.estimatedWaitTime.set(response.estimatedWaitTime || 0);
        
        if (response.message?.includes('Match found')) {
          this.checkForCreatedGame();
          return;
        }
        
        this.startPolling();
        this.startWaitTimeCounter();
      },
      error: (error: unknown) => {
        this.handleError(error);
        this.router.navigate(['/']);
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
    this.gameService.getSavedGame().pipe(
      take(1),
      takeUntilDestroyed(this.destroyRef),
      catchError(() => {
        this.logger.error('Error checking for created game');
        return of<Game | null>(null);
      })
    ).subscribe({
      next: (game: Game | null) => {
        if (game && this.isActivePvpGame(game)) {
          this.router.navigate(['/game', game.gameId]);
        } else {
          this.startPolling();
          this.startWaitTimeCounter();
        }
      }
    });
  }

  private startPolling(): void {
    interval(5000).pipe(
      debounceTime(500),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.checkMatchmakingStatus();
    });
  }

  private checkMatchmakingStatus(): void {
    this.gameService.getSavedGame().pipe(
      take(1),
      catchError((error) => {
        this.logger.error('Error checking matchmaking status:', error);
        return of<Game | null>(null);
      })
    ).subscribe({
      next: (game: Game | null) => {
        if (game && this.isActivePvpGame(game)) {
          this.router.navigate(['/game', game.gameId]);
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
        this.isCancelling.set(false);
        this.handleError(error);
        throw error;
      })
    ).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'info',
          summary: this.translateService.translate('matchmaking.cancelled.title'),
          detail: this.translateService.translate('matchmaking.cancelled.detail'),
          life: 3000,
        });
        this.router.navigate(['/']);
      },
      error: () => {
        this.isCancelling.set(false);
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

