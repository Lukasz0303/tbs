import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../components/navigation/navbar/navbar.component';
import { LoaderComponent } from '../../components/ui/loader/loader.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, NavbarComponent, LoaderComponent],
  template: `
    <app-loader />
    <app-navbar />
    <main class="mx-auto max-w-5xl px-4 py-6">
      <router-outlet />
    </main>
    <footer class="mt-8">
      <div class="w-full border-t border-white/20 bg-slate-900/40 text-white/90 backdrop-blur supports-[backdrop-filter]:backdrop-blur">
        <div class="mx-auto max-w-7xl px-4 py-5">
          <div class="flex flex-col items-center justify-center gap-2 text-center text-sm">
            <div class="order-2">
              © {{ currentYear }} World at War
            </div>
            <div class="order-1 flex items-center gap-4">
              <a class="transition-colors hover:text-white" routerLink="/polityka-prywatnosci">Polityka prywatności</a>
              <span class="h-3 w-px bg-white/30"></span>
              <a class="transition-colors hover:text-white" routerLink="/regulamin">Regulamin</a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  // expose year for template to avoid using 'new' in the template expression
  readonly currentYear = new Date().getFullYear();
}


