import { CurrencyPipe, DatePipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClaimsApi } from '../../core/api/claims.api';
import { ClaimSummary, TriageLane } from '../../core/models/claim.models';
import { problemMessage } from '../../core/ui/problem';
import { ToastService } from '../../core/ui/toast.service';
import { listStagger } from '../../shared/animations';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

const LANES: { lane: TriageLane; title: string; blurb: string }[] = [
  { lane: 'FAST_TRACK', title: 'Fast track', blurb: 'Low risk, small losses. Approve at the model estimate.' },
  { lane: 'STANDARD', title: 'Standard review', blurb: 'Normal adjuster review and dealer estimate.' },
  { lane: 'SPECIAL_INVESTIGATION', title: 'Investigation', blurb: 'Strong fraud or anomaly signals. Verify first.' },
];

@Component({
  selector: 'app-approval-queue',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, PercentPipe, LabelPipe, StatusBadgeComponent],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './approval-queue.component.html',
  styleUrl: './approval-queue.component.scss',
})
export class ApprovalQueueComponent {
  private readonly api = inject(ClaimsApi);
  private readonly toasts = inject(ToastService);
  readonly lanes = LANES;
  readonly claims = signal<ClaimSummary[] | null>(null);
  readonly approving = signal<number | null>(null);
  readonly byLane = computed(() => {
    const grouped: Record<TriageLane, ClaimSummary[]> = { FAST_TRACK: [], STANDARD: [], SPECIAL_INVESTIGATION: [] };
    (this.claims() ?? []).forEach((claim) => grouped[claim.lane ?? 'STANDARD'].push(claim));
    return grouped;
  });

  constructor() {
    this.load();
  }

  quickApprove(claim: ClaimSummary, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    if (!claim.predictedPayout || this.approving()) {
      return;
    }
    this.approving.set(claim.id);
    this.api
      .decide(claim.id, { action: 'APPROVE', amount: claim.predictedPayout, note: 'Fast-track approval at model estimate' })
      .subscribe({
        next: () => {
          this.toasts.show(`${claim.claimNumber} approved`);
          this.claims.update((list) => (list ?? []).filter((item) => item.id !== claim.id));
          this.approving.set(null);
        },
        error: (err) => {
          this.toasts.show(problemMessage(err, 'Approval failed'), 'error');
          this.approving.set(null);
        },
      });
  }

  private load(): void {
    this.api.queue().subscribe((page) => this.claims.set(page.items));
  }
}
