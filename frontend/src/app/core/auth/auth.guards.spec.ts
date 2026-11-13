import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { UserProfile } from '../models/auth.models';
import { roleGuard } from './auth.guards';
import { AuthService } from './auth.service';

describe('roleGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/queue' } as RouterStateSnapshot;

  function runAs(profile: UserProfile | null) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { whenSettled: () => of(profile) } }],
    });
    const result = TestBed.runInInjectionContext(() => roleGuard('ADJUSTER', 'MANAGER')(route, state));
    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }

  it('lets matching roles through', async () => {
    expect(await runAs({ id: 1, email: 'p@x.dev', fullName: 'P', role: 'MANAGER' })).toBeTrue();
  });

  it('sends other roles back to the dashboard', async () => {
    expect((await runAs({ id: 2, email: 'a@x.dev', fullName: 'A', role: 'AGENT' })) instanceof UrlTree).toBeTrue();
  });

  it('treats a missing profile as no access', async () => {
    expect((await runAs(null)) instanceof UrlTree).toBeTrue();
  });
});
