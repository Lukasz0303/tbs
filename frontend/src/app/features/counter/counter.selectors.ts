import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CounterState } from './counter.reducer';

export const selectCounterFeature = createFeatureSelector<CounterState>('counter');

export const selectCounterValue = createSelector(
  selectCounterFeature,
  (state) => state.value
);


