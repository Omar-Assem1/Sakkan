import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { TokenStorageService } from '../../../../core/services/token-storage.service';
import { PropertySummaryDto } from '../../models/property-browse.model';

@Component({
  selector: 'app-property-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslocoPipe],
  templateUrl: './property-card.component.html',
})
export class PropertyCardComponent {
  private readonly router = inject(Router, { optional: true });
  private readonly tokenStorage = inject(TokenStorageService);

  private _property!: PropertySummaryDto;
  readonly imageError = signal(false);

  @Input({ required: true })
  set property(value: PropertySummaryDto) {
    this._property = value;
    this.imageError.set(false);
  }
  get property(): PropertySummaryDto {
    return this._property;
  }

  @Input() locationName = '';
  @Input() propertyTypeName = '';

  isOwnProperty(): boolean {
    if (!this.tokenStorage.isAuthenticated()) {
      return false;
    }
    const userId = this.tokenStorage.currentUserId();
    if (userId === null) {
      return false;
    }
    return (
      userId === this.property.ownerId ||
      (this.property.agentId != null && userId === this.property.agentId)
    );
  }

  canRequestViewing(): boolean {
    if (!this.tokenStorage.isAuthenticated()) {
      return true;
    }
    if (this.isOwnProperty()) {
      return false;
    }
    return this.tokenStorage.hasRole('BUYER_TENANT', 'OWNER', 'AGENT');
  }

  onViewDetails(): void {
    this.router?.navigate(['/listings', this.property.id]);
  }

  onRequestViewing(): void {
    this.router?.navigate(['/viewings/request', this.property.id]);
  }

  onManageAvailability(): void {
    this.router?.navigate(['/viewings/availability', this.property.id]);
  }
}
