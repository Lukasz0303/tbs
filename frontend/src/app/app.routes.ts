import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { AuthComponent } from './features/auth/auth.component';
import { GameDashboardComponent } from './features/game/game-dashboard.component';
import { LeaderboardComponent } from './features/leaderboard/leaderboard.component';
import { NotFoundComponent } from './features/not-found/not-found.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', component: HomeComponent },
      { path: 'auth', component: AuthComponent },
      { path: 'game', component: GameDashboardComponent },
      { path: 'leaderboard', component: LeaderboardComponent },
      { path: '**', component: NotFoundComponent },
    ],
  },
];
