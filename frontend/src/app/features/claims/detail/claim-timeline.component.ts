import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TimelineEntry } from '../../../core/models/claim.models';
import { listStagger } from '../../../shared/animations';
import { LabelPipe } from '../../../shared/pipes/label.pipe';

const TITLES: Record<TimelineEntry['type'], string> = {
  FILED: 'Claim filed',
  SCORED: 'Scored and routed',
  ASSIGNED: 'Adjuster assigned',
  STATUS_CHANGED: 'Status changed',
  INFO_PROVIDED: 'Information provided',
  DOCUMENT_ADDED: 'Document added',
  AGENT_RECOMMENDED: 'Agent recommendation',
};

@Component({
  selector: 'app-claim-timeline',
  standalone: true,
  imports: [DatePipe, LabelPipe],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ol class="timeline" [@listStagger]="entries().length">
      @for (entry of entries(); track $index) {
        <li [class]="entry.type">
          <span class="dot"></span>
          <div>
            <strong>{{ titles[entry.type] }}</strong>
            @if (entry.toStatus && entry.type === 'STATUS_CHANGED') {
              <span class="muted"> · {{ entry.fromStatus | label }} → {{ entry.toStatus | label }}</span>
            }
            @if (entry.note) {
              <p>{{ entry.note }}</p>
            }
            <small class="muted">{{ entry.actorName }} · {{ entry.at | date: 'MMM d, h:mm a' }}</small>
          </div>
        </li>
      }
    </ol>
  `,
  styles: `
    .timeline { list-style: none; margin: 0; padding: 0 0 0 0.5rem; position: relative; }
    .timeline::before { content: ''; position: absolute; left: 13px; top: 6px; bottom: 6px; width: 2px; background: var(--line); }
    li { position: relative; display: flex; gap: 1rem; padding-bottom: 1.1rem; }
    .dot { flex: none; width: 12px; height: 12px; margin-top: 6px; border-radius: 50%; background: var(--brand-700); box-shadow: 0 0 0 4px var(--brand-100); z-index: 1; }
    .SCORED .dot { background: var(--sky-strong); }
    .DOCUMENT_ADDED .dot { background: var(--muted); }
    .INFO_PROVIDED .dot { background: #e09b00; }
    .AGENT_RECOMMENDED .dot { background: var(--sky-strong); box-shadow: 0 0 0 4px var(--info-bg); }
    li:last-child .dot { animation: pulse-ring 2s infinite; }
    p { margin: 0.2rem 0; }
  `,
})
export class ClaimTimelineComponent {
  readonly entries = input.required<TimelineEntry[]>();
  readonly titles = TITLES;
}
