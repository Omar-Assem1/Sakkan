import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { describe, expect, it } from 'vitest';
import { ErrorPageComponent } from './error-page.component';

describe('ErrorPageComponent', () => {
  let fixture: ComponentFixture<ErrorPageComponent>;
  let component: ErrorPageComponent;

  const createComponent = (queryParams = {}, data = {}) => {
    TestBed.configureTestingModule({
      imports: [
        ErrorPageComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              ERROR_PAGE: {
                DEFAULT_TITLE: 'Something went wrong',
                DEFAULT_MESSAGE: 'An unexpected error occurred.',
                CODE_LABEL: 'Error Code',
                DETAILS_LABEL: 'Error Details',
                BACK_HOME: 'Back to Listings',
                TRY_AGAIN: 'Try Again',
                NOT_FOUND_TITLE: 'Page Not Found',
                NOT_FOUND_MESSAGE: 'The page you are looking for does not exist.',
                FORBIDDEN_TITLE: 'Access Denied',
                FORBIDDEN_MESSAGE: 'You do not have permission.',
                SERVER_ERROR_TITLE: 'Internal Server Error',
                SERVER_ERROR_MESSAGE: 'The server encountered an error.',
              },
            },
          },
          translocoConfig: {
            availableLangs: ['en'],
            defaultLang: 'en',
          },
        }),
      ],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams,
              data,
            },
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ErrorPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it('renders default 404 error page', () => {
    createComponent();
    expect(component.errorCode).toBe(404);
    expect(component.displayTitle).toBe('Page Not Found');
    expect(component.displayMessage).toBe('The page you are looking for does not exist.');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="error-code"]')?.textContent).toContain('404');
    expect(compiled.querySelector('[data-testid="error-title"]')?.textContent).toContain('Page Not Found');
    expect(compiled.querySelector('[data-testid="back-home-button"]')).toBeTruthy();
  });

  it('displays custom error code, title, and technical details from query params', () => {
    createComponent({
      code: '500',
      title: 'Database Unavailable',
      message: 'Failed to connect to primary database replica.',
      detail: 'Connection pool exhausted on port 5432',
      path: '/api/properties',
      timestamp: '2026-11-18T16:00:00Z',
    });

    expect(component.errorCode).toBe(500);
    expect(component.displayTitle).toBe('Database Unavailable');
    expect(component.displayMessage).toBe('Failed to connect to primary database replica.');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="error-code"]')?.textContent).toContain('500');
    expect(compiled.querySelector('[data-testid="error-title"]')?.textContent).toContain('Database Unavailable');
    expect(compiled.querySelector('[data-testid="error-details-box"]')?.textContent).toContain(
      'Connection pool exhausted on port 5432'
    );
    expect(compiled.querySelector('[data-testid="error-details-box"]')?.textContent).toContain(
      '/api/properties'
    );
  });

  it('handles 403 Forbidden code defaults', () => {
    createComponent({ code: '403' });

    expect(component.errorCode).toBe(403);
    expect(component.displayTitle).toBe('Access Denied');
    expect(component.displayMessage).toBe('You do not have permission.');
  });
});
