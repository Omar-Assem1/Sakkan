import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { PropertySummaryDto } from '../../models/property-browse.model';

@Component({
  selector: 'app-property-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslocoPipe],
  templateUrl: './property-card.component.html'
})
export class PropertyCardComponent {
  @Input({ required: true }) property!: PropertySummaryDto;
  @Input() locationName: string = '';
}
