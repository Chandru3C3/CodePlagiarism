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
    // ── Build structured report object ──────────────────────────────────────
    const now = new Date();
    const timestamp = now.toISOString();
    const readable = now.toLocaleString('en-IN', {
      day: '2-digit', month: 'long', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true
    });

    const report = {
      reportMetadata: {
        title: 'AI Code Plagiarism Detection Report',
        generatedAt: timestamp,
        generatedAtReadable: readable,
        system: 'AI Code Plagiarism Detector',
        institution: 'Anna University, Centre for Distance Education',
        version: '1.0.0'
      },

      summary: {
        totalComparisons: this.results.length,
        highRiskCount: this.getHighRiskCount(),
        mediumRiskCount: this.getMediumRiskCount(),
        lowRiskCount: this.getLowRiskCount(),
        averageSimilarity: parseFloat(this.getAverageSimilarity().toFixed(2)),
        riskBreakdown: {
          high: `${this.getHighRiskCount()} comparison(s) above 80% similarity`,
          medium: `${this.getMediumRiskCount()} comparison(s) between 60–80% similarity`,
          low: `${this.getLowRiskCount()} comparison(s) below 60% similarity`
        }
      },

      comparisons: this.results.map((r, index) => ({
        comparisonId: index + 1,
        files: {
          file1: r.file1Name,
          file2: r.file2Name
        },
        riskStatus: r.status,
        riskDescription: this.getRiskDescription(r.status),
        similarity: {
          overall: parseFloat(r.overallSimilarity.toFixed(2)),
          tokenBased: parseFloat(r.tokenSimilarity.toFixed(2)),
          structuralAST: parseFloat(r.structuralSimilarity.toFixed(2)),
          unit: '%'
        },
        matchingSegments: (r.matches ?? []).map((m, mi) => ({
          segmentId: mi + 1,
          file1Lines: `Lines ${m.line1Range}`,
          file2Lines: `Lines ${m.line2Range}`,
          segmentSimilarity: parseFloat(m.similarity.toFixed(2))
        })),
        totalMatchingSegments: (r.matches ?? []).length
      })),

      // ── Algorithm descriptions (useful for faculty reading the JSON) ──────
      algorithmInfo: {
        tokenBased: {
          name: 'Token-Based Comparison',
          description: 'Lexical analysis converts source code into tokens. Longest Common Subsequence (LCS) algorithm identifies copied token sequences regardless of formatting changes.',
          formula: 'token_score = (2 × LCS_length) / (tokens_file1 + tokens_file2)'
        },
        structuralAST: {
          name: 'Abstract Syntax Tree (AST) Analysis',
          description: 'Source code is parsed into an AST. Tree Edit Distance (TED) compares structural similarity, detecting logical copying even after variable renaming or code restructuring.',
          formula: 'ast_score = 1 − (TED / max(size_tree1, size_tree2))'
        },
        overall: {
          name: 'Combined Weighted Score',
          description: 'Final score is a weighted average of token-based and AST scores.',
          formula: 'overall = (token_score × 0.6) + (ast_score × 0.4)'
        }
      }
    };

    // ── Serialize with 2-space indent for readability ──────────────────────
    const dataStr = JSON.stringify(report, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });

    // ── Filename with readable timestamp ──────────────────────────────────
    const fileTs = now.toISOString().replace(/[:.]/g, '-').slice(0, 19);
    this.downloadBlob(blob, `plagiarism-report-${fileTs}.json`);

    this.snackBar.open('JSON report exported successfully!', 'Close', { duration: 3000 });
  }

  getRiskDescription(status: string): string {
    const map: Record<string, string> = {
      HIGH: 'Similarity above 80% — strong evidence of code copying. Immediate review recommended.',
      MEDIUM: 'Similarity between 60–80% — significant overlap detected. Manual inspection advised.',
      LOW: 'Similarity below 60% — minor overlap. May be coincidental or common patterns.'
    };
    return map[status] ?? 'Unknown risk level.';
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