import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { PropertySearchBarComponent } from './property-search-bar.component';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('PropertySearchBarComponent', () => {
  let component: PropertySearchBarComponent;
  let fixture: ComponentFixture<PropertySearchBarComponent>;

  beforeEach(async () => {
    vi.useFakeTimers();

    await TestBed.configureTestingModule({
      imports: [
        PropertySearchBarComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              LISTINGS: {
                SEARCH: {
                  LABEL: 'Search properties',
                  PLACEHOLDER: 'Search by title or description...',
                  CLEAR: 'Clear search'
                }
              }
            },
            ar: {}
          },
          translocoConfig: {
            availableLangs: ['en', 'ar'],
            defaultLang: 'en'
          }
        })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PropertySearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should create and render with empty initial query', () => {
    expect(component).toBeTruthy();
    expect(component.query()).toBe('');
    const input = fixture.nativeElement.querySelector('input');
    expect(input.value).toBe('');
  });

  it('should populate input when initialQuery is provided', () => {
    component.initialQuery = 'Smouha';
    fixture.detectChanges();
    expect(component.query()).toBe('Smouha');
  });

  it('should debounce input emission by 300ms', () => {
    const emittedValues: string[] = [];
    component.searchChange.subscribe((val) => emittedValues.push(val));

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'A';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(100);

    input.value = 'Alex';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(100);

    input.value = 'Alexandria';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(299);
    expect(emittedValues.length).toBe(0);

    vi.advanceTimersByTime(1);
    expect(emittedValues).toEqual(['Alexandria']);
  });

  it('should show clear button when text is present and clear on click', () => {
    const emittedValues: string[] = [];
    component.searchChange.subscribe((val) => emittedValues.push(val));

    component.query.set('Villa');
    fixture.detectChanges();

    const clearBtn = fixture.nativeElement.querySelector('button');
    expect(clearBtn).toBeTruthy();

    clearBtn.click();
    fixture.detectChanges();

    expect(component.query()).toBe('');
    expect(emittedValues).toContain('');
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });
});
