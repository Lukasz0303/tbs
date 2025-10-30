import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="text-center py-16">
      <h2 class="text-4xl font-bold text-slate-900 dark:text-white">404</h2>
      <p class="mt-2 text-slate-600 dark:text-slate-300">Strona nie została znaleziona.</p>
      <a routerLink="/" class="inline-block mt-6 px-4 py-2 rounded bg-slate-900 text-white hover:bg-slate-800">Wróć na stronę główną</a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundComponent {}


