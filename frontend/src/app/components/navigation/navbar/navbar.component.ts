import { ChangeDetectionStrategy, Component, inject, computed, effect, ChangeDetectorRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { TranslateService } from '../../../services/translate.service';
import { AudioSettingsService } from '../../../services/audio-settings.service';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div class="sticky top-0 z-50 w-full">
      <header class="w-full border-b border-white/20 bg-slate-900/40 backdrop-blur supports-[backdrop-filter]:backdrop-blur">
        <div class="mx-auto flex h-12 max-w-6xl items-center justify-between px-3">
          <a routerLink="/" class="flex items-center gap-2 font-semibold tracking-wide text-white hover:text-white/90">
            <img src="assets/logo_2.png" alt="Logo" class="h-8 w-8 object-contain" />
            <span>{{ translateService.translate('home.title') }}</span>
          </a>
          <nav class="flex items-center gap-3 text-sm">
            <a routerLink="/" class="text-white/90 hover:text-white">{{ translateService.translate('home.menu.start') }}</a>
            @if (isAuthenticated()) {
              <a routerLink="/game/matchmaking" class="text-white/80 hover:text-white">{{ translateService.translate('home.menu.matchmaking') }}</a>
              <a routerLink="/leaderboard" class="text-white/80 hover:text-white">{{ translateService.translate('home.menu.ranking') }}</a>
            }
            <button
              type="button"
              class="flex h-8 w-8 items-center justify-center rounded-full border border-white/30 text-white/80 transition-all hover:border-white/60 hover:text-white hover:bg-white/5"
              (click)="toggleMuteAll()"
              [title]="isMuted() ? translateService.translate('settings.audio.enable') : translateService.translate('settings.audio.disableAll')"
              [attr.aria-label]="isMuted() ? translateService.translate('settings.audio.enable') : translateService.translate('settings.audio.disableAll')"
            >
              <i [class]="isMuted() ? 'pi pi-volume-off' : 'pi pi-volume-up'" class="text-base leading-none"></i>
            </button>
            @if (isAuthenticated()) {
              <div class="flex items-center gap-2 ml-3 pl-3 border-l border-white/20">
                <a 
                  routerLink="/profile" 
                  class="flex items-center justify-center w-8 h-8 rounded hover:bg-white/10 transition-all text-white/80 hover:text-white"
                  [title]="translateService.translate('home.menu.profile')"
                  [attr.aria-label]="translateService.translate('home.menu.profile')"
                >
                  <i class="pi pi-user text-base leading-none"></i>
                </a>
              </div>
            }
          </nav>
        </div>
      </header>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  protected readonly translateService = inject(TranslateService);
  private readonly audioSettingsService = inject(AudioSettingsService);
  private readonly audioSettingsSignal = toSignal(this.audioSettingsService.settings$, {
    initialValue: this.audioSettingsService.getSettings(),
  });

  readonly currentUser$ = this.authService.getCurrentUser();
  private readonly currentUserSignal = toSignal(this.currentUser$, { initialValue: null });
  readonly isAuthenticated = computed(() => {
    const user = this.currentUserSignal();
    return !!user && !user.isGuest;
  });

  readonly isMuted = computed(() => {
    const settings = this.audioSettingsSignal();
    return !settings.musicEnabled && !settings.clickSoundEnabled;
  });

  constructor() {
    effect(() => {
      this.currentUserSignal();
      this.cdr.markForCheck();
    });
  }

  toggleMuteAll(): void {
    this.audioSettingsService.toggleMuteAll();
  }
}


