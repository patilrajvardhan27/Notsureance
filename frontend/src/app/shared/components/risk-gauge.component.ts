import { ChangeDetectionStrategy, Component, computed, input, signal, afterNextRender } from '@angular/core';

const ARC_LENGTH = 157;

@Component({
  selector: 'app-risk-gauge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <figure class="gauge" [attr.aria-label]="label() + ' ' + percent() + '%'">
      <svg viewBox="0 0 120 70">
        <path d="M10 62a50 50 0 0 1 100 0" class="track" />
        <path d="M10 62a50 50 0 0 1 100 0" class="value" [class]="'value ' + tone()"
              [style.stroke-dashoffset]="offset()" [style.opacity]="percent() === 0 ? 0 : 1" />
      </svg>
      <figcaption>
        <strong>{{ percent() }}%</strong>
        <span>{{ label() }}</span>
      </figcaption>
    </figure>
  `,
  styles: `
    .gauge { margin: 0; position: relative; width: 180px; text-align: center; }
    svg { width: 100%; overflow: visible; }
    path { fill: none; stroke-width: 12; stroke-linecap: round; }
    .track { stroke: var(--brand-100); }
    .value { stroke-dasharray: ${ARC_LENGTH}; transition: stroke-dashoffset 1.1s cubic-bezier(0.22, 1, 0.36, 1), stroke 0.4s; }
    .low { stroke: var(--success); }
    .mid { stroke: #e09b00; }
    .high { stroke: var(--danger); }
    figcaption { position: absolute; inset: auto 0 0; display: flex; flex-direction: column; }
    strong { font-size: 1.7rem; color: var(--brand-900); line-height: 1; }
    span { font-size: 0.8rem; color: var(--muted); }
  `,
})
export class RiskGaugeComponent {
  readonly value = input.required<number>();
  readonly label = input('Fraud risk');
  private readonly ready = signal(false);

  readonly percent = computed(() => Math.round(this.value() * 100));
  readonly tone = computed(() => (this.value() >= 0.4 ? 'high' : this.value() >= 0.2 ? 'mid' : 'low'));
  readonly offset = computed(() => (this.ready() ? ARC_LENGTH * (1 - Math.min(1, this.value())) : ARC_LENGTH));

  constructor() {
    afterNextRender(() => requestAnimationFrame(() => this.ready.set(true)));
  }
}
