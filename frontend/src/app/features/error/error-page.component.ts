import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-error-page',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslocoModule],
  templateUrl: './error-page.component.html',
})
export class ErrorPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly transloco = inject(TranslocoService);

  errorCode = 404;
  errorTitle = '';
  errorMessage = '';
  errorDetails = '';
  path = '';
  timestamp = '';

  ngOnInit(): void {
    this.readParams(this.route.snapshot?.queryParams || {});
    this.route.queryParams?.subscribe((params) => this.readParams(params));
  }

  private readParams(query: Record<string, string | undefined>): void {
    const data = this.route.snapshot?.data || {};
    const navState = history.state || {};

    const code = Number(query['code'] || data['code'] || navState['code'] || 404);
    this.errorCode = isNaN(code) ? 404 : code;

    this.errorTitle = query['title'] || data['title'] || navState['title'] || '';
    this.errorMessage = query['message'] || data['message'] || navState['message'] || '';
    this.errorDetails = query['detail'] || data['detail'] || navState['detail'] || '';
    this.path = query['path'] || data['path'] || navState['path'] || '';
    this.timestamp = query['timestamp'] || navState['timestamp'] || '';
  }

  get titleKey(): string {
    switch (this.errorCode) {
      case 400:
        return 'ERROR_PAGE.DEFAULT_TITLE';
      case 401:
      case 403:
        return 'ERROR_PAGE.FORBIDDEN_TITLE';
      case 404:
        return 'ERROR_PAGE.NOT_FOUND_TITLE';
      case 500:
      default:
        return 'ERROR_PAGE.SERVER_ERROR_TITLE';
    }
  }

  get messageKey(): string {
    switch (this.errorCode) {
      case 401:
      case 403:
        return 'ERROR_PAGE.FORBIDDEN_MESSAGE';
      case 404:
        return 'ERROR_PAGE.NOT_FOUND_MESSAGE';
      case 500:
      default:
        return 'ERROR_PAGE.SERVER_ERROR_MESSAGE';
    }
  }

  get displayTitle(): string {
    if (this.errorTitle) return this.errorTitle;
    return this.transloco.translate(this.titleKey);
  }

  get displayMessage(): string {
    if (this.errorMessage) return this.errorMessage;
    return this.transloco.translate(this.messageKey);
  }

  goBack(): void {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      this.router.navigate(['/listings']);
    }
  }
}
