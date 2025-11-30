import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, filter, switchMap, take, map, Observable } from 'rxjs';
import { of } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { RankingService } from '../../services/ranking.service';
import { GameService } from '../../services/game.service';
import { UserService } from '../../services/user.service';
import { TranslateService } from '../../services/translate.service';
import { User } from '../../models/user.model';
import { UpdateUserResponse } from '../../services/user.service';
import { getAvatarPath } from '../../utils/avatar.util';
import { Ranking } from '../../models/ranking.model';
import { Game } from '../../models/game.model';
import { UserStatsComponent } from '../../components/profile/user-stats.component';
import { LastGameCardComponent } from '../../components/profile/last-game-card.component';
import { EditUsernameDialogComponent } from '../../components/profile/edit-username-dialog.component';
import { AvatarSelectorComponent } from '../../components/profile/avatar-selector.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AsyncPipe,
    ButtonModule,
    ToastModule,
    TooltipModule,
    UserStatsComponent,
    LastGameCardComponent,
    EditUsernameDialogComponent,
    AvatarSelectorComponent,
  ],
  providers: [MessageService],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly rankingService = inject(RankingService);
  private readonly gameService = inject(GameService);
  private readonly userService = inject(UserService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly currentUser$ = this.authService.getCurrentUser();
  readonly userRanking$ = new BehaviorSubject<Ranking | null>(null);
  readonly lastGame$ = new BehaviorSubject<Game | null>(null);
  readonly showEditDialog = signal<boolean>(false);
  readonly showAvatarDialog = signal<boolean>(false);
  readonly isSavingAvatar = signal<boolean>(false);

  @ViewChild(EditUsernameDialogComponent) editUsernameDialog?: EditUsernameDialogComponent;

  ngOnInit(): void {
    this.currentUser$
      .pipe(
        filter((user): user is User => user !== null),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((user) => {
        if (user.isGuest) {
          this.router.navigate(['/']).then().catch(() => {});
          return;
        }
        this.loadUserRanking(user.userId);
        this.loadLastGame();
      });
  }

  onEditUsername(): void {
    this.showEditDialog.set(true);
  }

  onCloseEditDialog(): void {
    this.showEditDialog.set(false);
  }

  onEditAvatar(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.showAvatarDialog.set(true);
  }

  onAvatarDialogVisibleChange(visible: boolean): void {
    this.showAvatarDialog.set(visible);
  }

  onCloseAvatarDialog(): void {
    this.showAvatarDialog.set(false);
    this.isSavingAvatar.set(false);
  }

  onSaveAvatar(newAvatar: number): void {
    if (this.isSavingAvatar()) {
      return;
    }

    this.updateUserProfile(
      (user) => this.userService.updateAvatar(user.userId, newAvatar),
      'profile.avatar.updateSuccess',
      () => this.showAvatarDialog.set(false),
      this.isSavingAvatar
    );
  }

  onSaveUsername(newUsername: string): void {
    if (this.editUsernameDialog) {
      this.editUsernameDialog.setLoading(true);
    }

    this.updateUserProfile(
      (user) => this.userService.updateUser(user.userId, { username: newUsername }),
      'profile.update.successDetail',
      () => {
        if (this.editUsernameDialog) {
          this.editUsernameDialog.setLoading(false);
        }
        this.showEditDialog.set(false);
      }
    );
  }

  private updateUserProfile(
    updateFn: (user: User) => Observable<UpdateUserResponse>,
    successMessageKey: string,
    onSuccess?: () => void,
    loadingSignal?: ReturnType<typeof signal<boolean>>
  ): void {
    if (loadingSignal?.()) {
      return;
    }

    this.currentUser$
      .pipe(
        filter((user): user is User => user !== null && !user.isGuest),
        take(1),
        switchMap((user) => {
          if (loadingSignal) {
            loadingSignal.set(true);
          }
          return updateFn(user).pipe(
            switchMap((updatedUser) => {
              return this.currentUser$.pipe(
                filter((currentUser): currentUser is User => currentUser !== null),
                take(1),
                map((currentUser) => ({ updatedUser, currentUser }))
              );
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ updatedUser, currentUser }) => {
          if (updatedUser && currentUser) {
            const user: User = {
              userId: updatedUser.userId,
              username: updatedUser.username,
              email: currentUser.email,
              isGuest: updatedUser.isGuest,
              avatar: updatedUser.avatar,
              totalPoints: updatedUser.totalPoints,
              gamesPlayed: updatedUser.gamesPlayed,
              gamesWon: updatedUser.gamesWon,
              createdAt: currentUser.createdAt,
              lastSeenAt: currentUser.lastSeenAt,
            };
            this.authService.updateCurrentUser(user);
            if (loadingSignal) {
              loadingSignal.set(false);
            }
            if (onSuccess) {
              onSuccess();
            }
            this.messageService.add({
              key: 'profile',
              severity: 'success',
              summary: this.translate.translate('profile.update.success'),
              detail: this.translate.translate(successMessageKey),
            });
          }
        },
        error: (error: HttpErrorResponse) => {
          if (loadingSignal) {
            loadingSignal.set(false);
          }
          if (this.editUsernameDialog) {
            this.editUsernameDialog.setLoading(false);
          }
          this.handleUpdateError(error);
        },
      });
  }

  onContinueGame(gameId: number): void {
    this.router.navigate(['/game', gameId]);
  }

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/auth/login']).then().catch(() => {});
      },
      error: () => {
        this.router.navigate(['/auth/login']).then().catch(() => {});
      }
    });
  }

  getAvatarPath(avatar: number | null | undefined): string {
    return getAvatarPath(avatar);
  }

  getWinRate(user: User): number {
    if (!user || !user.gamesPlayed || user.gamesPlayed === 0) {
      return 0;
    }
    return Math.round((user.gamesWon / user.gamesPlayed) * 100);
  }

  private loadUserRanking(userId: number): void {
    this.rankingService
      .getUserRanking(userId)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          return of<Ranking | null>(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (ranking) => {
          this.userRanking$.next(ranking as Ranking | null);
        },
        error: () => {
          this.userRanking$.next(null);
        },
      });
  }

  private loadLastGame(): void {
    this.gameService
      .getSavedGame()
      .pipe(
        catchError(() => {
          return of<Game | null>(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (game) => {
          this.lastGame$.next(game as Game | null);
        },
        error: () => {
          this.lastGame$.next(null);
        },
      });
  }

  private handleUpdateError(error: HttpErrorResponse): void {
    let message = this.translate.translate('profile.update.error');
    let summary = this.translate.translate('profile.update.errorSummary');

    if (error.status === 409) {
      message = this.translate.translate('profile.update.conflict');
    } else if (error.status === 403) {
      message = this.translate.translate('profile.update.forbidden');
    } else if (error.status === 404) {
      message = this.translate.translate('profile.update.notFound');
    } else if (error.status === 401) {
      message = this.translate.translate('profile.update.unauthorized');
      this.router.navigate(['/auth/login']);
    }

    this.messageService.add({
      key: 'profile',
      severity: 'error',
      summary,
      detail: message,
    });
  }
}

