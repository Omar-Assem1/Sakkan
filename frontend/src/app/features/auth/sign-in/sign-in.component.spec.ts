import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { SignInComponent } from './sign-in.component';

describe('SignInComponent', () => {
  let fixture: ComponentFixture<SignInComponent>;
  let component: SignInComponent;
  let calls: Array<[string, string]>;
  let loginResult: () => unknown;
  let navigatedTo: string;

  beforeEach(async () => {
    calls = [];
    navigatedTo = '';
    loginResult = () => of({ token: 't', tokenType: 'Bearer' });

    await TestBed.configureTestingModule({
      imports: [SignInComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login: (u: string, p: string) => { calls.push([u, p]); return loginResult(); } } },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    (router as any).navigate = async (c: unknown[]) => { navigatedTo = c.join('/'); return true; };
    (router as any).navigateByUrl = async (u: unknown) => { navigatedTo = String(u); return true; };

    fixture = TestBed.createComponent(SignInComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('is invalid and does not call the API when fields are empty', () => {
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(calls.length).toBe(0);
  });

  it('submits username and password to AuthService and navigates away', () => {
    component.form.setValue({ username: 'omar', password: 'Passw0rd1' });

    component.submit();

    expect(calls).toEqual([['omar', 'Passw0rd1']]);
    expect(navigatedTo).not.toBe('');
  });

  it('shows one generic error on 401 and stays on the page', () => {
    loginResult = () => throwError(() => ({ status: 401 }));
    component.form.setValue({ username: 'omar', password: 'bad' });

    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="auth-error"]')).toBeTruthy();
    expect(navigatedTo).toBe('');
  });
});
