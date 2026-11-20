import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// TODO i18n: replace the English labels with Transloco keys (EN/AR) once the test provider is confirmed.
@Component({
  selector: 'app-sign-in',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section
      class="mx-auto my-6 max-w-md rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start shadow-2xs transition-colors dark:border-slate-800 dark:bg-slate-900"
    >
      <div class="mb-6 flex items-center gap-3">
        <span
          aria-hidden="true"
          class="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-600 text-base font-black text-white shadow-2xs"
        >
          S
        </span>
        <div>
          <h1 class="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
            Sign in
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400">
            Welcome back to Sakan
          </p>
        </div>
      </div>

      <form class="space-y-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">Username</span>
          <input
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              usernameInvalid()
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            [attr.aria-invalid]="usernameInvalid()"
            aria-describedby="username-error"
            formControlName="username"
            autocomplete="username"
          />
          @if (usernameInvalid()) {
            <span id="username-error" class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
              Enter your username.
            </span>
          }
        </label>
        <label class="block">
          <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">Password</span>
          <input
            class="mt-1 w-full rounded-xl border bg-slate-50/50 ps-3.5 pe-3.5 py-2.5 text-sm text-slate-900 transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-500/20 focus:outline-hidden dark:bg-slate-800 dark:text-slate-100 dark:focus:bg-slate-800"
            [class]="
              passwordInvalid()
                ? 'border-red-600 dark:border-rose-500'
                : 'border-slate-300 dark:border-slate-700'
            "
            [attr.aria-invalid]="passwordInvalid()"
            aria-describedby="password-error"
            type="password"
            formControlName="password"
            autocomplete="current-password"
          />
          @if (passwordInvalid()) {
            <span id="password-error" class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
              Enter your password.
            </span>
          }
        </label>

        @if (error()) {
          <p
            class="rounded-xl bg-rose-50 p-3 text-sm font-medium text-red-600 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-300 dark:ring-rose-800/80"
            data-testid="auth-error"
            role="alert"
          >
            Invalid username or password.
          </p>
        }
        @if (googleError()) {
          <p
            class="mt-4 rounded-xl bg-rose-50 p-3 text-sm font-medium text-red-600 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-300 dark:ring-rose-800/80"
            role="alert"
          >
            Google sign-in could not be completed. Please try again.
          </p>
        }

        <button
          class="w-full cursor-pointer rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-emerald-500"
          type="submit"
          [disabled]="submitting()"
        >
          Sign in
        </button>
      </form>

      <div class="my-5 flex items-center gap-3 text-sm text-slate-500 dark:text-slate-400" aria-hidden="true">
        <span class="h-px flex-1 bg-slate-200 dark:bg-slate-700"></span>
        <span>or</span>
        <span class="h-px flex-1 bg-slate-200 dark:bg-slate-700"></span>
      </div>
      <a
        class="flex w-full items-center justify-center gap-2.5 rounded-xl border border-slate-300 bg-white py-2.5 text-sm font-semibold text-slate-800 shadow-2xs transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700"
        data-testid="google-sign-in"
        href="/oauth2/authorization/google"
      >
        <svg class="h-4 w-4" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="#4285F4"
            d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"
          />
          <path
            fill="#34A853"
            d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24z"
          />
          <path
            fill="#FBBC05"
            d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14z"
          />
          <path
            fill="#EA4335"
            d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96z"
          />
        </svg>
        <span>Continue with Google</span>
      </a>

      <div class="mt-6 space-y-2 border-t border-slate-100 pt-5 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
        <p>
          No account?
          <a
            class="font-semibold text-emerald-700 underline hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300"
            routerLink="/auth/sign-up"
          >
            Sign up
          </a>
        </p>
        <p>
          Administrator?
          <a
            class="font-semibold text-slate-800 underline hover:text-slate-900 dark:text-slate-200 dark:hover:text-white"
            routerLink="/auth/admin-sign-in"
          >
            Administrator sign in
          </a>
        </p>
      </div>
    </section>
  `,
})
export class SignInComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly error = signal(false);
  readonly submitting = signal(false);
  readonly googleError = signal(false);
  readonly authenticationFailed = signal(false);

  usernameInvalid(): boolean {
    return (
      (this.form.controls.username.touched && this.form.controls.username.invalid) ||
      this.authenticationFailed()
    );
  }

  passwordInvalid(): boolean {
    return (
      (this.form.controls.password.touched && this.form.controls.password.invalid) ||
      this.authenticationFailed()
    );
  }

  ngOnInit(): void {
    this.googleError.set(this.route.snapshot.queryParamMap.has('googleError'));
  }

  readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(false);
    this.authenticationFailed.set(false);
    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        this.submitting.set(false);
        const returnUrl = this.route.snapshot?.queryParamMap?.get('returnUrl');
        const target =
          returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/';
        this.router.navigateByUrl(target);
      },
      error: () => {
        this.submitting.set(false);
        this.error.set(true);
        // Keep both fields visually invalid so the response does not disclose which credential failed.
        this.authenticationFailed.set(true);
      },
    });
  }
}
