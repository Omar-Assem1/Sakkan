import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { AdminSignInComponent } from './admin-sign-in.component';

describe('AdminSignInComponent', () => {
  let fixture: ComponentFixture<AdminSignInComponent>;
  let component: AdminSignInComponent;
  let calls: Array<[string, string]>;
  let adminLoginResult: () => unknown;
  let navigatedTo: string;

  beforeEach(async () => {
    calls = [];
    navigatedTo = '';
    adminLoginResult = () => of({ token: 't', tokenType: 'Bearer' });

    await TestBed.configureTestingModule({
      imports: [AdminSignInComponent],
      providers: [
        provideRouter([]),
        // Only adminLogin is provided on purpose: calling the normal login() would throw and fail the test.
        {
          provide: AuthService,
          useValue: {
            adminLogin: (u: string, p: string) => {
              calls.push([u, p]);
              return adminLoginResult();
            },
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(async (c) => {
      navigatedTo = c.join('/');
      return true;
    });
    vi.spyOn(router, 'navigateByUrl').mockImplementation(async (u) => {
      navigatedTo = String(u);
      return true;
    });

    fixture = TestBed.createComponent(AdminSignInComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('T22 empty form is invalid and does not call the API', () => {
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(calls.length).toBe(0);
  });

  it('T22b invalid submit does not navigate', () => {
    component.submit();

    expect(navigatedTo).toBe('');
  });

  it('T23 submits username and password through adminLogin and navigates to /admin', () => {
    component.form.setValue({ username: 'admin', password: 'Adm1nPassw0rd' });

    component.submit();

    expect(calls).toEqual([['admin', 'Adm1nPassw0rd']]);
    expect(navigatedTo).toBe('/admin');
  });

  it('T24 shows one generic error on 401, stays on the page and re-enables the button', () => {
    adminLoginResult = () => throwError(() => ({ status: 401 }));
    component.form.setValue({ username: 'admin', password: 'bad' });

    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="auth-error"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="auth-rate-limit"]')).toBeFalsy();
    expect(navigatedTo).toBe('');
    expect(component.submitting()).toBe(false);
  });

  it('T24b shows the try-again-later message on 429, not the generic error', () => {
    adminLoginResult = () => throwError(() => ({ status: 429 }));
    component.form.setValue({ username: 'admin', password: 'bad' });

    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="auth-rate-limit"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="auth-error"]')).toBeFalsy();
    expect(navigatedTo).toBe('');
  });

  it('T24c shows the generic error when the server is unreachable (status 0)', () => {
    adminLoginResult = () => throwError(() => ({ status: 0 }));
    component.form.setValue({ username: 'admin', password: 'x' });

    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="auth-error"]')).toBeTruthy();
    expect(navigatedTo).toBe('');
  });

  it('T24d a new attempt clears the previous error', () => {
    adminLoginResult = () => throwError(() => ({ status: 401 }));
    component.form.setValue({ username: 'admin', password: 'bad' });
    component.submit();
    fixture.detectChanges();

    adminLoginResult = () => of({ token: 't', tokenType: 'Bearer' });
    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="auth-error"]')).toBeFalsy();
    expect(navigatedTo).toBe('/admin');
  });
  it('S-F1 the password field is masked and uses the right autocomplete hint', () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector(
      'input[formControlName="password"]',
    );

    expect(input.type).toBe('password');
    expect(input.getAttribute('autocomplete')).toBe('current-password');
  });

  it('S-F2 the typed password never appears in the page text, even after a failed attempt', () => {
    adminLoginResult = () => throwError(() => ({ status: 401 }));
    component.form.setValue({ username: 'admin', password: 'Sup3rSecretTyped' });

    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Sup3rSecretTyped');
  });
});
