import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

const ROLE_LABELS: Record<string, string> = {
  BUYER_TENANT: 'Buyer / Tenant',
  OWNER: 'Property Owner',
  AGENT: 'Real Estate Agent',
};

// TODO i18n: replace the English labels with Transloco keys (EN/AR) once the test provider is confirmed.
@Component({
  selector: 'app-sign-up',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section
      class="mx-auto my-6 max-w-md rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start shadow-2xs transition-colors dark:border-slate-800 dark:bg-slate-900"
    >
      <h1 class="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
        Create account
      </h1>
      <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
        Join Sakan to browse homes, list properties, and schedule viewings
      </p>

      <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">Username</span>
          <input
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              fieldInvalid('username')
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            [attr.aria-invalid]="fieldInvalid('username')"
            formControlName="username"
            autocomplete="username"
          />
          @if (fieldInvalid('username')) {
            <span class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
              Use 3–50 letters, digits, dots, dashes, or underscores.
            </span>
          }
        </label>
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">Email</span>
          <input
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              fieldInvalid('email')
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            [attr.aria-invalid]="fieldInvalid('email')"
            type="email"
            formControlName="email"
            autocomplete="email"
          />
          @if (fieldInvalid('email')) {
            <span class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
              Enter a valid email address.
            </span>
          }
        </label>
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">
            Password (8+ characters, a letter and a digit)
          </span>
          <input
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              fieldInvalid('password')
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            [attr.aria-invalid]="fieldInvalid('password')"
            type="password"
            formControlName="password"
            autocomplete="new-password"
          />
          @if (fieldInvalid('password')) {
            <span class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
              Password must be 8–72 characters and include a letter and a digit.
            </span>
          }
        </label>
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">
            Phone (+201012345678)
          </span>
          <input
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              fieldInvalid('phone')
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            [attr.aria-invalid]="fieldInvalid('phone')"
            type="tel"
            formControlName="phone"
            autocomplete="tel"
          />
          @if (fieldInvalid('phone')) {
            <span class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
              Enter a phone number in international format, such as +201012345678.
            </span>
          }
        </label>
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">I am a</span>
          <select
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              fieldInvalid('role')
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            formControlName="role"
          >
            @for (r of roles; track r) {
              <option [value]="r">{{ roleLabel(r) }}</option>
            }
          </select>
        </label>

        @if (error()) {
          <p
            class="rounded-xl bg-rose-50 p-3 text-sm font-medium text-red-600 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-300 dark:ring-rose-800/80"
            data-testid="auth-error"
            role="alert"
          >
            {{ error() }}
          </p>
        }

        <button
          class="w-full cursor-pointer rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-emerald-500"
          type="submit"
          [disabled]="submitting()"
        >
          Sign up
        </button>
      </form>

      <p
        class="mt-6 border-t border-slate-100 pt-5 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400"
      >
        Already registered?
        <a
          class="font-semibold text-emerald-700 underline hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300"
          routerLink="/auth/sign-in"
        >
          Sign in
        </a>
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

  roleLabel(role: string): string {
    return ROLE_LABELS[role] ?? role;
  }

  fieldInvalid(name: 'username' | 'email' | 'password' | 'phone' | 'role'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  readonly form = new FormGroup({
    username: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[A-Za-z0-9_.-]+$/),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72),
        Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
      ],
    }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^\+[1-9]\d{1,14}$/)],
    }),
    role: new FormControl('BUYER_TENANT', {
      nonNullable: true,
      validators: [Validators.required],
    }),
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
          err?.status === 409
            ? 'This username or email is already registered.'
            : 'Could not create the account.',
        );
      },
    });
  }
}
