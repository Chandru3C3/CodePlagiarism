import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ComparisonResult, Submission, UploadResponse } from '../models/comparison.model';

@Injectable({
  providedIn: 'root'
})
export class PlagiarismService {
  private apiUrl = 'http://localhost:8080/api';

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
    return this.http.post(`${this.apiUrl}/generate-report`, 
      { submissionIds },
      { 
        responseType: 'blob',  // This handles both text and binary
        headers: new HttpHeaders({
          'Accept': 'text/plain, application/pdf, application/json'
        })
      }
    );
}
}

