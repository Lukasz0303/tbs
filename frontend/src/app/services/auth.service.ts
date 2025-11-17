import { inject, Injectable, computed, effect, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';
import { GuestSessionResponse } from '../models/api.model';

interface AuthUserResponse {
  userId: number;
  username: string | null;
  email: string | null;
  isGuest: boolean;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
  lastSeenAt: string | null;
  authToken?: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
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
            this.clearAuthToken();
          }
          this.currentUserSignal.set(null);
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
          if (response.authToken) {
            localStorage.setItem('wow-auth-token', response.authToken);
          }
          const guestUser: User = {
            userId: response.userId,
            username: null,
            email: null,
            isGuest: response.isGuest,
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

  getAuthToken(): string | null {
    return localStorage.getItem('wow-auth-token');
  }

  clearAuthToken(): void {
    localStorage.removeItem('wow-auth-token');
  }

  updateCurrentUser(user: User | null): void {
    this.currentUserSignal.set(user);
  }

  isAuthenticated(): boolean {
    return !!this.getAuthToken() && !!this.currentUserSignal() && !this.currentUserSignal()?.isGuest;
  }

  login(email: string, password: string): Observable<User> {
    const request: LoginRequest = { email, password };
    return this.http.post<AuthUserResponse>(`${this.apiUrl}/v1/auth/login`, request).pipe(
      tap((response) => {
        if (response.authToken) {
          localStorage.setItem('wow-auth-token', response.authToken);
        }
        const user = this.mapToUser(response);
        if (user) {
          this.currentUserSignal.set(user);
        }
      }),
      map((response) => {
        const user = this.mapToUser(response);
        if (!user) {
          throw new Error('Failed to map user from login response');
        }
        return user;
      })
    );
  }

  register(username: string, email: string, password: string): Observable<User> {
    const request: RegisterRequest = { username, email, password };
    return this.http.post<AuthUserResponse>(`${this.apiUrl}/v1/auth/register`, request).pipe(
      tap((response) => {
        if (response.authToken) {
          localStorage.setItem('wow-auth-token', response.authToken);
        }
        const user = this.mapToUser(response);
        if (user) {
          this.currentUserSignal.set(user);
        }
      }),
      map((response) => {
        const user = this.mapToUser(response);
        if (!user) {
          throw new Error('Failed to map user from register response');
        }
        return user;
      })
    );
  }

  logout(): void {
    this.currentUserSignal.set(null);
    this.clearAuthToken();
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
      totalPoints: response.totalPoints,
      gamesPlayed: response.gamesPlayed,
      gamesWon: response.gamesWon,
      createdAt: response.createdAt,
      lastSeenAt: response.lastSeenAt,
    };
  }
}

