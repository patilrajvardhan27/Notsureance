import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="logo" [class.light]="light()">
      <svg viewBox="0 0 64 64" aria-hidden="true">
        <path d="M32 4 8 13v17c0 15 10 26 24 30 14-4 24-15 24-30V13L32 4Z" class="shield" />
        <path d="M17 40c6-7 24-7 30 0M19 33c6-6 20-6 26 0M22 26c5-4 15-4 20 0" class="rows" />
        <circle cx="32" cy="18" r="3.4" class="sun" />
      </svg>
      <span class="word">Notsureance</span>
    </span>
  `,
  styles: `
    .logo { display: inline-flex; align-items: center; gap: 0.55rem; font-weight: 800; font-size: 1.3rem; color: var(--brand-700); letter-spacing: -0.01em; }
    svg { width: 34px; height: 34px; transition: transform 0.4s var(--ease-out); }
    .logo:hover svg { transform: rotate(-6deg) scale(1.06); }
    .shield { fill: var(--brand-700); }
    .rows { stroke: #fff; stroke-width: 3.2; fill: none; stroke-linecap: round; stroke-dasharray: 60; animation: draw 1.4s var(--ease-out) both; }
    .sun { fill: var(--sky); }
    .light { color: #fff; }
    .light .shield { fill: #fff; }
    .light .rows { stroke: var(--brand-700); }
    @keyframes draw { from { stroke-dashoffset: 60; } to { stroke-dashoffset: 0; } }
  `,
})
export class LogoComponent {
  readonly light = input(false);
}
