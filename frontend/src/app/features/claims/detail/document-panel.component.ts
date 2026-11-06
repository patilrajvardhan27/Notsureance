import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { ClaimsApi } from '../../../core/api/claims.api';
import { ClaimDocument } from '../../../core/models/claim.models';
import { problemMessage } from '../../../core/ui/problem';
import { ToastService } from '../../../core/ui/toast.service';
import { listStagger } from '../../../shared/animations';

const ACCEPTED = ['image/jpeg', 'image/png', 'application/pdf'];
const MAX_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-document-panel',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  animations: [listStagger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="card">
      <h3>Photos and documents</h3>
      <label class="drop" [class.over]="dragging()" (dragover)="$event.preventDefault(); dragging.set(true)"
             (dragleave)="dragging.set(false)" (drop)="onDrop($event)">
        <input type="file" accept=".jpg,.jpeg,.png,.pdf" (change)="onPick($event)" [disabled]="uploading()" />
        <strong>{{ uploading() ? 'Uploading…' : 'Drop files here or browse' }}</strong>
        <span class="muted">JPEG, PNG or PDF up to 5 MB</span>
      </label>
      <ul [@listStagger]="documents().length">
        @for (doc of documents(); track doc.id) {
          <li>
            <button type="button" class="link" (click)="download(doc)">{{ doc.fileName }}</button>
            <span class="muted">{{ doc.sizeBytes / 1024 | number: '1.0-0' }} KB · {{ doc.uploadedBy }} · {{ doc.uploadedAt | date: 'MMM d' }}</span>
          </li>
        }
      </ul>
    </article>
  `,
  styles: `
    .drop { display: flex; flex-direction: column; align-items: center; gap: 0.25rem; padding: 1.5rem; border: 2px dashed var(--brand-200); border-radius: var(--radius); background: var(--brand-50); cursor: pointer; transition: background 0.2s, border-color 0.2s, transform 0.2s var(--ease-out); }
    .drop.over { background: var(--brand-100); border-color: var(--brand-700); transform: scale(1.01); }
    input { position: absolute; width: 1px; height: 1px; opacity: 0; }
    ul { list-style: none; padding: 0; margin: 1rem 0 0; }
    li { display: flex; flex-direction: column; padding: 0.55rem 0; border-bottom: 1px solid var(--line); }
    .link { background: none; border: 0; padding: 0; color: var(--brand-700); font: 600 1rem var(--font); text-align: left; cursor: pointer; }
  `,
})
export class DocumentPanelComponent {
  private readonly api = inject(ClaimsApi);
  private readonly toasts = inject(ToastService);
  readonly claimId = input.required<number>();
  readonly documents = signal<ClaimDocument[]>([]);
  readonly uploading = signal(false);
  readonly dragging = signal(false);

  constructor() {
    effect(() => this.api.documents(this.claimId()).subscribe((docs) => this.documents.set(docs)));
  }

  onPick(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.upload(input.files[0]);
    }
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.upload(file);
    }
  }

  download(doc: ClaimDocument): void {
    this.api.download(this.claimId(), doc.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = Object.assign(document.createElement('a'), { href: url, download: doc.fileName });
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  private upload(file: File): void {
    if (!ACCEPTED.includes(file.type) || file.size > MAX_BYTES) {
      this.toasts.show('Use a JPEG, PNG or PDF under 5 MB', 'error');
      return;
    }
    this.uploading.set(true);
    this.api.upload(this.claimId(), file).subscribe({
      next: (doc) => {
        this.documents.update((docs) => [doc, ...docs]);
        this.uploading.set(false);
        this.toasts.show(`${doc.fileName} attached`);
      },
      error: (err) => {
        this.uploading.set(false);
        this.toasts.show(problemMessage(err, 'Upload failed'), 'error');
      },
    });
  }
}
