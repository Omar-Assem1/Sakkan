import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { SignUpComponent } from './sign-up.component';

describe('SignUpComponent', () => {
  let fixture: ComponentFixture<SignUpComponent>;
  let component: SignUpComponent;
  let payloads: unknown[];
  let registerResult: () => unknown;
  let navigatedTo: string;

  const valid = {
    username: 'omar', email: 'omar@sakan.com', password: 'Passw0rd1',
    phone: '+201012345678', role: 'OWNER',
  };

  beforeEach(async () => {
    payloads = [];
    navigatedTo = '';
    registerResult = () => of({ id: 1 });

    await TestBed.configureTestingModule({
      imports: [SignUpComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { register: (p: unknown) => { payloads.push(p); return registerResult(); } } },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(async (commands) => { navigatedTo = commands.join('/'); return true; });
    vi.spyOn(router, 'navigateByUrl').mockImplementation(async (url) => { navigatedTo = String(url); return true; });

    fixture = TestBed.createComponent(SignUpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('offers only BUYER_TENANT, OWNER and AGENT (never ADMIN)', () => {
    expect(component.roles).toEqual(['BUYER_TENANT', 'OWNER', 'AGENT']);
  });

  it('rejects a weak password', () => {
    component.form.patchValue({ ...valid, password: 'password' });

    expect(component.form.controls.password.invalid).toBe(true);
  });

  it('rejects a phone that is not E.164', () => {
    component.form.patchValue({ ...valid, phone: '01012345678' });

    expect(component.form.controls.phone.invalid).toBe(true);
  });

  it('submits a valid form and then sends the user to sign in', () => {
    component.form.setValue(valid);

    component.submit();

    expect(payloads).toEqual([valid]);
    expect(navigatedTo).toContain('sign-in');
  });

  it('shows an error on 409 and does not navigate', () => {
    registerResult = () => throwError(() => ({ status: 409 }));
    component.form.setValue(valid);

    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="auth-error"]')).toBeTruthy();
    expect(navigatedTo).toBe('');
  });
});
