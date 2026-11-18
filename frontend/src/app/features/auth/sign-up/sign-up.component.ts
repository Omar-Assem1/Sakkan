import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// TODO i18n: replace the English labels with Transloco keys (EN/AR) once the test provider is confirmed.
@Component({
  selector: 'app-sign-up',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-md rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">Create account</h1>

      <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="block">
          <span class="text-sm text-slate-700">Username</span>
          <input class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2"
                 formControlName="username" autocomplete="username" />
        </label>
        <label class="block">
          <span class="text-sm text-slate-700">Email</span>
          <input class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2"
                 type="email" formControlName="email" autocomplete="email" />
        </label>
        <label class="block">
          <span class="text-sm text-slate-700">Password (8+ characters, a letter and a digit)</span>
          <input class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2"
                 type="password" formControlName="password" autocomplete="new-password" />
        </label>
        <label class="block">
          <span class="text-sm text-slate-700">Phone (+201012345678)</span>
          <input class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2"
                 type="tel" formControlName="phone" autocomplete="tel" />
        </label>
        <label class="block">
          <span class="text-sm text-slate-700">I am a</span>
          <select class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2" formControlName="role">
            @for (r of roles; track r) {
              <option [value]="r">{{ r }}</option>
            }
          </select>
        </label>

        @if (error()) {
          <p class="text-sm text-red-600" data-testid="auth-error" role="alert">{{ error() }}</p>
        }

        <button class="w-full rounded bg-slate-900 py-2 text-white disabled:opacity-50"
                type="submit" [disabled]="submitting()">Sign up</button>
      </form>

      <p class="mt-4 text-sm text-slate-600">
        Already registered? <a class="underline" routerLink="/auth/sign-in">Sign in</a>
      </p>
    </section>
  `,
})
export class SignUpComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** ADMIN and MAINTENANCE_STAFF are never offered. The server rejects them too. */
  readonly roles: string[] = ['BUYER_TENANT', 'OWNER', 'AGENT'];

  readonly error = signal('');
  readonly submitting = signal(false);

  readonly form = new FormGroup({
    username: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(50),
        Validators.pattern(/^[A-Za-z0-9_.-]+$/)],
    }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72),
        Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)],
    }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^\+[1-9]\d{1,14}$/)],
    }),
    role: new FormControl('BUYER_TENANT', { nonNullable: true, validators: [Validators.required] }),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set('');
    this.submitting.set(true);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigateByUrl('/auth/sign-in');
      },
      error: (err: { status?: number }) => {
        this.submitting.set(false);
        this.error.set(
          err?.status === 409 ? 'This username or email is already registered.' : 'Could not create the account.',
        );
      },
    });
  }
}
