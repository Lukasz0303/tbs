import { inject, Injectable, computed, effect, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, catchError, map, of, tap, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';
import { GuestSessionResponse } from '../models/api.model';

interface AuthUserResponse {
  userId: number;
  username: string | null;
  email: string | null;
  isGuest: boolean;
  avatar: number | null;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
  lastSeenAt: string | null;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  avatar?: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;
  private readonly currentUserSignal = signal<User | null>(null);
  private readonly isGuestSignal = computed(
    () => this.currentUserSignal()?.isGuest ?? true
  );

  readonly currentUser$ = toObservable(this.currentUserSignal);
  readonly isGuest$ = toObservable(this.isGuestSignal);

  constructor() {
    effect(() => {
      const user = this.currentUserSignal();
      if (!user) {
        localStorage.removeItem('wow-current-user');
        return;
      }
      localStorage.setItem('wow-current-user', JSON.stringify(user));
    });

    const cachedUser = localStorage.getItem('wow-current-user');
    if (cachedUser) {
      try {
        const parsed = JSON.parse(cachedUser) as User;
        this.currentUserSignal.set(parsed);
      } catch {
        localStorage.removeItem('wow-current-user');
      }
    }
  }

  loadCurrentUser(): Observable<User | null> {
    return this.http
      .get<AuthUserResponse>(`${this.apiUrl}/v1/auth/me`)
      .pipe(
        map((response) => this.mapToUser(response)),
        tap((user) => this.currentUserSignal.set(user)),
        catchError((error) => {
          if (error.status === 401 || error.status === 403) {
            this.forceLogout();
          }
          return of(null);
        })
      );
  }

  getCurrentUser(): Observable<User | null> {
    return this.currentUser$;
  }

  isGuest(): Observable<boolean> {
    return this.isGuest$;
  }

  createGuestSession(): Observable<GuestSessionResponse> {
    return this.http
      .post<GuestSessionResponse>(`${this.apiUrl}/v1/guests`, {})
      .pipe(
        tap((response) => {
          const guestUser: User = {
            userId: response.userId,
            username: null,
            email: null,
            isGuest: response.isGuest,
            avatar: response.avatar ?? 1,
            totalPoints: response.totalPoints,
            gamesPlayed: response.gamesPlayed,
            gamesWon: response.gamesWon,
            createdAt: response.createdAt,
            lastSeenAt: null,
          };
          this.currentUserSignal.set(guestUser);
        })
      );
  }

  updateCurrentUser(user: User | null): void {
    this.currentUserSignal.set(user);
  }

  isAuthenticated(): boolean {
    return !!this.currentUserSignal() && !this.currentUserSignal()?.isGuest;
  }

  login(email: string, password: string): Observable<User> {
    const request: LoginRequest = { email, password };
    return this.http.post<AuthUserResponse>(`${this.apiUrl}/v1/auth/login`, request).pipe(
      tap((response) => {
        const user = this.mapToUser(response);
        if (user) {
          this.currentUserSignal.set(user);
        }
      }),
      switchMap(() => this.loadCurrentUser()),
      map((user) => {
        if (!user) {
          throw new Error('Failed to load user after login');
        }
        return user;
      })
    );
  }

  register(username: string, email: string, password: string, avatar?: number): Observable<User> {
    const request: RegisterRequest = { username, email, password, avatar };
    return this.http.post<AuthUserResponse>(`${this.apiUrl}/v1/auth/register`, request).pipe(
      tap((response) => {
        const user = this.mapToUser(response);
        if (user) {
          this.currentUserSignal.set(user);
        }
      }),
      switchMap(() => this.loadCurrentUser()),
      map((user) => {
        if (!user) {
          throw new Error('Failed to load user after register');
        }
        return user;
      })
    );
  }

  logout(): Observable<void> {
    const currentUser = this.currentUserSignal();
    const isGuest = currentUser?.isGuest ?? false;
    
    if (isGuest) {
      this.currentUserSignal.set(null);
      return of(undefined);
    }

    return this.http.post<void>(`${this.apiUrl}/v1/auth/logout`, {}).pipe(
      tap(() => {
        this.currentUserSignal.set(null);
      }),
      catchError((error) => {
        this.currentUserSignal.set(null);
        return of(undefined);
      }),
      map(() => undefined)
    );
  }

  forceLogout(): void {
    this.currentUserSignal.set(null);
  }

  private mapToUser(response: AuthUserResponse | null): User | null {
    if (!response) {
      return null;
    }
    return {
      userId: response.userId,
      username: response.username,
      email: response.email,
      isGuest: response.isGuest,
      avatar: response.avatar ?? 1,
      totalPoints: response.totalPoints,
      gamesPlayed: response.gamesPlayed,
      gamesWon: response.gamesWon,
      createdAt: response.createdAt,
      lastSeenAt: response.lastSeenAt,
    };
  }
}

