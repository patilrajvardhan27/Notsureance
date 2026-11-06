import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../core/ui/toast.service';
import { popIn } from '../animations';

@Component({
  selector: 'app-toast-outlet',
  standalone: true,
  animations: [popIn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div @popIn class="toast" [class]="'toast ' + toast.tone" role="status">
          <span>{{ toast.message }}</span>
          <button type="button" (click)="toasts.dismiss(toast.id)" aria-label="Dismiss">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    .stack { position: fixed; right: 1.25rem; bottom: 1.25rem; display: flex; flex-direction: column; gap: 0.6rem; z-index: 50; }
    .toast { display: flex; align-items: center; gap: 1rem; min-width: 280px; max-width: 420px; padding: 0.85rem 1rem; border-radius: var(--radius-sm); background: var(--brand-900); color: #fff; box-shadow: var(--shadow-lg); border-left: 5px solid var(--sky); }
    .success { border-left-color: #43c26b; }
    .error { border-left-color: #ff6b5e; }
    span { flex: 1; }
    button { background: none; border: 0; color: inherit; font-size: 1.3rem; cursor: pointer; opacity: 0.7; }
  `,
})
export class ToastOutletComponent {
  readonly toasts = inject(ToastService);
}
