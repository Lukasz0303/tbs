import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Router } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { TranslateService } from '../../services/translate.service';
import { AuthService } from '../../services/auth.service';
import { GameService } from '../../services/game.service';
import { LoggerService } from '../../services/logger.service';
import { Game } from '../../models/game.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { catchError, of, switchMap, take } from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, AsyncPipe, ToastModule, ButtonModule],
  providers: [MessageService],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly gameService = inject(GameService);
  readonly translateService = inject(TranslateService);
  private readonly messageService = inject(MessageService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);

  readonly currentUser$ = this.authService.getCurrentUser();
  readonly savedGame = signal<Game | null>(null);
  readonly isLoadingSavedGame = signal<boolean>(false);

  ngOnInit(): void {
    this.checkForSavedGame();
  }

  private checkForSavedGame(): void {
    this.authService
      .getCurrentUser()
      .pipe(
        take(1),
        switchMap((user) => {
          if (!user || user.isGuest || !this.authService.isAuthenticated()) {
            return of(null);
          }
          this.isLoadingSavedGame.set(true);
          return this.gameService.getSavedGame();
        }),
        catchError(() => {
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (game) => {
          this.isLoadingSavedGame.set(false);
          this.savedGame.set(game);
        },
      });
  }

  continueGame(): void {
    const game = this.savedGame();
    if (!game || !game.gameId) {
      this.notifyError('home.error.savedGame');
      return;
    }
    this.router.navigate(['/game', game.gameId]).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  startNewGame(): void {
    this.router.navigate(['/game-options'], { queryParams: { new: true } }).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  playAsGuest(): void {
    this.router.navigate(['/game-options']).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  goToRegister(): void {
    this.router.navigate(['/auth/register']).catch((error) => {
      this.notifyError('home.error.navigation');
      this.handleError(error);
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/']).catch((error) => {
          this.notifyError('home.error.navigation');
          this.handleError(error);
        });
      },
      error: (error) => {
        this.handleError(error);
        this.router.navigate(['/']).catch((navError) => {
          this.notifyError('home.error.navigation');
          this.handleError(navError);
        });
      }
    });
  }

  private handleError(error: unknown): void {
    this.logger.error('Error in HomeComponent:', error);
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
