import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PropertyImage { id: number; url: string; thumbnailUrl: string; mimeType: string; fileSize: number; displayOrder: number; primary: boolean; createdAt: string; }

@Injectable({ providedIn: 'root' })
export class PropertyImageService {
  private readonly http = inject(HttpClient);
  private readonly maxBytes = 10 * 1024 * 1024;
  private readonly allowed = new Set(['image/jpeg', 'image/png', 'image/webp']);

  async compress(file: File): Promise<File> {
    if (!this.allowed.has(file.type)) throw new Error('PROPERTY.IMAGES.INVALID_TYPE');
    if (file.size > this.maxBytes) throw new Error('PROPERTY.IMAGES.TOO_LARGE');
    const bitmap = await createImageBitmap(file);
    try {
      const scale = Math.min(1, 2400 / Math.max(bitmap.width, bitmap.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(bitmap.width * scale));
      canvas.height = Math.max(1, Math.round(bitmap.height * scale));
      const context = canvas.getContext('2d');
      if (!context) throw new Error('PROPERTY.IMAGES.COMPRESSION_FAILED');
      context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
      const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob(value => value ? resolve(value) : reject(new Error('PROPERTY.IMAGES.COMPRESSION_FAILED')), 'image/jpeg', 0.82));
      if (blob.size > this.maxBytes) throw new Error('PROPERTY.IMAGES.TOO_LARGE');
      return new File([blob], file.name.replace(/\.[^.]+$/, '') + '.jpg', { type: 'image/jpeg' });
    } finally { bitmap.close(); }
  }

  upload(propertyId: number, files: File[]): Observable<HttpEvent<PropertyImage[]>> {
    const data = new FormData();
    files.forEach(file => data.append('files', file));

    const req = new HttpRequest<FormData>(
      'POST',
      `/api/properties/${propertyId}/images`,
      data,
      { reportProgress: true }
    );

    return this.http.request<PropertyImage[]>(req);
  }
  list(propertyId: number): Observable<PropertyImage[]> { return this.http.get<PropertyImage[]>(`/api/properties/${propertyId}/images`); }
  remove(propertyId: number, imageId: number): Observable<void> { return this.http.delete<void>(`/api/properties/${propertyId}/images/${imageId}`); }
}
