import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CountUpDirective } from '../directives/count-up.directive';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CountUpDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="kpi lift" [class]="'kpi lift ' + accent()">
      <span class="label">{{ label() }}</span>
      <strong [appCountUp]="value()" [format]="format()">0</strong>
      @if (hint()) {
        <span class="hint">{{ hint() }}</span>
      }
    </article>
  `,
  styles: `
    .kpi { position: relative; overflow: hidden; padding: 1.25rem 1.4rem; border-radius: var(--radius); background: var(--surface); border: 1px solid var(--line); display: flex; flex-direction: column; gap: 0.25rem; }
    .kpi::after { content: ''; position: absolute; inset: 0 auto 0 0; width: 4px; background: var(--brand-700); }
    .success::after { background: var(--success); }
    .warning::after { background: #e09b00; }
    .sky::after { background: var(--sky-strong); }
    .label { font-size: 0.85rem; color: var(--muted); font-weight: 600; }
    strong { font-size: 2rem; color: var(--brand-900); font-variant-numeric: tabular-nums; line-height: 1.1; }
    .hint { font-size: 0.8rem; color: var(--muted); }
  `,
})
export class KpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly format = input<'number' | 'currency' | 'percent'>('number');
  readonly hint = input<string>('');
  readonly accent = input<'brand' | 'success' | 'warning' | 'sky'>('brand');
}
