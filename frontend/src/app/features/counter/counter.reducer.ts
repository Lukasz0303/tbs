import { createReducer, on } from '@ngrx/store';
import { decrement, increment, reset } from './counter.actions';

export interface CounterState {
  value: number;
}

export const initialState: CounterState = {
  value: 0,
};

export const counterReducer = createReducer(
  initialState,
  on(increment, (state): CounterState => ({ value: state.value + 1 })),
  on(decrement, (state): CounterState => ({ value: state.value - 1 })),
  on(reset, (): CounterState => ({ value: 0 }))
);


