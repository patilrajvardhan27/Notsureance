import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClaimsApi } from '../../../core/api/claims.api';
import { AuthService } from '../../../core/auth/auth.service';
import { ClaimDetail, ClaimStatus, DecisionAction } from '../../../core/models/claim.models';
import { problemMessage } from '../../../core/ui/problem';
import { ToastService } from '../../../core/ui/toast.service';
import { expand } from '../../../shared/animations';

interface ActionOption {
  action: DecisionAction;
  label: string;
  style: string;
}

const ACTIONS: Partial<Record<ClaimStatus, ActionOption>> = {
  UNDER_REVIEW: { action: 'START_REVIEW', label: 'Start review', style: 'btn-outline' },
  APPROVED: { action: 'APPROVE', label: 'Approve', style: 'btn-success' },
  NEEDS_INFO: { action: 'REQUEST_INFO', label: 'Request info', style: 'btn-outline' },
  DENIED: { action: 'DENY', label: 'Deny', style: 'btn-danger' },
  PAID: { action: 'MARK_PAID', label: 'Mark as paid', style: 'btn-primary' },
};

@Component({
  selector: 'app-decision-panel',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe],
  animations: [expand],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './decision-panel.component.html',
  styleUrl: './decision-panel.component.scss',
})
export class DecisionPanelComponent {
  private readonly api = inject(ClaimsApi);
  private readonly toasts = inject(ToastService);
  readonly auth = inject(AuthService);
  readonly claim = input.required<ClaimDetail>();
  readonly updated = output<ClaimDetail>();
  readonly chosen = signal<DecisionAction | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');

  readonly options = computed(() =>
    this.claim().nextStatuses.map((status) => ACTIONS[status]).filter((option): option is ActionOption => !!option),
  );
  readonly needsNote = computed(() => ['DENY', 'REQUEST_INFO'].includes(this.chosen() ?? ''));

  readonly form = inject(FormBuilder).group({
    amount: [null as number | null, [Validators.min(0.01)]],
    note: ['', [Validators.maxLength(1000)]],
    suspectedFraud: [false],
  });

  constructor() {
    effect(() => {
      const assessment = this.claim().assessment;
      this.form.patchValue({ amount: Math.round(assessment.predictedPayout) || null }, { emitEvent: false });
    });
  }

  choose(action: DecisionAction): void {
    this.error.set('');
    if (action === 'START_REVIEW' || action === 'MARK_PAID') {
      this.chosen.set(action);
      this.confirm();
      return;
    }
    this.chosen.set(this.chosen() === action ? null : action);
  }

  confirm(): void {
    const action = this.chosen();
    const { amount, note, suspectedFraud } = this.form.getRawValue();
    if (!action || this.busy()) {
      return;
    }
    if (this.needsNote() && !note?.trim()) {
      this.error.set('Please add a note for the agent');
      return;
    }
    this.busy.set(true);
    const id = this.claim().summary.id;
    this.api.decide(id, { action, amount: action === 'APPROVE' ? amount : null, note, suspectedFraud: !!suspectedFraud }).subscribe({
      next: (detail) => this.done(detail, 'Decision saved'),
      error: (err) => this.fail(err),
    });
  }

  respond(): void {
    const note = this.form.getRawValue().note?.trim();
    if (!note) {
      this.error.set('Add the information the adjuster asked for');
      return;
    }
    this.busy.set(true);
    this.api.provideInfo(this.claim().summary.id, note).subscribe({
      next: (detail) => this.done(detail, 'Sent back to the adjuster'),
      error: (err) => this.fail(err),
    });
  }

  private done(detail: ClaimDetail, message: string): void {
    this.busy.set(false);
    this.chosen.set(null);
    this.form.patchValue({ note: '', suspectedFraud: false });
    this.toasts.show(message);
    this.updated.emit(detail);
  }

  private fail(err: unknown): void {
    this.busy.set(false);
    this.error.set(problemMessage(err, 'That action could not be completed'));
  }
}
