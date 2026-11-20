import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-google-callback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section
      class="mx-auto my-8 max-w-md rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start shadow-2xs transition-colors dark:border-slate-800 dark:bg-slate-900"
    >
      @if (loading()) {
        <div class="flex items-center gap-3">
          <div
            class="h-6 w-6 animate-spin rounded-full border-3 border-emerald-600 border-t-transparent"
          ></div>
          <h1 class="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
            Signing you in
          </h1>
        </div>
        <p class="mt-3 text-sm text-slate-600 dark:text-slate-400">
          Please wait while we securely finish Google sign-in.
        </p>
      } @else if (registrationRequired()) {
        <h1 class="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
          Choose your username
        </h1>
        <p class="mt-2 text-sm text-slate-600 dark:text-slate-400">
          Create your Sakan account for <span class="font-semibold">{{ email() }}</span>.
        </p>
        <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="complete()">
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
                Use 3–50 letters, digits, dots, dashes, or underscores.
              </span>
            }
          </label>
          @if (error()) {
            <p
              role="alert"
              class="rounded-xl bg-rose-50 p-3 text-sm font-medium text-red-600 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-300 dark:ring-rose-800/80"
            >
              {{ error() }}
            </p>
          }
          <button
            class="w-full cursor-pointer rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-emerald-500"
            type="submit"
            [disabled]="submitting()"
          >
            Create account
          </button>
        </form>
      } @else {
        <h1 class="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
          Google sign-in failed
        </h1>
        <p
          class="mt-3 rounded-xl bg-rose-50 p-3 text-sm font-medium text-rose-800 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-200 dark:ring-rose-800/80"
          role="alert"
        >
          {{ error() || 'This sign-in link is invalid or expired. Please try again.' }}
        </p>
        <a
          class="mt-5 inline-flex items-center rounded-xl bg-emerald-600 ps-4 pe-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
          routerLink="/auth/sign-in"
        >
          Return to sign in
        </a>
      }
    </section>
  `,
})
export class GoogleCallbackComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly registrationRequired = signal(false);
  readonly email = signal('');
  readonly error = signal('');
  readonly submitting = signal(false);

  usernameInvalid(): boolean {
    return this.form.controls.username.touched && this.form.controls.username.invalid;
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
  });
  private code = '';

  ngOnInit(): void {
    this.code = this.route.snapshot.queryParamMap.get('code') ?? '';
    if (!this.code) {
      this.loading.set(false);
      this.error.set('Google did not return a sign-in code. Please try again.');
      return;
    }
    this.auth.exchangeGoogleCode(this.code).subscribe({
      next: (result) => {
        this.loading.set(false);
        if (result.registrationRequired) {
          this.email.set(result.email ?? '');
          this.registrationRequired.set(true);
        } else {
          void this.router.navigateByUrl('/');
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('This Google sign-in could not be completed. Please try again.');
      },
    });
  }

  complete(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.auth.completeGoogleRegistration(this.code, this.form.controls.username.value).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl('/');
      },
      error: (err: { status?: number }) => {
        this.submitting.set(false);
        this.error.set(
          err.status === 409
            ? 'That username is already taken. Please choose another.'
            : 'Could not create your account. Please try again.',
        );
      },
    });
  }
}
