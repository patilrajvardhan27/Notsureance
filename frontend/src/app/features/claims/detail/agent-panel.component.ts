import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgentApi } from '../../../core/api/agent.api';
import { AuthService } from '../../../core/auth/auth.service';
import { AgentRun } from '../../../core/models/agent.models';
import { ClaimDetail } from '../../../core/models/claim.models';
import { problemMessage } from '../../../core/ui/problem';
import { ToastService } from '../../../core/ui/toast.service';
import { expand, popIn } from '../../../shared/animations';
import { CitedTextComponent } from '../../../shared/components/cited-text.component';
import { SyntheticNoteComponent } from '../../../shared/components/synthetic-note.component';
import { LabelPipe } from '../../../shared/pipes/label.pipe';

@Component({
  selector: 'app-agent-panel',
  standalone: true,
  imports: [FormsModule, DatePipe, LabelPipe, CitedTextComponent, SyntheticNoteComponent],
  animations: [popIn, expand],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './agent-panel.component.html',
  styleUrl: './agent-panel.component.scss',
})
export class AgentPanelComponent {
  private readonly api = inject(AgentApi);
  private readonly toasts = inject(ToastService);
  readonly auth = inject(AuthService);
  readonly claimId = input.required<number>();
  readonly claimUpdated = output<ClaimDetail>();

  readonly runs = signal<AgentRun[]>([]);
  readonly latest = computed(() => this.runs()[0] ?? null);
  readonly busy = signal(false);
  readonly showTrace = signal(false);
  readonly error = signal('');
  question = '';
  note = '';

  constructor() {
    effect(() => this.api.runs(this.claimId()).subscribe((runs) => this.runs.set(runs)), { allowSignalWrites: true });
  }

  ask(): void {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.api.run(this.claimId(), this.question.trim()).subscribe({
      next: (run) => {
        this.runs.update((list) => [run, ...list].slice(0, 5));
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(problemMessage(err, 'The agent could not finish this run'));
        this.busy.set(false);
      },
    });
  }

  approve(run: AgentRun): void {
    if (!run.proposal) {
      return;
    }
    this.api.approve(run.proposal.id, this.note).subscribe({
      next: (claim) => {
        this.markProposal(run, 'APPROVED');
        this.toasts.show('Proposal approved and applied to the claim');
        this.claimUpdated.emit(claim);
      },
      error: (err) => this.toasts.show(problemMessage(err, 'Could not apply the proposal'), 'error'),
    });
  }

  reject(run: AgentRun): void {
    if (!run.proposal) {
      return;
    }
    this.api.reject(run.proposal.id, this.note).subscribe(() => {
      this.markProposal(run, 'REJECTED');
      this.toasts.show('Proposal rejected. The claim was not changed.', 'info');
    });
  }

  private markProposal(run: AgentRun, status: 'APPROVED' | 'REJECTED'): void {
    this.note = '';
    this.runs.update((list) =>
      list.map((item) => (item.id === run.id && item.proposal ? { ...item, proposal: { ...item.proposal, status } } : item)),
    );
  }
}
