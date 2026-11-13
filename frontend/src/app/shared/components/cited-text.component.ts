import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

interface Segment {
  text: string;
  cited: boolean;
  valid: boolean;
}

// Splits [FEQ-...] and [FEC-x.y] citations into chips without ever rendering model text as HTML.
@Component({
  selector: 'app-cited-text',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@for (part of segments(); track $index) {@if (part.cited) {<span class="cite" [class.bad]="!part.valid"
    [title]="part.valid ? 'Returned by a tool in this run' : 'No tool returned this id'">{{ part.text }}</span>} @else {<span>{{ part.text }}</span>}}`,
  styles: `
    .cite { display: inline-block; margin: 0 0.15rem; padding: 0 0.4rem; border-radius: 4px; font-size: 0.85em; font-weight: 700; background: var(--brand-100); color: var(--brand-800); }
    .bad { background: var(--danger-bg); color: var(--danger); text-decoration: line-through; }
  `,
})
export class CitedTextComponent {
  readonly text = input<string | null>('');
  readonly valid = input<string[]>([]);

  readonly segments = computed<Segment[]>(() => {
    const value = this.text() ?? '';
    const parts: Segment[] = [];
    let last = 0;
    for (const match of value.matchAll(/\[([^\]]{3,200})\]/g)) {
      parts.push({ text: value.slice(last, match.index), cited: false, valid: true });
      for (const id of match[1].split(/[,;]\s*/)) {
        parts.push({ text: id.trim(), cited: true, valid: this.valid().includes(id.trim()) });
      }
      last = (match.index ?? 0) + match[0].length;
    }
    parts.push({ text: value.slice(last), cited: false, valid: true });
    return parts.filter((part) => part.text.length > 0);
  });
}
