import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-synthetic-note',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="note" title="Every farm, claim and person on this screen is made up">Synthetic data</span>`,
  styles: `
    .note { display: inline-flex; align-items: center; padding: 0.15rem 0.6rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; color: var(--warning); background: var(--warning-bg); border: 1px dashed #e09b00; }
  `,
})
export class SyntheticNoteComponent {}
