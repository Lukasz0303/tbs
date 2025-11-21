import { ChangeDetectionStrategy, Component, inject, signal, computed, effect, ChangeDetectorRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AsyncPipe, NgClass } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { TranslateService } from '../../../services/translate.service';
import { AudioSettingsService } from '../../../services/audio-settings.service';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, AsyncPipe, NgClass],
  template: `
    <div class="sticky top-0 z-50 w-full">
      <header class="w-full border-b border-white/20 bg-slate-900/40 backdrop-blur supports-[backdrop-filter]:backdrop-blur">
        <div class="mx-auto flex h-12 max-w-6xl items-center justify-between px-3">
          <a routerLink="/" class="flex items-center gap-2 font-semibold tracking-wide text-white hover:text-white/90">
            <img src="assets/logo_2.png" alt="Logo" class="h-8 w-8 object-contain" />
            <span>Kółko i krzyżyk</span>
          </a>
          <nav class="flex items-center gap-3 text-sm">
            <a routerLink="/" class="text-white/90 hover:text-white">Główna</a>
            @if (isAuthenticated()) {
              <a routerLink="/game/matchmaking" class="text-white/80 hover:text-white">{{ translateService.translate('home.menu.matchmaking') }}</a>
              <a routerLink="/leaderboard" class="text-white/80 hover:text-white">Ranking</a>
            }
            <a routerLink="/polityka-prywatnosci" class="text-white/80 hover:text-white">Polityka</a>
            <a routerLink="/regulamin" class="text-white/80 hover:text-white">Regulamin</a>
            <button
              type="button"
              class="flex h-8 w-8 items-center justify-center rounded-full border border-white/30 text-white/80 transition hover:border-white/60 hover:text-white"
              (click)="openAudioSettings()"
              [title]="translateService.translate('settings.audio.open')"
              [attr.aria-label]="translateService.translate('settings.audio.open')"
            >
              <i class="pi pi-sliders-h text-sm"></i>
            </button>
            @if (currentUser$ | async; as user) {
              <div class="flex items-center gap-2 ml-3 pl-3 border-l border-white/20">
                <span class="text-white/90">{{ user.isGuest ? translateService.translate('home.status.guest') : (user.username || user.email) }}</span>
                <button
                  (click)="onLogout()"
                  class="flex items-center justify-center w-8 h-8 rounded hover:bg-white/10 transition-colors text-white/80 hover:text-white"
                  [title]="translateService.translate('home.hero.logoutButton')"
                  [attr.aria-label]="translateService.translate('home.hero.logoutButton')"
                >
                  <i class="pi pi-sign-out text-sm"></i>
                </button>
              </div>
            }
          </nav>
        </div>
      </header>
      @if (audioSettingsDialogVisible()) {
        <section
          class="fixed inset-0 z-40 flex items-center justify-center overflow-auto bg-black/60 backdrop-blur-sm p-4 sm:p-6 md:p-8"
          (click)="closeAudioSettings()">
          <div
            class="mx-auto max-w-md w-full rounded-2xl border border-white/10 bg-slate-900/80 text-slate-100 shadow-2xl"
            (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between gap-4 border-b border-white/10 px-6 py-4">
              <h1 class="text-xl sm:text-2xl font-semibold">
                {{ translateService.translate('settings.audio.title') }}
              </h1>
              <button
                type="button"
                class="rounded-md border border-white/20 bg-white/10 px-3 py-1 text-sm text-slate-100 hover:bg-white/20"
                (click)="closeAudioSettings()">
                Zamknij
              </button>
            </div>
            <div class="px-6 py-6 space-y-4 leading-relaxed">
              <div class="flex items-start justify-between gap-3">
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-semibold text-slate-100 mb-1">{{ translateService.translate('settings.audio.music') }}</p>
                  <p class="text-xs text-slate-300 leading-relaxed">
                    {{ translateService.translate('settings.audio.musicDescription') }}
                  </p>
                </div>
                <button
                  type="button"
                  class="flex-shrink-0 rounded-md border border-white/20 bg-white/10 px-3 py-1.5 text-xs font-semibold text-slate-100 transition hover:bg-white/20"
                  [ngClass]="
                    audioSettings().musicEnabled
                      ? 'bg-white/20 border-white/30'
                      : ''
                  "
                  (click)="toggleMusic()"
                  [attr.aria-pressed]="audioSettings().musicEnabled">
                  {{
                    audioSettings().musicEnabled
                      ? translateService.translate('settings.audio.enabled')
                      : translateService.translate('settings.audio.disabled')
                  }}
                </button>
              </div>
              <div class="flex items-start justify-between gap-3">
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-semibold text-slate-100 mb-1">{{ translateService.translate('settings.audio.clickSound') }}</p>
                  <p class="text-xs text-slate-300 leading-relaxed">
                    {{ translateService.translate('settings.audio.clickSoundDescription') }}
                  </p>
                </div>
                <button
                  type="button"
                  class="flex-shrink-0 rounded-md border border-white/20 bg-white/10 px-3 py-1.5 text-xs font-semibold text-slate-100 transition hover:bg-white/20"
                  [ngClass]="
                    audioSettings().clickSoundEnabled
                      ? 'bg-white/20 border-white/30'
                      : ''
                  "
                  (click)="toggleClickSound()"
                  [attr.aria-pressed]="audioSettings().clickSoundEnabled">
                  {{
                    audioSettings().clickSoundEnabled
                      ? translateService.translate('settings.audio.enabled')
                      : translateService.translate('settings.audio.disabled')
                  }}
                </button>
              </div>
            </div>
          </div>
        </section>
      }
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
  protected readonly audioSettingsDialogVisible = signal(false);
  protected readonly audioSettings = this.audioSettingsSignal;

  readonly currentUser$ = this.authService.getCurrentUser();
  private readonly currentUserSignal = toSignal(this.currentUser$, { initialValue: null });
  readonly isAuthenticated = computed(() => {
    const user = this.currentUserSignal();
    return !!user && !user.isGuest;
  });

  constructor() {
    effect(() => {
      this.currentUserSignal();
      this.cdr.markForCheck();
    });
  }

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/auth/login']).catch(() => {});
      },
      error: () => {
        this.router.navigate(['/auth/login']).catch(() => {});
      }
    });
  }

  openAudioSettings(): void {
    this.audioSettingsDialogVisible.set(true);
  }

  closeAudioSettings(): void {
    this.audioSettingsDialogVisible.set(false);
  }

  toggleMusic(): void {
    const current = this.audioSettingsSignal().musicEnabled;
    this.audioSettingsService.setMusicEnabled(!current);
  }

  toggleClickSound(): void {
    const current = this.audioSettingsSignal().clickSoundEnabled;
    this.audioSettingsService.setClickSoundEnabled(!current);
  }
}


