import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ComparisonResult, Submission, UploadResponse } from '../models/comparison.model';
import { environment } from '../../environments/environment';
import { HTTP_TIMEOUT } from './http-timeout.interceptor';

@Injectable({
  providedIn: 'root'
})
export class PlagiarismService {

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  uploadFiles(files: File[], username: string): Observable<UploadResponse> {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    formData.append('username', username);

    return this.http.post<UploadResponse>(`${this.apiUrl}/upload`, formData);
  }

  analyzeSubmissions(submissionIds: number[]): Observable<ComparisonResult[]> {
    return this.http.post<ComparisonResult[]>(`${this.apiUrl}/analyze`, {
      submissionIds
    });
  }

  getAllSubmissions(): Observable<Submission[]> {
    return this.http.get<Submission[]>(`${this.apiUrl}/submissions`);
  }

  getAllResults(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/results`);
  }

  downloadReport(resultId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download-report/${resultId}`, {
      responseType: 'blob',
      headers: new HttpHeaders({
        'Accept': 'application/pdf'
      })
    });
  }

  generatePdfReport(submissionIds: number[]): Observable<Blob> {
  return this.http.post(
    `${this.apiUrl}/generate-report`,
    { submissionIds },
    {
      responseType: 'blob',
      observe: 'body',
      context: new HttpContext().set(HTTP_TIMEOUT, 120_000),
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/pdf, text/plain'
      })
    }
  ); 
}
}

