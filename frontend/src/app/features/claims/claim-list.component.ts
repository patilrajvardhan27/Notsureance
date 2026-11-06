import { CurrencyPipe, DatePipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, startWith, switchMap, tap } from 'rxjs';
import { ClaimsApi } from '../../core/api/claims.api';
import { AuthService } from '../../core/auth/auth.service';
import { CLAIM_STATUSES, ClaimSort, ClaimStatus, ClaimSummary, Page, TriageLane } from '../../core/models/claim.models';
import { listStagger } from '../../shared/animations';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-claim-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, StatusBadgeComponent, EmptyStateComponent, LabelPipe, CurrencyPipe,
    DatePipe, PercentPipe],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './claim-list.component.html',
  styleUrl: './claim-list.component.scss',
})
export class ClaimListComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly api = inject(ClaimsApi);
  readonly statuses = CLAIM_STATUSES;
  readonly lanes: TriageLane[] = ['FAST_TRACK', 'STANDARD', 'SPECIAL_INVESTIGATION'];
  readonly sorts: { value: ClaimSort; label: string }[] = [
    { value: 'NEWEST', label: 'Newest first' },
    { value: 'PRIORITY', label: 'Highest priority' },
    { value: 'FRAUD_RISK', label: 'Highest fraud risk' },
    { value: 'LARGEST_LOSS', label: 'Largest loss' },
  ];
  readonly page = signal<Page<ClaimSummary> | null>(null);
  readonly loading = signal(true);
  readonly pageIndex = signal(0);

  readonly filters = inject(FormBuilder).nonNullable.group({
    q: '',
    status: '' as ClaimStatus | '',
    lane: '' as TriageLane | '',
    sort: 'NEWEST' as ClaimSort,
  });

  constructor() {
    this.filters.valueChanges
      .pipe(
        startWith(this.filters.getRawValue()),
        debounceTime(250),
        tap(() => this.pageIndex.set(0)),
        switchMap(() => this.fetch()),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((page) => this.show(page));
  }

  goTo(index: number): void {
    this.pageIndex.set(index);
    this.fetch().subscribe((page) => this.show(page));
  }

  open(claim: ClaimSummary): void {
    this.router.navigate(['/claims', claim.id]);
  }

  private fetch() {
    this.loading.set(true);
    return this.api.search({ ...this.filters.getRawValue(), page: this.pageIndex(), size: 12 });
  }

  private show(page: Page<ClaimSummary>): void {
    this.page.set(page);
    this.loading.set(false);
  }
}
