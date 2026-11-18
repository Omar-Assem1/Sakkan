import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-property-search-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, TranslocoPipe],
  templateUrl: './property-search-bar.component.html'
})
export class PropertySearchBarComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();
  private lastEmittedValue = '';

  @Input() set initialQuery(val: string | null | undefined) {
    const q = val || '';
    this.query.set(q);
    this.lastEmittedValue = q;
  }

  @Output() searchChange = new EventEmitter<string>();

  readonly query = signal<string>('');

  ngOnInit(): void {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((term) => {
        const trimmed = term.trim();
        if (trimmed !== this.lastEmittedValue) {
          this.lastEmittedValue = trimmed;
          this.searchChange.emit(trimmed);
        }
      });
  }

  onInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.query.set(val);
    this.searchSubject.next(val);
  }

  clear(): void {
    this.query.set('');
    this.lastEmittedValue = '';
    this.searchSubject.next('');
    this.searchChange.emit('');
  }
}
