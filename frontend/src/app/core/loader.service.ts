import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoaderService {
  private readonly isLoadingSubject = new BehaviorSubject<boolean>(false);
  readonly isLoading$ = this.isLoadingSubject.asObservable();

  show(): void {
    this.isLoadingSubject.next(true);
  }

  hide(): void {
    this.isLoadingSubject.next(false);
  }
}


