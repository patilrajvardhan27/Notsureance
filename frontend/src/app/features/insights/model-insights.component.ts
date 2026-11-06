import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { PlatformApi } from '../../core/api/platform.api';
import { ModelReport, Segment } from '../../core/models/insight.models';
import { ToastService } from '../../core/ui/toast.service';
import { listStagger } from '../../shared/animations';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';

@Component({
  selector: 'app-model-insights',
  standalone: true,
  imports: [KpiCardComponent, DatePipe, DecimalPipe, PercentPipe],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './model-insights.component.html',
  styleUrl: './model-insights.component.scss',
})
export class ModelInsightsComponent {
  private readonly api = inject(PlatformApi);
  private readonly toasts = inject(ToastService);
  readonly report = signal<ModelReport | null>(null);
  readonly segments = signal<Segment[]>([]);
  readonly training = signal(false);

  readonly maxWeight = computed(() =>
    Math.max(0.01, ...(this.report()?.fraudFeatureWeights ?? []).map((item) => Math.abs(item.weight))),
  );
  readonly matrix = computed(() => {
    const m = this.report()?.fraudHoldout;
    if (!m) {
      return [];
    }
    const total = m.truePositives + m.falsePositives + m.trueNegatives + m.falseNegatives;
    return [
      { label: 'Flagged, was fraud', value: m.truePositives, share: m.truePositives / total, good: true },
      { label: 'Flagged, was clean', value: m.falsePositives, share: m.falsePositives / total, good: false },
      { label: 'Cleared, was fraud', value: m.falseNegatives, share: m.falseNegatives / total, good: false },
      { label: 'Cleared, was clean', value: m.trueNegatives, share: m.trueNegatives / total, good: true },
    ];
  });

  constructor() {
    this.api.modelReport().subscribe((report) => this.report.set(report));
    this.api.segments().subscribe((segments) => this.segments.set(segments));
  }

  retrain(): void {
    this.training.set(true);
    this.api.retrain().subscribe({
      next: (report) => {
        this.report.set(report);
        this.training.set(false);
        this.toasts.show(`Models retrained with ${report.feedbackSamples} adjuster decisions`);
      },
      error: () => this.training.set(false),
    });
  }
}
