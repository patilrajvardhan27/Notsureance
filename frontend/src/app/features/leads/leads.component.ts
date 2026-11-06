import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PlatformApi } from '../../core/api/platform.api';
import { Lead, LeadStatus } from '../../core/models/quote.models';
import { ToastService } from '../../core/ui/toast.service';
import { listStagger } from '../../shared/animations';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';

const COLUMNS: { status: LeadStatus; title: string }[] = [
  { status: 'NEW', title: 'New' },
  { status: 'CONTACTED', title: 'Contacted' },
  { status: 'CONVERTED', title: 'Won' },
  { status: 'LOST', title: 'Lost' },
];

@Component({
  selector: 'app-leads',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, EmptyStateComponent, KpiCardComponent],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <p class="eyebrow">Growth</p>
      <h1>Quote leads</h1>
      <p class="muted">Prospects from the public quote page, routed to the agent with the lightest new-lead load.</p>
      <div class="grid grid-4 kpis">
        <app-kpi-card label="Open pipeline premium" [value]="pipelinePremium()" format="currency" />
        <app-kpi-card label="Won premium" [value]="wonPremium()" format="currency" accent="success" />
        <app-kpi-card label="Win rate" [value]="winRate()" format="percent" accent="sky" />
      </div>
      @if (leads().length === 0) {
        <app-empty-state title="No leads yet" message="Share the quote page to start filling the pipeline.">
          <a class="btn btn-primary" routerLink="/quote">Open quote page</a>
        </app-empty-state>
      } @else {
        <div class="board">
          @for (column of columns; track column.status) {
            <section class="column">
              <h3>{{ column.title }} <span class="muted">{{ grouped()[column.status].length }}</span></h3>
              <div [@listStagger]="grouped()[column.status].length" class="cards">
                @for (lead of grouped()[column.status]; track lead.id) {
                  <article class="lead lift">
                    <strong>{{ lead.fullName }}</strong>
                    <span class="muted small">{{ lead.category }} · {{ lead.stateCode }} · {{ lead.createdAt | date: 'MMM d' }}</span>
                    <span class="money">{{ lead.annualPremium | currency: 'USD' : 'symbol' : '1.0-0' }} / yr</span>
                    <a class="small" [href]="'mailto:' + lead.email">{{ lead.email }}</a>
                    <select class="input" [value]="lead.status" (change)="move(lead, $any($event.target).value)"
                            [attr.aria-label]="'Status for ' + lead.fullName">
                      @for (option of columns; track option.status) {
                        <option [value]="option.status">{{ option.title }}</option>
                      }
                    </select>
                  </article>
                }
              </div>
            </section>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .kpis { margin: 1.25rem 0; }
    .board { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .column { padding: 1rem; border-radius: var(--radius); background: var(--brand-50); }
    .cards { display: grid; gap: 0.7rem; }
    .lead { display: flex; flex-direction: column; gap: 0.3rem; padding: 0.9rem; border-radius: var(--radius-sm); background: var(--surface); border: 1px solid var(--line); }
    .small { font-size: 0.85rem; }
    select { min-height: 36px; margin-top: 0.4rem; }
    @media (max-width: 960px) { .board { grid-template-columns: 1fr 1fr; } }
  `,
})
export class LeadsComponent {
  private readonly api = inject(PlatformApi);
  private readonly toasts = inject(ToastService);
  readonly columns = COLUMNS;
  readonly leads = signal<Lead[]>([]);

  readonly grouped = computed(() => {
    const groups: Record<LeadStatus, Lead[]> = { NEW: [], CONTACTED: [], CONVERTED: [], LOST: [] };
    this.leads().forEach((lead) => groups[lead.status].push(lead));
    return groups;
  });
  readonly pipelinePremium = computed(() =>
    [...this.grouped().NEW, ...this.grouped().CONTACTED].reduce((sum, lead) => sum + lead.annualPremium, 0),
  );
  readonly wonPremium = computed(() => this.grouped().CONVERTED.reduce((sum, lead) => sum + lead.annualPremium, 0));
  readonly winRate = computed(() => {
    const closed = this.grouped().CONVERTED.length + this.grouped().LOST.length;
    return closed === 0 ? 0 : this.grouped().CONVERTED.length / closed;
  });

  constructor() {
    this.api.leads().subscribe((leads) => this.leads.set(leads));
  }

  move(lead: Lead, status: LeadStatus): void {
    this.api.updateLead(lead.id, status).subscribe((updated) => {
      this.leads.update((list) => list.map((item) => (item.id === updated.id ? updated : item)));
      this.toasts.show(`${lead.fullName} moved to ${COLUMNS.find((c) => c.status === status)?.title}`);
    });
  }
}
