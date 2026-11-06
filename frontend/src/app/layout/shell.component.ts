import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { Role } from '../core/models/auth.models';
import { routeTransition } from '../shared/animations';
import { LogoComponent } from '../shared/components/logo.component';
import { ToastOutletComponent } from '../shared/components/toast-outlet.component';
import { LabelPipe } from '../shared/pipes/label.pipe';

interface NavLink {
  path: string;
  label: string;
  roles: Role[];
}

const LINKS: NavLink[] = [
  { path: '/dashboard', label: 'Dashboard', roles: ['AGENT', 'ADJUSTER'] },
  { path: '/claims/new', label: 'File a claim', roles: ['AGENT'] },
  { path: '/claims', label: 'Track claims', roles: ['AGENT', 'ADJUSTER'] },
  { path: '/queue', label: 'Approval queue', roles: ['ADJUSTER'] },
  { path: '/leads', label: 'Leads', roles: ['AGENT'] },
  { path: '/insights', label: 'Model insights', roles: ['ADJUSTER'] },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LogoComponent, ToastOutletComponent, LabelPipe],
  animations: [routeTransition],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly menuOpen = signal(false);
  readonly year = new Date().getFullYear();
  readonly links = computed(() => {
    const role = this.auth.role();
    return role ? LINKS.filter((link) => link.roles.includes(role)) : [];
  });
  readonly initials = computed(() =>
    (this.auth.user()?.fullName ?? '')
      .split(' ')
      .map((part) => part[0])
      .join('')
      .slice(0, 2),
  );

  constructor() {
    inject(Router)
      .events.pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.menuOpen.set(false));
  }

  routeKey(outlet: RouterOutlet): string {
    return outlet.isActivated ? (outlet.activatedRouteData['page'] ?? '') : '';
  }
}
