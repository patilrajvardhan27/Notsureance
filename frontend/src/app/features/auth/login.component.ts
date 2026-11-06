import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { problemMessage } from '../../core/ui/problem';
import { fadeSlide } from '../../shared/animations';

const DEMO_PASSWORD = 'Harvest#2026';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  animations: [fadeSlide],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly next = input<string>();
  readonly busy = signal(false);
  readonly error = signal('');

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });

  readonly demoAccounts = [
    { label: 'Agent', email: 'agent@notsureance.dev', note: 'Files claims and works leads' },
    { label: 'Adjuster', email: 'adjuster@notsureance.dev', note: 'Reviews and approves claims' },
  ];

  useDemo(email: string): void {
    this.form.setValue({ email, password: DEMO_PASSWORD });
  }

  submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set('');
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        const target = this.next();
        this.router.navigateByUrl(target && target.startsWith('/') && !target.startsWith('//') ? target : '/dashboard');
      },
      error: (err) => {
        this.error.set(problemMessage(err, 'Sign in failed'));
        this.busy.set(false);
      },
    });
  }
}
