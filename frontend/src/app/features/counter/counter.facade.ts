import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { decrement, increment, reset } from './counter.actions';
import { selectCounterValue } from './counter.selectors';

@Injectable({ providedIn: 'root' })
export class CounterFacade {
  private readonly store = inject(Store);

  readonly value$ = this.store.select(selectCounterValue);

  increment(): void {
    this.store.dispatch(increment());
  }

  decrement(): void {
    this.store.dispatch(decrement());
  }

  reset(): void {
    this.store.dispatch(reset());
  }
}


