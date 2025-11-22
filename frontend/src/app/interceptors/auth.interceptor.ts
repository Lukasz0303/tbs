import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, first, switchMap, EMPTY } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const clonedReq = req.clone({
    withCredentials: true
  });

  return next(clonedReq).pipe(
    catchError((error) => {
      if (error.status === 403 || error.status === 401) {
        const isLogoutRequest = req.url.includes('/auth/logout');
        
        if (!isLogoutRequest) {
          authService.logout().pipe(
            first(),
            catchError(() => {
              authService.forceLogout();
              return EMPTY;
            })
          ).subscribe();
        } else {
          authService.forceLogout();
        }
      }
      return throwError(() => error);
    })
  );
};

