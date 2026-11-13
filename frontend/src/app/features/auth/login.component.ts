import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { fadeSlide } from '../../shared/animations';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  animations: [fadeSlide],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  readonly auth = inject(AuthService);
  readonly next = input<string>();

  readonly demoUsers = [
    { label: 'Agent', email: 'agent@notsureance.dev', note: 'Files claims and works leads' },
    { label: 'Adjuster', email: 'adjuster@notsureance.dev', note: 'Reviews claims and approves agent proposals' },
    { label: 'Manager', email: 'manager@notsureance.dev', note: 'Reads every claim, report and model metric' },
  ];

  constructor() {
    const router = inject(Router);
    effect(() => {
      if (this.auth.isSignedIn()) {
        router.navigateByUrl(this.safeNext());
      }
    });
  }

  signIn(): void {
    this.auth.login(this.safeNext());
  }

  private safeNext(): string {
    const target = this.next();
    return target && target.startsWith('/') && !target.startsWith('//') ? target : '/dashboard';
  }
}
