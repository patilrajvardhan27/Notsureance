import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { PlatformApi } from '../../core/api/platform.api';
import { FraudRing } from '../../core/models/insight.models';
import { problemMessage } from '../../core/ui/problem';
import { listStagger } from '../../shared/animations';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { SyntheticNoteComponent } from '../../shared/components/synthetic-note.component';

@Component({
  selector: 'app-fraud-rings',
  standalone: true,
  imports: [CurrencyPipe, EmptyStateComponent, SyntheticNoteComponent],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="row">
        <div>
          <p class="eyebrow">Neo4j graph</p>
          <h1>Fraud rings</h1>
        </div>
        <span class="spacer"></span>
        <app-synthetic-note />
      </div>
      <p class="muted">Claimants who share two or more identifiers (phone, payout account, repair shop), grouped into
        connected groups and ranked by total claimed. Per-claim models cannot see these links.</p>
      @if (rings(); as list) {
        <div class="rings" [@listStagger]="list.length">
          @for (ring of list; track $index) {
            <article class="card ring lift">
              <div class="row">
                <h3>{{ ring.members.length }} linked claimants</h3>
                <span class="spacer"></span>
                <strong class="money">{{ ring.totalClaimed | currency: 'USD' : 'symbol' : '1.0-0' }}</strong>
              </div>
              <div class="shared">
                @for (id of ring.sharedIdentifiers; track id) { <span class="chip">{{ id }}</span> }
              </div>
              <ul>
                @for (member of ring.members; track member.claimantKey) {
                  <li>
                    <span>{{ member.name }}</span>
                    <span class="muted">{{ member.claims }} claim(s)</span>
                    @if (member.flaggedClaims > 0) { <span class="flag">{{ member.flaggedClaims }} confirmed fraud</span> }
                  </li>
                }
              </ul>
            </article>
          } @empty {
            <app-empty-state title="No rings found" message="No two claimants share two or more identifiers." />
          }
        </div>
      } @else if (error()) {
        <app-empty-state title="Graph unavailable" [message]="error()" />
      } @else {
        <div class="skeleton" style="height: 200px"></div>
      }
    </div>
  `,
  styles: `
    .rings { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 1.25rem; margin-top: 1.25rem; }
    .ring { border-top: 4px solid var(--danger); }
    .shared { display: flex; flex-wrap: wrap; gap: 0.35rem; margin: 0.5rem 0 1rem; }
    ul { list-style: none; padding: 0; margin: 0; }
    li { display: flex; gap: 0.75rem; align-items: center; padding: 0.45rem 0; border-bottom: 1px solid var(--line); }
    .flag { margin-left: auto; font-size: 0.8rem; font-weight: 700; color: var(--danger); }
  `,
})
export class FraudRingsComponent {
  readonly rings = signal<FraudRing[] | null>(null);
  readonly error = signal('');

  constructor() {
    inject(PlatformApi).fraudRings().subscribe({
      next: (rings) => this.rings.set(rings),
      error: (err) => this.error.set(problemMessage(err, 'The fraud graph is not available right now.')),
    });
  }
}
