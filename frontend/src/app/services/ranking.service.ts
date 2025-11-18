import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Ranking } from '../models/ranking.model';
import { PaginatedResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class RankingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

  getRanking(page: number = 0, size: number = 50): Observable<PaginatedResponse<Ranking>> {
    if (page < 0) {
      return throwError(() => new Error('Invalid page: must be non-negative'));
    }

    if (size <= 0 || size > 100) {
      return throwError(() => new Error('Invalid size: must be between 1 and 100'));
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<Ranking>>(`${this.apiUrl}/rankings`, { params })
      .pipe(
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }

  getUserRanking(userId: number): Observable<Ranking> {
    if (!userId || userId <= 0) {
      return throwError(() => new Error('Invalid userId: must be a positive number'));
    }

    return this.http
      .get<Ranking>(`${this.apiUrl}/rankings/${userId}`)
      .pipe(
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }

  getPlayersAround(userId: number, range: number = 5): Observable<Ranking[]> {
    if (!userId || userId <= 0) {
      return throwError(() => new Error('Invalid userId: must be a positive number'));
    }

    if (range <= 0 || range > 20) {
      return throwError(() => new Error('Invalid range: must be between 1 and 20'));
    }

    const params = new HttpParams().set('range', range.toString());

    return this.http
      .get<{ items: Ranking[] }>(`${this.apiUrl}/rankings/around/${userId}`, { params })
      .pipe(
        map((response) => response.items || []),
        catchError((error) => {
          return throwError(() => error);
        })
      );
  }
}

