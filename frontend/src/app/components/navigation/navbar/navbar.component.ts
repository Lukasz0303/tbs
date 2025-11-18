import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, AsyncPipe],
  template: `
    <header class="sticky top-0 z-50 w-full border-b border-white/20 bg-slate-900/40 backdrop-blur supports-[backdrop-filter]:backdrop-blur">
      <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
        <a routerLink="/" class="font-semibold tracking-wide text-white hover:text-white/90">
          World at War
        </a>
        <nav class="flex items-center gap-4 text-sm">
          <a routerLink="/" class="text-white/90 hover:text-white">Menu</a>
          <a routerLink="/leaderboard" class="text-white/80 hover:text-white">Ranking</a>
          <a routerLink="/polityka-prywatnosci" class="text-white/80 hover:text-white">Polityka</a>
          <a routerLink="/regulamin" class="text-white/80 hover:text-white">Regulamin</a>
          @if (currentUser$ | async; as user) {
            <div class="flex items-center gap-2 ml-4 pl-4 border-l border-white/20">
              <span class="text-white/90">{{ user.isGuest ? 'Gość' : (user.username || user.email) }}</span>
              <button
                (click)="onLogout()"
                class="flex items-center justify-center w-8 h-8 rounded hover:bg-white/10 transition-colors text-white/80 hover:text-white"
                title="Wyloguj"
                aria-label="Wyloguj"
              >
                <i class="pi pi-sign-out text-sm"></i>
              </button>
            </div>
          }
        </nav>
      </div>
    </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser$ = this.authService.getCurrentUser();

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']).catch(() => {});
  }
}


