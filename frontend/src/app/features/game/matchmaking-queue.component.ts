import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { interval, of } from 'rxjs';
import { catchError, take, filter, throttleTime } from 'rxjs/operators';
import { asyncScheduler } from 'rxjs';
import { trigger, transition, style, animate } from '@angular/animations';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { MatchmakingService, PlayerQueueStatus, QueueStatusResponse, BoardSizeEnum, QueuePlayerStatusEnum } from '../../services/matchmaking.service';
import { TranslateService } from '../../services/translate.service';
import { LoggerService } from '../../services/logger.service';
import { AuthService } from '../../services/auth.service';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-matchmaking-queue',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    TagModule,
    ProgressSpinnerModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './matchmaking-queue.component.html',
  styleUrls: ['./matchmaking-queue.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('150ms ease-in', style({ opacity: 0, transform: 'translateY(-5px)' }))
      ])
    ])
  ],
})
export class MatchmakingQueueComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly matchmakingService = inject(MatchmakingService);
  private readonly messageService = inject(MessageService);
  protected readonly translateService = inject(TranslateService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly gameService = inject(GameService);
  private readonly destroyRef = inject(DestroyRef);

  readonly queueStatus = signal<QueueStatusResponse | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly isJoining = signal<boolean>(false);
  readonly currentUserId = signal<number | null>(null);
  readonly hasActiveGame = signal<boolean>(false);
  readonly isPageVisible = signal<boolean>(true);

  readonly players = computed(() => this.queueStatus()?.players ?? []);
  readonly totalCount = computed(() => this.queueStatus()?.totalCount ?? 0);
  readonly hasPlayers = computed(() => this.totalCount() > 0);

  private visibilityChangeHandler?: () => void;
  private abortController = new AbortController();

  ngOnInit(): void {
    this.route.queryParams.pipe(take(1)).subscribe(params => {
      const boardSize = params['boardSize'];
      if (boardSize) {
        const size = +boardSize as 3 | 4 | 5;
        if ([3, 4, 5].includes(size)) {
          this.router.navigate(['/game/matchmaking/waiting'], { queryParams: { boardSize: size } });
          return;
        }
      }
    });

    this.authService.getCurrentUser().pipe(
      takeUntilDestroyed(this.destroyRef),
      take(1)
    ).subscribe({
      next: (user) => {
        this.currentUserId.set(user?.userId || null);
        this.checkActiveGame();
      }
    });
    
    this.setupPageVisibilityListener();
    this.loadQueueStatus();
    this.startPolling();
  }

  private setupPageVisibilityListener(): void {
    this.isPageVisible.set(!document.hidden);
    
    this.visibilityChangeHandler = () => {
      this.isPageVisible.set(!document.hidden);
      if (!document.hidden) {
        this.loadQueueStatus();
        this.checkActiveGame();
      }
    };
    
    document.addEventListener('visibilitychange', this.visibilityChangeHandler, {
      signal: this.abortController.signal
    });
    
    this.destroyRef.onDestroy(() => {
      this.abortController.abort();
      if (this.visibilityChangeHandler) {
        this.visibilityChangeHandler = undefined;
      }
    });
  }

  loadQueueStatus(): void {
    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.matchmakingService.getQueueStatus(undefined).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError((error) => {
        this.logger.error('Error loading queue status:', error);
        this.messageService.add({
          severity: 'error',
          summary: this.translateService.translate('queue.error.title'),
          detail: this.translateService.translate('queue.error.detail'),
          life: 5000,
        });
        this.isLoading.set(false);
        if (!this.queueStatus()) {
          this.queueStatus.set({ players: [], totalCount: 0 });
        }
        return of<QueueStatusResponse | null>(null);
      })
    ).subscribe({
      next: (response) => {
        if (response) {
          const currentStatus = this.queueStatus();
          
          if (this.hasQueueStatusChanged(currentStatus, response)) {
            this.logger.debug('Queue status changed, updating UI');
            this.queueStatus.set(response);
          } else {
            this.logger.debug('Queue status unchanged, skipping UI update');
          }
        }
        
        this.isLoading.set(false);
      }
    });
  }

  private hasQueueStatusChanged(
    oldStatus: QueueStatusResponse | null,
    newStatus: QueueStatusResponse
  ): boolean {
    if (!oldStatus) {
      return true;
    }

    if (oldStatus.totalCount !== newStatus.totalCount) {
      return true;
    }

    if (oldStatus.players.length !== newStatus.players.length) {
      return true;
    }

    const oldPlayersMap = new Map(
      oldStatus.players.map(p => [p.userId, p])
    );

    for (const newPlayer of newStatus.players) {
      const oldPlayer = oldPlayersMap.get(newPlayer.userId);
      
      if (!oldPlayer) {
        return true;
      }

      if (
        oldPlayer.status !== newPlayer.status ||
        oldPlayer.boardSize !== newPlayer.boardSize ||
        oldPlayer.username !== newPlayer.username ||
        oldPlayer.score !== newPlayer.score
      ) {
        return true;
      }
    }

    return false;
  }

  startPolling(): void {
    interval(5000).pipe(
      takeUntilDestroyed(this.destroyRef),
      filter(() => {
        const isPageVisible = this.isPageVisible();
        const isJoining = this.isJoining();
        const isLoading = this.isLoading();
        return isPageVisible && !isJoining && !isLoading;
      }),
      throttleTime(5000, asyncScheduler, { leading: true, trailing: false })
    ).subscribe(() => {
      this.loadQueueStatus();
      this.checkActiveGame();
    });
  }

  refreshQueue(): void {
    this.loadQueueStatus();
    this.checkActiveGame();
  }

  getBoardSizeLabel(size: string | BoardSizeEnum | number): string {
    if (typeof size === 'number' && [3, 4, 5].includes(size)) {
      return `${size}x${size}`;
    }
    const normalizedSize = String(size).toUpperCase();
    const sizeMap: Record<string, string> = {
      'THREE': '3x3',
      'FOUR': '4x4',
      'FIVE': '5x5',
      '3': '3x3',
      '4': '4x4',
      '5': '5x5',
    };
    return sizeMap[normalizedSize] || sizeMap[String(size)] || String(size);
  }

  private isValidScore(score: unknown): score is number {
    return typeof score === 'number' && !isNaN(score) && isFinite(score);
  }

  formatScore(score: number | null | undefined): string {
    if (!this.isValidScore(score)) {
      return '0';
    }
    return score.toLocaleString('pl-PL');
  }

  getStatusSeverity(status: string | QueuePlayerStatusEnum): 'success' | 'warning' | 'info' | 'danger' {
    const normalizedStatus = String(status).toUpperCase();
    const statusMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
      'WAITING': 'info',
      'MATCHED': 'warning',
      'PLAYING': 'success',
    };
    return statusMap[normalizedStatus] || 'info';
  }

  getStatusLabel(status: string | QueuePlayerStatusEnum): string {
    const normalizedStatus = String(status).toUpperCase();
    const statusMap: Record<string, string> = {
      'WAITING': 'queue.status.waiting',
      'MATCHED': 'queue.status.matched',
      'PLAYING': 'queue.status.playing',
    };
    return statusMap[normalizedStatus] || normalizedStatus;
  }

  canJoinGame(player: PlayerQueueStatus): boolean {
    const userId = this.currentUserId();
    if (!userId) {
      return false;
    }
    
    if (this.hasActiveGame()) {
      return false;
    }
    
    const normalizedStatus = String(player.status).toUpperCase();
    return normalizedStatus === 'WAITING' && player.userId !== userId;
  }

  private isGameWithStatus(game: unknown): game is { status: string } {
    return typeof game === 'object' && game !== null && 'status' in game;
  }

  private checkActiveGame(): void {
    this.gameService.getSavedGame().pipe(
      takeUntilDestroyed(this.destroyRef),
      take(1),
      catchError(() => {
        this.hasActiveGame.set(false);
        return of(null);
      })
    ).subscribe({
      next: (game) => {
        if (game && this.isGameWithStatus(game)) {
          const status = String(game.status).toLowerCase();
          this.hasActiveGame.set(status === 'waiting' || status === 'in_progress');
        } else {
          this.hasActiveGame.set(false);
        }
      }
    });
  }

  trackByUserId(_index: number, player: PlayerQueueStatus): number {
    return player.userId;
  }

  joinGame(player: PlayerQueueStatus): void {
    if (this.isJoining()) {
      return;
    }

    const userId = this.currentUserId();
    if (!userId) {
      this.messageService.add({
        severity: 'error',
        summary: this.translateService.translate('queue.join.error.title'),
        detail: this.translateService.translate('queue.join.error.detail'),
        life: 5000,
      });
      return;
    }

    if (player.userId === userId) {
      this.messageService.add({
        severity: 'warn',
        summary: this.translateService.translate('queue.join.error.title'),
        detail: this.translateService.translate('queue.join.error.ownGame'),
        life: 5000,
      });
      return;
    }

    if (!this.canJoinGame(player)) {
      this.messageService.add({
        severity: 'warn',
        summary: this.translateService.translate('queue.join.error.title'),
        detail: this.translateService.translate('queue.join.error.detail'),
        life: 5000,
      });
      return;
    }

    const rawBoardSize = player.boardSize;
    let boardSize: 3 | 4 | 5 | null = null;
    
    if (typeof rawBoardSize === 'number') {
      if (rawBoardSize === 3 || rawBoardSize === 4 || rawBoardSize === 5) {
        boardSize = rawBoardSize;
      } else {
        this.logger.warn('Invalid numeric board size', { rawBoardSize });
        this.messageService.add({
          severity: 'error',
          summary: this.translateService.translate('queue.join.error.title'),
          detail: this.translateService.translate('queue.join.error.invalidBoardSize'),
          life: 5000,
        });
        return;
      }
    } else if (typeof rawBoardSize === 'string') {
      const numValue = Number(rawBoardSize);
      if (!Number.isNaN(numValue) && (numValue === 3 || numValue === 4 || numValue === 5)) {
        boardSize = numValue as 3 | 4 | 5;
      } else {
        const boardSizeMap: Record<string, 3 | 4 | 5> = {
          'THREE': 3,
          'FOUR': 4,
          'FIVE': 5,
        };
        const normalized = rawBoardSize.toUpperCase();
        boardSize = boardSizeMap[normalized] || null;
        if (!boardSize) {
          this.logger.warn('Invalid string board size', { rawBoardSize, normalized });
          this.messageService.add({
            severity: 'error',
            summary: this.translateService.translate('queue.join.error.title'),
            detail: this.translateService.translate('queue.join.error.invalidBoardSize'),
            life: 5000,
          });
          return;
        }
      }
    } else {
      this.logger.warn('Unknown board size type', { rawBoardSize, type: typeof rawBoardSize });
      this.messageService.add({
        severity: 'error',
        summary: this.translateService.translate('queue.join.error.title'),
        detail: this.translateService.translate('queue.join.error.invalidBoardSize'),
        life: 5000,
      });
      return;
    }

    if (!boardSize) {
      this.logger.error('Could not resolve board size', { rawBoardSize });
      this.messageService.add({
        severity: 'error',
        summary: this.translateService.translate('queue.join.error.title'),
        detail: this.translateService.translate('queue.join.error.invalidBoardSize'),
        life: 5000,
      });
      return;
    }
    
    this.logger.debug('Joining game with board size', { 
      rawBoardSize, 
      rawBoardSizeType: typeof rawBoardSize,
      resolvedBoardSize: boardSize,
      playerUserId: player.userId,
      playerUsername: player.username
    });

    this.isJoining.set(true);
    this.matchmakingService.challengePlayer(player.userId, boardSize).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError((error: unknown) => {
        this.isJoining.set(false);
        this.logger.error('Error joining game:', error);
        
        let errorMessage = this.translateService.translate('queue.join.error.detail');
        
        if (error instanceof HttpErrorResponse) {
          this.logger.error('Error structure:', {
            status: error.status,
            error: error.error,
            errorError: error.error?.error,
            errorCode: error.error?.error?.code,
            errorMessage: error.error?.error?.message
          });
          
          if (error.status === 409) {
            const errorDetails = error.error?.error;
            const errorCode = errorDetails?.code;
            const backendMessage = errorDetails?.message;
            
            if (errorCode === 'USER_UNAVAILABLE' || errorCode === 'CONFLICT') {
              errorMessage = this.translateService.translate('queue.join.error.unavailable');
            } else if (backendMessage) {
              errorMessage = backendMessage;
            }
            
            this.loadQueueStatus();
          } else {
            const errorDetails = error.error?.error;
            if (errorDetails?.message) {
              errorMessage = errorDetails.message;
            }
          }
        } else if (error instanceof Error) {
          errorMessage = error.message;
        }
        
        this.messageService.add({
          severity: 'error',
          summary: this.translateService.translate('queue.join.error.title'),
          detail: errorMessage,
          life: 5000,
        });
        return of(null);
      })
    ).subscribe({
      next: (game) => {
        this.isJoining.set(false);
        if (game && 'gameId' in game) {
          this.messageService.add({
            severity: 'success',
            summary: this.translateService.translate('queue.join.success.title'),
            detail: this.translateService.translate('queue.join.success.detail'),
            life: 3000,
          });
          this.router.navigate(['/game', game.gameId]);
        }
      }
    });
  }
}

