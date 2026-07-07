import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { DocumentService } from '../document.service';

declare const DocsAPI: any;

@Component({
  selector: 'app-editor',
  standalone: true,
  template: `
    <div class="editor-bar">
      <button (click)="close.emit()">← Back</button>
      <span>{{ fileName }} <em>({{ edit ? 'edit' : 'view' }})</em></span>
    </div>
    <div class="editor-host">
      <div #placeholder id="onlyoffice-placeholder"></div>
    </div>
    @if (error) { <p class="err">{{ error }}</p> }
  `,
  styles: [`
    .editor-bar { display:flex; gap:12px; align-items:center; padding:8px 12px; background:#f4f6f8; border-bottom:1px solid #ddd; }
    .editor-bar button { cursor:pointer; }
    .editor-host, #onlyoffice-placeholder { height: calc(100vh - 46px); }
    .err { color:#c0392b; padding:12px; }
  `],
})
export class EditorComponent implements AfterViewInit, OnDestroy {
  @Input() fileName!: string;
  @Input() edit = true;
  @Output() close = new EventEmitter<void>();
  @ViewChild('placeholder') placeholder!: ElementRef<HTMLDivElement>;

  private docs = inject(DocumentService);
  private editorInstance: any;
  error = '';

  ngAfterViewInit(): void {
    this.docs.config(this.fileName, this.edit).subscribe({
      next: (res) => this.loadApiAndInit(res.documentServerApiUrl, res.config),
      error: (e) => (this.error = 'Failed to load config: ' + (e?.error?.error ?? e.message)),
    });
  }

  private loadApiAndInit(apiUrl: string, config: any): void {
    const init = () => {
      try {
        this.editorInstance = new DocsAPI.DocEditor('onlyoffice-placeholder', config);
      } catch (e: any) {
        this.error = 'Editor init failed: ' + e.message;
      }
    };

    if (typeof DocsAPI !== 'undefined') {
      init();
      return;
    }
    // Load the Document Server api.js once, then initialise.
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${apiUrl}"]`);
    if (existing) {
      existing.addEventListener('load', init);
      return;
    }
    const script = document.createElement('script');
    script.src = apiUrl;
    script.onload = init;
    script.onerror = () => (this.error = 'Cannot load Document Server api.js from ' + apiUrl);
    document.body.appendChild(script);
  }

  ngOnDestroy(): void {
    if (this.editorInstance?.destroyEditor) {
      this.editorInstance.destroyEditor();
    }
  }
}
