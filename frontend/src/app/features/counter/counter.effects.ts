import { inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { tap } from 'rxjs/operators';
import { increment, decrement, reset } from './counter.actions';

export const logCounterChanges = createEffect(
  (actions$ = inject(Actions)) =>
    actions$.pipe(
      ofType(increment, decrement, reset),
      tap(() => {
        // side-effect placeholder
      })
    ),
  { functional: true, dispatch: false }
);


