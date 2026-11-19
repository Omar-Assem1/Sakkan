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
    <section class="mx-auto max-w-md rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">Sign in</h1>

      <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="block">
          <span class="text-sm text-slate-700">Username</span>
          <input class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2"
                 formControlName="username" autocomplete="username" />
        </label>
        <label class="block">
          <span class="text-sm text-slate-700">Password</span>
          <input class="mt-1 w-full rounded border border-slate-300 ps-3 pe-3 py-2"
                 type="password" formControlName="password" autocomplete="current-password" />
        </label>

        @if (error()) {
          <p class="text-sm text-red-600" data-testid="auth-error" role="alert">Invalid username or password.</p>
        }
      @if (googleError()) {
        <p class="mt-4 text-sm text-red-600" role="alert">Google sign-in could not be completed. Please try again.</p>
      }

        <button class="w-full rounded bg-slate-900 py-2 text-white disabled:opacity-50"
                type="submit" [disabled]="submitting()">Sign in</button>
      </form>

      <div class="my-5 flex items-center gap-3 text-sm text-slate-500" aria-hidden="true">
        <span class="h-px flex-1 bg-slate-200"></span><span>or</span><span class="h-px flex-1 bg-slate-200"></span>
      </div>
      <a class="flex w-full items-center justify-center rounded border border-slate-300 py-2 text-slate-800 hover:bg-slate-50"
         data-testid="google-sign-in" href="/oauth2/authorization/google">Continue with Google</a>

      <p class="mt-4 text-sm text-slate-600">
        No account? <a class="underline" routerLink="/auth/sign-up">Sign up</a>
      </p>
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
    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.submitting.set(false);
        this.error.set(true);
      },
    });
  }
}
