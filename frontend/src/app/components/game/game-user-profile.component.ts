import { ChangeDetectionStrategy, Component, Input, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { User } from '../../models/user.model';
import { Game, PlayerSymbol } from '../../models/game.model';
import { TranslateService } from '../../services/translate.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { getAvatarPath } from '../../utils/avatar.util';
import { AvatarSelectorComponent } from '../profile/avatar-selector.component';
import { switchMap, take } from 'rxjs';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';

@Component({
  selector: 'app-game-user-profile',
  standalone: true,
  imports: [CommonModule, ButtonModule, TooltipModule, AvatarSelectorComponent, ToastModule],
  providers: [MessageService],
  templateUrl: './game-user-profile.component.html',
  styleUrls: ['./game-user-profile.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameUserProfileComponent {
  private readonly userSignal = signal<User | null>(null);
  private readonly gameSignal = signal<Game | null>(null);

  @Input() set user(value: User | null) {
    this.userSignal.set(value);
  }
  get user(): User | null {
    return this.userSignal();
  }

  @Input() set game(value: Game | null) {
    this.gameSignal.set(value);
  }
  get game(): Game | null {
    return this.gameSignal();
  }

  @Input() remainingSeconds: number = 10;
  @Input() isActiveTurn: boolean = false;
  @Input() gameId: number | null = null;
  @Input() onSurrender: ((gameId: number) => void) | null = null;

  readonly translate = inject(TranslateService);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  readonly showAvatarDialog = signal<boolean>(false);
  readonly isSavingAvatar = signal<boolean>(false);
  private readonly currentUserSignal = toSignal(this.authService.getCurrentUser(), { initialValue: null });

  readonly displayName = computed(() => {
    const user = this.userSignal();
    if (!user) {
      return '-';
    }
    return user.username || this.translate.translate('profile.guest');
  });

  readonly playerSymbol = computed(() => {
    const user = this.userSignal();
    const game = this.gameSignal();
    if (!user || !game) {
      return null;
    }
    if (game.gameType === 'vs_bot') {
      return 'x';
    }
    return Number(game.player1Id) === Number(user.userId) ? 'x' : 'o';
  });

  readonly avatarPath = computed(() => {
    const user = this.userSignal();
    return getAvatarPath(user?.avatar);
  });

  readonly canEditAvatar = computed(() => {
    const user = this.userSignal();
    const currentUser = this.currentUserSignal();
    
    if (!user || !currentUser) {
      return false;
    }
    
    const isCurrentUser = Number(user.userId) === Number(currentUser.userId);
    return isCurrentUser && !user.isGuest;
  });

  readonly winRate = computed(() => {
    const user = this.userSignal();
    if (!user || !user.gamesPlayed || user.gamesPlayed === 0) {
      return 0;
    }
    return Math.round((user.gamesWon / user.gamesPlayed) * 100);
  });

  onEditAvatar(): void {
    if (this.canEditAvatar()) {
      this.showAvatarDialog.set(true);
    }
  }

  onCloseAvatarDialog(): void {
    this.showAvatarDialog.set(false);
  }

  onSaveAvatar(newAvatar: number): void {
    const user = this.userSignal();
    if (!user || user.isGuest || this.isSavingAvatar()) {
      return;
    }

    this.isSavingAvatar.set(true);
    this.userService
      .updateAvatar(user.userId, newAvatar)
      .pipe(
        switchMap(() => {
          return this.authService.loadCurrentUser();
        }),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.isSavingAvatar.set(false);
          this.messageService.add({
            key: 'game-avatar',
            severity: 'success',
            summary: this.translate.translate('profile.avatar.updateSuccess'),
            detail: '',
          });
          this.showAvatarDialog.set(false);
        },
        error: (error) => {
          this.isSavingAvatar.set(false);
          this.messageService.add({
            key: 'game-avatar',
            severity: 'error',
            summary: this.translate.translate('profile.update.errorSummary'),
            detail: error.error?.message || this.translate.translate('profile.update.error'),
          });
        },
      });
  }
}

