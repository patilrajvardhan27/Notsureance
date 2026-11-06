import { LabelPipe } from './label.pipe';

describe('LabelPipe', () => {
  const pipe = new LabelPipe();

  it('turns enum names into sentence case', () => {
    expect(pipe.transform('UNDER_REVIEW')).toBe('Under review');
    expect(pipe.transform('HAIL')).toBe('Hail');
  });

  it('uses friendlier names where they exist', () => {
    expect(pipe.transform('SPECIAL_INVESTIGATION')).toBe('Investigation');
  });

  it('shows a dash for empty values', () => {
    expect(pipe.transform(null)).toBe('—');
  });
});
