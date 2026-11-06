import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ClaimStatus, TriageLane } from '../../core/models/claim.models';
import { LabelPipe } from '../pipes/label.pipe';

const TONES: Record<string, string> = {
  SUBMITTED: 'info',
  UNDER_REVIEW: 'brand',
  NEEDS_INFO: 'warning',
  APPROVED: 'success',
  PAID: 'success',
  DENIED: 'danger',
  FAST_TRACK: 'success',
  STANDARD: 'brand',
  SPECIAL_INVESTIGATION: 'danger',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'badge ' + tone()"><i></i>{{ value() | label }}</span>`,
  styles: `
    .badge { display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.22rem 0.7rem; border-radius: 999px; font-size: 0.8rem; font-weight: 700; white-space: nowrap; }
    i { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
    .info { background: var(--info-bg); color: var(--brand-800); }
    .brand { background: var(--brand-100); color: var(--brand-700); }
    .warning { background: var(--warning-bg); color: var(--warning); }
    .success { background: var(--success-bg); color: var(--success); }
    .danger { background: var(--danger-bg); color: var(--danger); }
    .danger i, .warning i { animation: pulse-ring 1.8s infinite; }
  `,
})
export class StatusBadgeComponent {
  readonly value = input<ClaimStatus | TriageLane | null>(null);
  readonly tone = computed(() => TONES[this.value() ?? ''] ?? 'brand');
}
