import { CurrencyPipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ClaimAssessment, LossCause } from '../../../core/models/claim.models';
import { Neighborhood } from '../../../core/models/insight.models';
import { RiskGaugeComponent } from '../../../shared/components/risk-gauge.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { LabelPipe } from '../../../shared/pipes/label.pipe';

@Component({
  selector: 'app-claim-insight',
  standalone: true,
  imports: [RiskGaugeComponent, StatusBadgeComponent, LabelPipe, CurrencyPipe, PercentPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './claim-insight.component.html',
  styleUrl: './claim-insight.component.scss',
})
export class ClaimInsightComponent {
  readonly assessment = input.required<ClaimAssessment>();
  readonly statedCause = input.required<LossCause>();
  readonly neighborhood = input<Neighborhood | null>(null);

  readonly causeMismatch = computed(
    () => this.assessment().predictedCause !== this.statedCause() && this.assessment().causeConfidence >= 0.6,
  );
  readonly maxImpact = computed(() => Math.max(1, ...this.assessment().topFactors.map((factor) => factor.impact)));
}
