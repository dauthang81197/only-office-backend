import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api.config';

export interface EditorConfigResponse {
  documentServerApiUrl: string;
  config: any;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private base = `${API_BASE}/api/documents`;

  list(): Observable<string[]> {
    return this.http.get<string[]>(this.base);
  }

  upload(file: File): Observable<{ fileName: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ fileName: string }>(this.base, form);
  }

  config(fileName: string, edit: boolean): Observable<EditorConfigResponse> {
    return this.http.get<EditorConfigResponse>(
      `${this.base}/${encodeURIComponent(fileName)}/config`,
      { params: { edit } }
    );
  }
}
