import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { PlatformApi } from '../../../core/api/platform.api';
import { LinkedParties } from '../../../core/models/insight.models';
import { problemMessage } from '../../../core/ui/problem';
import { listStagger } from '../../../shared/animations';
import { SyntheticNoteComponent } from '../../../shared/components/synthetic-note.component';

@Component({
  selector: 'app-linked-parties',
  standalone: true,
  imports: [CurrencyPipe, SyntheticNoteComponent],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="card graph">
      <div class="row"><h3>Linked parties</h3><span class="spacer"></span><app-synthetic-note /></div>
      <p class="muted">Other claimants reachable through a shared phone, payout account or repair shop, from the Neo4j graph.</p>
      @if (data(); as d) {
        @if (d.flaggedWithinTwoHops > 0) {
          <p class="banner alert">{{ d.flaggedWithinTwoHops }} confirmed fraud claim(s) within two hops of this policyholder.</p>
        }
        <ul [@listStagger]="d.parties.length">
          @for (party of d.parties; track party.claimantKey) {
            <li [class.hot]="party.flaggedClaims > 0">
              <div class="row">
                <span class="hops">{{ party.hops }} hop{{ party.hops > 1 ? 's' : '' }}</span>
                <strong>{{ party.name }}</strong>
                <span class="spacer"></span>
                <span class="money">{{ party.claimedAmount | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
              </div>
              <div class="via">
                @for (step of party.via; track $index) { <span class="chip">{{ step }}</span> }
              </div>
              <small class="muted">{{ party.claims }} claim(s), {{ party.flaggedClaims }} confirmed fraud</small>
            </li>
          } @empty {
            <li class="muted">No shared identifiers with any other claimant.</li>
          }
        </ul>
      } @else if (error()) {
        <p class="muted">{{ error() }}</p>
      } @else {
        <div class="skeleton" style="height: 120px"></div>
      }
    </article>
  `,
  styles: `
    .graph { border-top: 4px solid var(--brand-800); }
    ul { list-style: none; padding: 0; margin: 0; display: grid; gap: 0.6rem; }
    li { padding: 0.75rem; border-radius: var(--radius-sm); background: var(--brand-50); }
    li.hot { background: var(--danger-bg); }
    .hops { font-size: 0.75rem; font-weight: 700; padding: 0.1rem 0.5rem; border-radius: 999px; background: var(--brand-700); color: #fff; }
    .via { display: flex; flex-wrap: wrap; gap: 0.35rem; margin: 0.4rem 0; }
    .alert { background: var(--danger-bg); border-left-color: var(--danger); color: var(--danger); }
  `,
})
export class LinkedPartiesComponent {
  private readonly api = inject(PlatformApi);
  readonly claimId = input.required<number>();
  readonly data = signal<LinkedParties | null>(null);
  readonly error = signal('');

  constructor() {
    effect(() =>
      this.api.linkedParties(this.claimId()).subscribe({
        next: (result) => this.data.set(result),
        error: (err) => this.error.set(problemMessage(err, 'The fraud graph is not available right now.')),
      }),
    );
  }
}
