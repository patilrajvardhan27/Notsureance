import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { roleGuard, signedInGuard } from './auth.guards';
import { AuthService } from './auth.service';

describe('auth guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/claims' } as RouterStateSnapshot;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
  });

  it('sends signed-out users to login and remembers where they were going', () => {
    const result = TestBed.runInInjectionContext(() => signedInGuard(route, state)) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login?next=%2Fclaims');
  });

  it('keeps agents out of adjuster pages', () => {
    spyOn(TestBed.inject(AuthService), 'hasRole').and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => roleGuard('ADJUSTER')(route, state));
    expect(result instanceof UrlTree).toBeTrue();
  });
});
