import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty rise-in">
      <svg viewBox="0 0 120 80" aria-hidden="true">
        <path d="M5 70h110" stroke="var(--line)" stroke-width="3" />
        <path d="M20 70c10-22 30-22 40 0M60 70c10-30 35-30 45 0" stroke="var(--brand-200)" stroke-width="3" fill="none" />
        <circle cx="92" cy="18" r="9" fill="var(--sky)" class="sun" />
      </svg>
      <h3>{{ title() }}</h3>
      <p class="muted">{{ message() }}</p>
      <ng-content />
    </div>
  `,
  styles: `
    .empty { text-align: center; padding: 2.5rem 1rem; }
    svg { width: 140px; margin-bottom: 0.75rem; }
    .sun { animation: float 4s ease-in-out infinite; }
  `,
})
export class EmptyStateComponent {
  readonly title = input('Nothing here yet');
  readonly message = input('');
}
