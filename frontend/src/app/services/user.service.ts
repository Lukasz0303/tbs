import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';

export interface UpdateUserRequest {
  username?: string;
  avatar?: number;
}

export interface UpdateUserResponse {
  userId: number;
  username: string | null;
  isGuest: boolean;
  avatar: number | null;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

  updateUser(userId: number, data: UpdateUserRequest): Observable<UpdateUserResponse> {
    if (!userId || userId <= 0) {
      return throwError(() => new Error('Invalid userId: must be a positive number'));
    }

    if (data.username !== undefined) {
      const trimmedUsername = data.username.trim();
      if (!trimmedUsername || trimmedUsername.length === 0) {
        return throwError(() => new Error('Invalid username: cannot be empty'));
      }
      if (trimmedUsername.length < 3 || trimmedUsername.length > 50) {
        return throwError(() => new Error('Invalid username: must be between 3 and 50 characters'));
      }
    }

    if (data.avatar !== undefined && (data.avatar < 1 || data.avatar > 6)) {
      return throwError(() => new Error('Invalid avatar: must be between 1 and 6'));
    }

    return this.http
      .put<UpdateUserResponse>(`${this.apiUrl}/v1/users/${userId}`, data)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          return throwError(() => error);
        })
      );
  }

  updateAvatar(userId: number, avatar: number): Observable<UpdateUserResponse> {
    if (!userId || userId <= 0) {
      return throwError(() => new Error('Invalid userId: must be a positive number'));
    }

    if (avatar < 1 || avatar > 6) {
      return throwError(() => new Error('Invalid avatar: must be between 1 and 6'));
    }

    return this.http
      .put<UpdateUserResponse>(`${this.apiUrl}/v1/users/${userId}/avatar`, { avatar })
      .pipe(
        catchError((error: HttpErrorResponse) => {
          return throwError(() => error);
        })
      );
  }

  getUserProfile(userId: number): Observable<User> {
    if (!userId || userId <= 0) {
      return throwError(() => new Error('Invalid userId: must be a positive number'));
    }

    return this.http
      .get<User>(`${this.apiUrl}/v1/users/${userId}`)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          return throwError(() => error);
        })
      );
  }
}

