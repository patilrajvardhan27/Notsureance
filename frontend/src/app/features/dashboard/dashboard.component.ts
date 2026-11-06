import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ClaimsApi } from '../../core/api/claims.api';
import { PlatformApi } from '../../core/api/platform.api';
import { AuthService } from '../../core/auth/auth.service';
import { CLAIM_STATUSES, ClaimSummary, TriageLane } from '../../core/models/claim.models';
import { DashboardStats, Segment } from '../../core/models/insight.models';
import { listStagger } from '../../shared/animations';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [KpiCardComponent, StatusBadgeComponent, LabelPipe, CurrencyPipe, DatePipe, RouterLink],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  readonly stats = signal<DashboardStats | null>(null);
  readonly recent = signal<ClaimSummary[]>([]);
  readonly segments = signal<Segment[]>([]);
  readonly lanes: TriageLane[] = ['FAST_TRACK', 'STANDARD', 'SPECIAL_INVESTIGATION'];

  readonly statusBars = computed(() => {
    const data = this.stats();
    if (!data) {
      return [];
    }
    const max = Math.max(1, ...CLAIM_STATUSES.map((status) => data.claimsByStatus[status] ?? 0));
    return CLAIM_STATUSES.map((status) => ({
      status,
      count: data.claimsByStatus[status] ?? 0,
      width: ((data.claimsByStatus[status] ?? 0) / max) * 100,
    }));
  });

  readonly greeting = computed(() => {
    const hour = new Date().getHours();
    const part = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    return `${part}, ${this.auth.user()?.fullName.split(' ')[0] ?? ''}`;
  });

  constructor() {
    const claims = inject(ClaimsApi);
    const platform = inject(PlatformApi);
    forkJoin({
      stats: platform.dashboard(),
      recent: claims.search({ sort: 'NEWEST', size: 6 }),
      segments: platform.segments(),
    }).subscribe(({ stats, recent, segments }) => {
      this.stats.set(stats);
      this.recent.set(recent.items);
      this.segments.set(segments);
    });
  }

  open(claim: ClaimSummary): void {
    this.router.navigate(['/claims', claim.id]);
  }
}
