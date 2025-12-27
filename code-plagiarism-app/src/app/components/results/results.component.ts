import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ComparisonResult } from '../../models/comparison.model';
import { PlagiarismService } from '../../services/plagiarism.service';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { StorageService } from '../../services/storage.service';


@Component({
  selector: 'app-results',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule
  ],
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css']
})
export class ResultsComponent implements OnInit {
  results: ComparisonResult[] = [];
  submissionIds: number[] = [];
  generatingPdf = false;
  loading = false;

  constructor(
    private plagiarismService: PlagiarismService,
    private snackBar: MatSnackBar,
    private router: Router,
    private storage: StorageService
  ) { }

  ngOnInit() {
    this.loadResults();
  }

  loadResults() {
    const storedResults = this.storage.getItem<ComparisonResult[]>('analysisResults');
    const storedIds = this.storage.getItem<number[]>('submissionIds');

    if (storedResults) {
      this.results = storedResults;
    }

    if (storedIds) {
      this.submissionIds = storedIds;
    }
  }

  getStatusColor(status: string): string {
    const colorMap: { [key: string]: string } = {
      'HIGH': 'warn',
      'MEDIUM': 'accent',
      'LOW': 'primary'
    };
    return colorMap[status] || '';
  }

  getStatusClass(status: string): string {
    return `status-${status?.toLowerCase()}`;
  }

  exportJson() {
    const dataStr = JSON.stringify(this.results, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    this.downloadBlob(blob, `plagiarism-results-${Date.now()}.json`);
    this.snackBar.open('Results exported successfully!', 'Close', { duration: 2000 });
  }

  getAverageSimilarity(): number {
    if (this.results.length === 0) return 0;
    const sum = this.results.reduce((acc, r) => acc + r.overallSimilarity, 0);
    return Math.round(sum / this.results.length);
  }

  getHighRiskCount(): number {
    return this.results.filter(r => r.status === 'HIGH').length;
  }

  getMediumRiskCount(): number {
    return this.results.filter(r => r.status === 'MEDIUM').length;
  }

  getLowRiskCount(): number {
    return this.results.filter(r => r.status === 'LOW').length;
  }

  getProgressBarColor(similarity: number): string {
    if (similarity > 80) return 'warn';
    if (similarity > 60) return 'accent';
    return 'primary';
  }

  navigateToUpload() {
    this.router.navigate(['/upload']);
  }

  generatePdfReport() {
    if (this.submissionIds.length === 0) {
      this.snackBar.open('No submission data available for report generation', 'Close', {
        duration: 3000
      });
      return;
    }

    this.generatingPdf = true;

    this.plagiarismService.generatePdfReport(this.submissionIds).subscribe({
      next: (blob: Blob) => {
        // Check if it's actually a PDF
        if (blob.type === 'application/pdf' || blob.type === 'text/plain') {
          const filename = blob.type === 'application/pdf'
            ? `plagiarism-report-${Date.now()}.pdf`
            : `plagiarism-report-${Date.now()}.txt`;
          this.downloadBlob(blob, filename);
          this.generatingPdf = false;
          this.snackBar.open('Report downloaded successfully!', 'Close', { duration: 3000 });
        } else {
          throw new Error('Invalid response type');
        }
      },
      error: (error) => {
        console.error('PDF generation failed:', error);
        this.generatingPdf = false;
        this.snackBar.open('Failed to generate report. Please try again.', 'Close', {
          duration: 3000
        });
      }
    });
  }

  private downloadBlob(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}