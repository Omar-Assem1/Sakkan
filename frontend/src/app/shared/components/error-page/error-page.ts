import { Component, computed, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';

export interface ApiError {
  status: number;
  title: string;
  detail?: string;
  type?: string;
  timestamp?: string;
}

@Component({
  selector: 'app-error-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error-page.html',
  styleUrl: './error-page.css',
})
export class ErrorPageComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /**
   * Error comes from one of three sources (priority order):
   * 1. Router navigation state  → router.navigate(['/error'], { state: { error } })
   * 2. Route data               → { path: '**', data: { error: {...} } }
   * 3. Default fallback
   */
  error: ApiError =
    history.state?.['error'] ??
    this.route.snapshot.data?.['error'] ?? {
      status: 500,
      title: 'Something went wrong',
      detail: 'An unexpected error occurred.',
    };


  goBack() {
    history.back();
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
