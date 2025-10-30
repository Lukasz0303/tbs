import { Injectable, inject } from '@angular/core';
import { Router, NavigationStart, NavigationEnd, NavigationCancel, NavigationError } from '@angular/router';
import { filter } from 'rxjs/operators';
import { LoaderService } from './loader.service';

@Injectable({ providedIn: 'root' })
export class RouterLoaderService {
  private readonly router = inject(Router);
  private readonly loader = inject(LoaderService);

  constructor() {
    this.router.events
      .pipe(
        filter(
          (e) =>
            e instanceof NavigationStart ||
            e instanceof NavigationEnd ||
            e instanceof NavigationCancel ||
            e instanceof NavigationError
        )
      )
      .subscribe((e) => {
        if (e instanceof NavigationStart) {
          this.loader.show();
          return;
        }
        this.loader.hide();
      });
  }
}


