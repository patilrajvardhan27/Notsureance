import { TestBed } from '@angular/core/testing';
import { CitedTextComponent } from './cited-text.component';

describe('CitedTextComponent', () => {
  function render(text: string, valid: string[]) {
    const fixture = TestBed.createComponent(CitedTextComponent);
    fixture.componentRef.setInput('text', text);
    fixture.componentRef.setInput('valid', valid);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('turns citations into chips and flags ids no tool returned', () => {
    const element = render('Excluded [FEC-3.4, FEC-9.9] for [FEQ-2026-THF7Z2W].', ['FEC-3.4', 'FEQ-2026-THF7Z2W']);
    const chips = Array.from(element.querySelectorAll('.cite')).map((chip) => chip.textContent?.trim());
    expect(chips).toEqual(['FEC-3.4', 'FEC-9.9', 'FEQ-2026-THF7Z2W']);
    expect(element.querySelectorAll('.cite.bad').length).toBe(1);
  });

  it('never renders model text as markup', () => {
    const element = render('<img src=x onerror=alert(1)> done [FEC-3.4]', ['FEC-3.4']);
    expect(element.querySelector('img')).toBeNull();
    expect(element.textContent).toContain('<img src=x onerror=alert(1)>');
  });
});
