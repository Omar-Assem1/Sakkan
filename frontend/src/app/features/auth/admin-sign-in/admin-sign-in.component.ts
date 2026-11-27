import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// TODO i18n: replace the English labels with Transloco keys (EN/AR), same as the normal sign-in form.
@Component({
  selector: 'app-admin-sign-in',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section
      class="mx-auto my-6 max-w-md rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start shadow-2xs transition-colors dark:border-slate-800 dark:bg-slate-900"
    >
      <span
        class="inline-flex items-center rounded-full bg-emerald-100 ps-2.5 pe-2.5 py-0.5 text-xs font-bold uppercase tracking-wider text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
      >
        ADMIN
      </span>
      <h1 class="mt-2 text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
        Administrator sign in
      </h1>

      <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
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
            formControlName="username"
            autocomplete="username"
          />
          @if (usernameInvalid()) {
            <span class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
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
            type="password"
            formControlName="password"
            autocomplete="current-password"
          />
          @if (passwordInvalid()) {
            <span class="mt-1 block text-xs font-medium text-red-600 dark:text-rose-400">
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
        @if (rateLimited()) {
          <p
            class="rounded-xl bg-rose-50 p-3 text-sm font-medium text-red-600 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-300 dark:ring-rose-800/80"
            data-testid="auth-rate-limit"
            role="alert"
          >
            Too many failed attempts. Please try again later.
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

      <p
        class="mt-6 border-t border-slate-100 pt-5 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400"
      >
        Not an administrator?
        <a
          class="font-semibold text-emerald-700 underline hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300"
          routerLink="/auth/sign-in"
        >
          Go to the normal sign in
        </a>
      </p>
    </section>
  `,
})
export class AdminSignInComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly error = signal(false);
  readonly rateLimited = signal(false);
  readonly submitting = signal(false);

  usernameInvalid(): boolean {
    return this.form.controls.username.touched && this.form.controls.username.invalid;
  }

  passwordInvalid(): boolean {
    return this.form.controls.password.touched && this.form.controls.password.invalid;
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
    this.rateLimited.set(false);
    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.adminLogin(username, password).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigateByUrl('/admin');
      },
      error: (err: { status?: number }) => {
        this.submitting.set(false);
        if (err?.status === 429) {
          this.rateLimited.set(true);
        } else {
          this.error.set(true);
        }
      },
    });
  }
}
