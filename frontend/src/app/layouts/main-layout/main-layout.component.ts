import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../components/navigation/navbar/navbar.component';
import { LoaderComponent } from '../../components/ui/loader/loader.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, LoaderComponent],
  template: `
    <app-loader />
    <app-navbar />
    <main class="mx-auto max-w-5xl px-4 py-6">
      <router-outlet />
    </main>
    <footer class="border-t border-slate-200 dark:border-slate-800 py-6 text-center text-sm text-slate-600 dark:text-slate-400">
      © {{ currentYear }} World at War
    </footer>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  // expose year for template to avoid using 'new' in the template expression
  readonly currentYear = new Date().getFullYear();
}


