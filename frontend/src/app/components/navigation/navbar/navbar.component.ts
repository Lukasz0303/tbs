import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  template: `
    <header class="sticky top-0 z-50 w-full border-b border-white/20 bg-slate-900/40 backdrop-blur supports-[backdrop-filter]:backdrop-blur">
      <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
        <a [routerLink]="[{ outlets: { primary: [''], modal: null } }]" class="font-semibold tracking-wide text-white hover:text-white/90">
          World at War
        </a>
        <nav class="flex items-center gap-4 text-sm">
          <a [routerLink]="[{ outlets: { primary: [''], modal: null } }]" class="text-white/90 hover:text-white">Menu</a>
          <a [routerLink]="[{ outlets: { modal: ['polityka-prywatnosci'] } }]" class="text-white/80 hover:text-white">Polityka</a>
          <a [routerLink]="[{ outlets: { modal: ['regulamin'] } }]" class="text-white/80 hover:text-white">Regulamin</a>
        </nav>
      </div>
    </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
}


