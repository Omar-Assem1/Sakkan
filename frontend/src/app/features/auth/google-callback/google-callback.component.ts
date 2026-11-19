import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-google-callback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-md rounded-xl border border-slate-200 bg-white px-6 py-8 text-start">
      @if (loading()) {
        <h1 class="text-2xl font-bold text-slate-900">Signing you in</h1>
        <p class="mt-3 text-slate-600">Please wait while we securely finish Google sign-in.</p>
      } @else if (registrationRequired()) {
        <h1 class="text-2xl font-bold text-slate-900">Choose your username</h1>
        <p class="mt-2 text-sm text-slate-600">Create your Sakan account for {{ email() }}.</p>
        <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="complete()">
          <label class="block">
            <span class="text-sm text-slate-700">Username</span>
            <input class="mt-1 w-full rounded border border-slate-300 px-3 py-2" formControlName="username"
                   autocomplete="username" />
          </label>
          @if (error()) { <p role="alert" class="text-sm text-red-600">{{ error() }}</p> }
          <button class="w-full rounded bg-slate-900 py-2 text-white disabled:opacity-50" type="submit"
                  [disabled]="submitting()">Create account</button>
        </form>
      } @else {
        <h1 class="text-2xl font-bold text-slate-900">Google sign-in failed</h1>
        <p class="mt-3 text-slate-600" role="alert">{{ error() || 'This sign-in link is invalid or expired. Please try again.' }}</p>
        <a class="mt-5 inline-block underline" routerLink="/auth/sign-in">Return to sign in</a>
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
  readonly form = new FormGroup({
    username: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(50),
        Validators.pattern(/^[A-Za-z0-9_.-]+$/)],
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
        this.error.set(err.status === 409
          ? 'That username is already taken. Please choose another.'
          : 'Could not create your account. Please try again.');
      },
    });
  }
}
