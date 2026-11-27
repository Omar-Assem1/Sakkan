import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { PropertyService } from '../services/property.service';
import { PropertyResponse, PropertyStatus } from '../models/property.model';

@Component({
  selector: 'app-my-properties',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslocoDirective],
  templateUrl: './my-properties.component.html'
})
export class MyPropertiesComponent implements OnInit {
  private readonly propertyService = inject(PropertyService);
  private readonly router = inject(Router);

  properties = signal<PropertyResponse[]>([]);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);

  ngOnInit(): void {
    this.loadMyProperties(0);
  }

  loadMyProperties(page = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.propertyService.getMyProperties(page, 10).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.properties.set(res.content);
        this.currentPage.set(res.page);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('MY_PROPERTIES.ERRORS.LOAD_FAILED');
      }
    });
  }

  onPublish(id: number): void {
    this.propertyService.publishProperty(id).subscribe({
      next: (updated) => {
        this.properties.set(
          this.properties().map((p) => (p.id === id ? { ...p, status: updated.status } : p))
        );
      },
      error: () => {
        this.errorMessage.set('MY_PROPERTIES.ERRORS.ACTION_FAILED');
      }
    });
  }

  onArchive(id: number): void {
    this.propertyService.archiveProperty(id).subscribe({
      next: (updated) => {
        this.properties.set(
          this.properties().map((p) => (p.id === id ? { ...p, status: updated.status } : p))
        );
      },
      error: () => {
        this.errorMessage.set('MY_PROPERTIES.ERRORS.ACTION_FAILED');
      }
    });
  }

  onEdit(id: number): void {
    this.router.navigate(['/properties', id, 'edit']);
  }

  onManageAvailability(id: number): void {
    this.router.navigate(['/viewings/availability', id]);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.loadMyProperties(page);
    }
  }

  getStatusClass(status: PropertyStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800';
      case 'DRAFT':
        return 'bg-amber-100 text-amber-800 border-amber-200 dark:bg-amber-950/70 dark:text-amber-300 dark:border-amber-800';
      case 'ARCHIVED':
        return 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700';
      default:
        return 'bg-sky-100 text-sky-800 border-sky-200 dark:bg-sky-950/70 dark:text-sky-300 dark:border-sky-800';
    }
  }
}
