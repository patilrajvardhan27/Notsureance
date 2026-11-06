import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, filter, of, startWith, switchMap } from 'rxjs';
import { PlatformApi } from '../../core/api/platform.api';
import { EQUIPMENT_CATEGORIES } from '../../core/models/policy.models';
import { LeadReceipt, QuoteRequest, QuoteResult } from '../../core/models/quote.models';
import { problemMessage } from '../../core/ui/problem';
import { expand, listStagger, popIn } from '../../shared/animations';
import { CountUpDirective } from '../../shared/directives/count-up.directive';

const STATES = ['AL', 'AR', 'CA', 'CO', 'GA', 'IA', 'ID', 'IL', 'IN', 'KS', 'KY', 'MI', 'MN', 'MO', 'MS', 'MT', 'NC',
  'ND', 'NE', 'NY', 'OH', 'OK', 'OR', 'PA', 'SD', 'TN', 'TX', 'WA', 'WI'];

@Component({
  selector: 'app-quote',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, CountUpDirective],
  animations: [popIn, listStagger, expand],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './quote.component.html',
  styleUrl: './quote.component.scss',
})
export class QuoteComponent {
  private readonly api = inject(PlatformApi);
  private readonly fb = inject(FormBuilder);
  readonly categories = EQUIPMENT_CATEGORIES;
  readonly states = STATES;
  readonly deductibles = [1000, 2500, 5000, 10000];
  readonly currentYear = new Date().getFullYear();
  readonly result = signal<QuoteResult | null>(null);
  readonly receipt = signal<LeadReceipt | null>(null);
  readonly leadError = signal('');
  readonly sending = signal(false);

  readonly quoteForm = this.fb.nonNullable.group({
    category: this.fb.nonNullable.control<QuoteRequest['category']>('TRACTOR'),
    equipmentValue: [250000, [Validators.required, Validators.min(5000), Validators.max(2000000)]],
    modelYear: [this.currentYear - 4, [Validators.required, Validators.min(1980), Validators.max(this.currentYear + 1)]],
    stateCode: ['IA', Validators.required],
    deductible: [2500],
    gpsTracked: [false],
    safetyTraining: [false],
    otherInsuredUnits: [0, [Validators.min(0), Validators.max(50)]],
    claimFreeYears: [3, [Validators.min(0), Validators.max(20)]],
    bundleFarmPolicy: [false],
  });

  readonly leadForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    phone: ['', [Validators.pattern(/^[0-9+()\- ]{7,20}$/)]],
    consentToContact: [false, Validators.requiredTrue],
  });

  constructor() {
    this.quoteForm.valueChanges
      .pipe(
        startWith(this.quoteForm.getRawValue()),
        debounceTime(350),
        filter(() => this.quoteForm.valid),
        switchMap(() => this.api.quote(this.quoteForm.getRawValue()).pipe(catchError(() => of(null)))),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((result) => result && this.result.set(result));
  }

  setDeductible(value: number): void {
    this.quoteForm.patchValue({ deductible: value });
  }

  applySuggestion(action: string): void {
    const patches: Record<string, Partial<QuoteRequest>> = {
      'Add GPS anti-theft tracking': { gpsTracked: true },
      'Complete an operator safety course': { safetyTraining: true },
      'Bundle with a farm and ranch policy': { bundleFarmPolicy: true },
      'Raise the deductible to $5,000': { deductible: 5000 },
    };
    this.quoteForm.patchValue(patches[action] ?? {});
  }

  requestCallback(): void {
    if (this.leadForm.invalid || this.sending()) {
      this.leadForm.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.leadError.set('');
    const lead = this.leadForm.getRawValue();
    this.api.captureLead({ ...lead, quote: this.quoteForm.getRawValue() }).subscribe({
      next: (receipt) => this.receipt.set(receipt),
      error: (err) => {
        this.leadError.set(problemMessage(err, 'We could not save your request'));
        this.sending.set(false);
      },
    });
  }
}
