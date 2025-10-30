import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ThemeService } from '../../../core/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  template: `
  <header class="sticky top-0 z-10 border-b border-slate-200 bg-white/80 dark:bg-slate-900/80 backdrop-blur">
    <div class="mx-auto max-w-5xl px-4 h-14 flex items-center justify-between">
      <a routerLink="/" class="font-semibold text-slate-900 dark:text-white">World at War</a>
      <nav class="flex items-center gap-4 text-slate-700 dark:text-slate-200">
        <a routerLink="/game" routerLinkActive="font-semibold" class="hover:underline">Gra</a>
        <a routerLink="/leaderboard" routerLinkActive="font-semibold" class="hover:underline">Ranking</a>
        <a routerLink="/auth" routerLinkActive="font-semibold" class="hover:underline">Logowanie</a>
        <button class="ml-3 px-2 py-1 rounded bg-slate-200 dark:bg-slate-800" (click)="theme.toggle()">🌓</button>
      </nav>
    </div>
  </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  readonly theme = inject(ThemeService);
}


