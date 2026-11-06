import { Pipe, PipeTransform } from '@angular/core';

const OVERRIDES: Record<string, string> = {
  FAST_TRACK: 'Fast track',
  SPECIAL_INVESTIGATION: 'Investigation',
  NEEDS_INFO: 'Needs info',
  MECHANICAL_BREAKDOWN: 'Mechanical breakdown',
};

@Pipe({ name: 'label', standalone: true })
export class LabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    if (OVERRIDES[value]) {
      return OVERRIDES[value];
    }
    const words = value.toLowerCase().split('_');
    return words.map((word, i) => (i === 0 ? word[0].toUpperCase() + word.slice(1) : word)).join(' ');
  }
}
