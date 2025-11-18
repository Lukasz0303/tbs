import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { GameDashboardComponent } from './features/game/game-dashboard.component';
import { LeaderboardComponent } from './features/leaderboard/leaderboard.component';
import { NotFoundComponent } from './features/not-found/not-found.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', component: HomeComponent },
      {
        path: 'auth/login',
        loadComponent: () =>
          import('./features/auth/auth-login.component').then(
            (m) => m.AuthLoginComponent,
          ),
      },
      {
        path: 'auth/register',
        loadComponent: () =>
          import('./features/auth/auth-register.component').then(
            (m) => m.AuthRegisterComponent,
          ),
      },
      {
        path: 'game-options',
        loadComponent: () =>
          import('./features/game-options/game-options.component').then(
            (m) => m.GameOptionsComponent,
          ),
      },
      { path: 'game', component: GameDashboardComponent },
      {
        path: 'game/:gameId',
        loadComponent: () =>
          import('./features/game/game.component').then(
            (m) => m.GameComponent,
          ),
      },
      { path: 'leaderboard', component: LeaderboardComponent },
      {
        path: 'polityka-prywatnosci',
        loadComponent: () =>
          import('./features/legal/privacy-policy.component').then(
            (m) => m.PrivacyPolicyComponent,
          ),
      },
      {
        path: 'regulamin',
        loadComponent: () =>
          import('./features/legal/terms-of-service.component').then(
            (m) => m.TermsOfServiceComponent,
          ),
      },
      { path: '**', component: NotFoundComponent },
    ],
  },
];
