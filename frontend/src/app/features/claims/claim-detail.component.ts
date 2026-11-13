import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClaimsApi } from '../../core/api/claims.api';
import { PlatformApi } from '../../core/api/platform.api';
import { AuthService } from '../../core/auth/auth.service';
import { ClaimDetail } from '../../core/models/claim.models';
import { Neighborhood } from '../../core/models/insight.models';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';
import { ClaimInsightComponent } from './detail/claim-insight.component';
import { ClaimTimelineComponent } from './detail/claim-timeline.component';
import { DecisionPanelComponent } from './detail/decision-panel.component';
import { DocumentPanelComponent } from './detail/document-panel.component';
import { LinkedPartiesComponent } from './detail/linked-parties.component';

@Component({
  selector: 'app-claim-detail',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, LabelPipe, StatusBadgeComponent, EmptyStateComponent,
    ClaimInsightComponent, ClaimTimelineComponent, DecisionPanelComponent, DocumentPanelComponent,
    LinkedPartiesComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './claim-detail.component.html',
  styleUrl: './claim-detail.component.scss',
})
export class ClaimDetailComponent {
  private readonly claims = inject(ClaimsApi);
  private readonly platform = inject(PlatformApi);
  readonly auth = inject(AuthService);
  readonly id = input.required<string>();
  readonly claim = signal<ClaimDetail | null>(null);
  readonly neighborhood = signal<Neighborhood | null>(null);
  readonly missing = signal(false);

  constructor() {
    effect(() => this.load(Number(this.id())), { allowSignalWrites: true });
  }

  refresh(detail: ClaimDetail): void {
    this.claim.set(detail);
  }

  private load(id: number): void {
    this.missing.set(false);
    this.claims.get(id).subscribe({
      next: (detail) => this.claim.set(detail),
      error: () => this.missing.set(true),
    });
    if (this.auth.hasRole('ADJUSTER', 'MANAGER')) {
      this.platform.similarClaims(id).subscribe((near) => this.neighborhood.set(near));
    }
  }
}
