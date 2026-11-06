import { Directive, ElementRef, effect, inject, input } from '@angular/core';

@Directive({ selector: '[appCountUp]', standalone: true })
export class CountUpDirective {
  readonly appCountUp = input.required<number>();
  readonly format = input<'number' | 'currency' | 'percent'>('number');
  private readonly element = inject(ElementRef<HTMLElement>);

  constructor() {
    effect((onCleanup) => {
      const target = this.appCountUp() ?? 0;
      const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
      const duration = reduced ? 0 : 900;
      const start = performance.now();
      let frame = 0;
      const tick = (now: number) => {
        const progress = duration === 0 ? 1 : Math.min(1, (now - start) / duration);
        const eased = 1 - Math.pow(1 - progress, 3);
        this.element.nativeElement.textContent = this.render(target * eased);
        if (progress < 1) {
          frame = requestAnimationFrame(tick);
        }
      };
      frame = requestAnimationFrame(tick);
      onCleanup(() => cancelAnimationFrame(frame));
    });
  }

  private render(value: number): string {
    switch (this.format()) {
      case 'currency':
        return value.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
      case 'percent':
        return `${Math.round(value * 100)}%`;
      default:
        return Math.round(value).toLocaleString('en-US');
    }
  }
}
