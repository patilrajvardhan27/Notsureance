import { HttpErrorResponse } from '@angular/common/http';
import { problemMessage } from './problem';

describe('problemMessage', () => {
  it('joins the detail with field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Some fields are invalid', errors: { estimatedLoss: 'must be at least 100' } },
    });
    expect(problemMessage(error)).toBe('Some fields are invalid · estimatedLoss: must be at least 100');
  });

  it('falls back when the body is not a problem document', () => {
    expect(problemMessage(new Error('boom'), 'Try again')).toBe('Try again');
  });
});
