import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { DocumentService } from './document.service';
import { EditorComponent } from './editor/editor';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, EditorComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private docs = inject(DocumentService);

  files = signal<string[]>([]);
  message = signal('');
  error = signal('');

  // Currently open document (null = list view).
  open = signal<{ fileName: string; edit: boolean } | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.docs.list().subscribe({
      next: (f) => this.files.set(f),
      error: (e) => this.error.set('Cannot reach backend: ' + e.message),
    });
  }

  upload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.message.set('');
    this.error.set('');
    this.docs.upload(file).subscribe({
      next: (r) => {
        this.message.set('Uploaded ' + r.fileName);
        this.refresh();
      },
      error: (e) => this.error.set('Upload failed: ' + (e?.error?.error ?? e.message)),
    });
    input.value = '';
  }

  edit(fileName: string): void {
    this.open.set({ fileName, edit: true });
  }

  view(fileName: string): void {
    this.open.set({ fileName, edit: false });
  }

  back(): void {
    this.open.set(null);
    this.refresh();
  }
}
