import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { provideRouterStore } from '@ngrx/router-store';
import { counterReducer } from './features/counter/counter.reducer';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(),
    provideRouter(routes),
    provideStore({ counter: counterReducer }),
    provideEffects([]),
    provideRouterStore(),
    provideStoreDevtools({ maxAge: 25 })
  ]
};

