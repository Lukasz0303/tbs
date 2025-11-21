import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAuthToken();

  let clonedReq = req.clone({
    withCredentials: true
  });

  if (token) {
    clonedReq = clonedReq.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(clonedReq).pipe(
    catchError((error) => {
      if (error.status === 403 || error.status === 401) {
        authService.clearAuthToken();
        authService.updateCurrentUser(null);
      }
      return throwError(() => error);
    })
  );
};

