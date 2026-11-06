import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClaimsApi } from '../../core/api/claims.api';
import { PlatformApi } from '../../core/api/platform.api';
import { ClaimDetail, LOSS_CAUSES, LossCause } from '../../core/models/claim.models';
import { EquipmentView, PolicySummary } from '../../core/models/policy.models';
import { problemMessage } from '../../core/ui/problem';
import { ToastService } from '../../core/ui/toast.service';
import { fadeSlide, listStagger, popIn } from '../../shared/animations';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { LabelPipe } from '../../shared/pipes/label.pipe';

const STEPS = ['Policy', 'Incident', 'Details', 'Review'];

@Component({
  selector: 'app-file-claim',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LabelPipe, CurrencyPipe, DatePipe, RiskGaugeComponent, StatusBadgeComponent],
  animations: [fadeSlide, listStagger, popIn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './file-claim.component.html',
  styleUrl: './file-claim.component.scss',
})
export class FileClaimComponent {
  private readonly platform = inject(PlatformApi);
  private readonly claims = inject(ClaimsApi);
  private readonly toasts = inject(ToastService);

  readonly steps = STEPS;
  readonly causes = LOSS_CAUSES;
  readonly today = new Date().toISOString().slice(0, 10);
  readonly step = signal(0);
  readonly policies = toSignal(this.platform.policies(), { initialValue: [] as PolicySummary[] });
  readonly equipment = signal<EquipmentView[]>([]);
  readonly submitting = signal(false);
  readonly locating = signal(false);
  readonly result = signal<ClaimDetail | null>(null);
  readonly error = signal('');

  readonly form = inject(FormBuilder).group({
    policyId: [null as number | null, Validators.required],
    equipmentId: [null as number | null, Validators.required],
    cause: [null as LossCause | null, Validators.required],
    incidentDate: [this.today, Validators.required],
    harvestCritical: [false],
    description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
    estimatedLoss: [null as number | null, [Validators.required, Validators.min(100), Validators.max(5_000_000)]],
    latitude: [null as number | null],
    longitude: [null as number | null],
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  readonly selectedPolicy = computed(() => this.policies().find((p) => p.id === this.values().policyId));
  readonly selectedEquipment = computed(() => this.equipment().find((e) => e.id === this.values().equipmentId));
  readonly descriptionLength = computed(() => this.values().description?.length ?? 0);

  private readonly stepControls: string[][] = [
    ['policyId', 'equipmentId'],
    ['cause', 'incidentDate'],
    ['description', 'estimatedLoss'],
    [],
  ];

  choosePolicy(policy: PolicySummary): void {
    this.form.patchValue({ policyId: policy.id, equipmentId: null });
    this.platform.equipment(policy.id).subscribe((items) => this.equipment.set(items));
  }

  stepValid(index: number): boolean {
    return this.stepControls[index].every((name) => this.form.get(name)?.valid);
  }

  next(): void {
    const controls = this.stepControls[this.step()];
    controls.forEach((name) => this.form.get(name)?.markAsTouched());
    if (this.stepValid(this.step())) {
      this.step.update((value) => Math.min(value + 1, STEPS.length - 1));
    }
  }

  back(): void {
    this.step.update((value) => Math.max(value - 1, 0));
  }

  useMyLocation(): void {
    if (!navigator.geolocation) {
      this.toasts.show('Location is not available on this device', 'info');
      return;
    }
    this.locating.set(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.form.patchValue({
          latitude: Number(position.coords.latitude.toFixed(6)),
          longitude: Number(position.coords.longitude.toFixed(6)),
        });
        this.locating.set(false);
      },
      () => {
        this.toasts.show('We could not read your location', 'info');
        this.locating.set(false);
      },
      { enableHighAccuracy: true, timeout: 8000 },
    );
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    const value = this.form.getRawValue();
    this.claims
      .file({
        policyId: value.policyId!,
        equipmentId: value.equipmentId!,
        cause: value.cause!,
        incidentDate: value.incidentDate!,
        description: value.description!.trim(),
        estimatedLoss: value.estimatedLoss!,
        latitude: value.latitude,
        longitude: value.longitude,
        harvestCritical: !!value.harvestCritical,
      })
      .subscribe({
        next: (detail) => {
          this.result.set(detail);
          this.toasts.show(`Claim ${detail.summary.claimNumber} filed`);
        },
        error: (err) => {
          this.error.set(problemMessage(err, 'The claim could not be filed'));
          this.submitting.set(false);
        },
      });
  }
}
