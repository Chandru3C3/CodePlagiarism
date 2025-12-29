import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlagiarismService } from '../../services/plagiarism.service';
import { CommonModule } from '@angular/common'; // Required for *ngIf and *ngFor
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  totalSubmissions = 0;
  totalComparisons = 0;
  highRiskCount = 0;
  recentResults: any[] = [];
  loading = true;

  constructor(
    private plagiarismService: PlagiarismService,
    private router: Router
  ) { }

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.loading = true;

   forkJoin({
    submissions: this.plagiarismService.getAllSubmissions(),
    results: this.plagiarismService.getAllResults()
  }).subscribe({
    next: ({ submissions, results }) => {
      this.totalSubmissions = submissions.length;
      this.totalComparisons = results.length;
      this.highRiskCount = results.filter((r: any) => r.status === 'HIGH').length;
      this.recentResults = results.slice(-5).reverse();
      this.loading = false;
    },
    error: (error) => {
      console.error('Dashboard load failed:', error);
      this.loading = false;
    }
  });
  }

  navigateToUpload() {
    this.router.navigate(['/upload']);
  }

  navigateToResults() {
    this.router.navigate(['/results']);
  }
}
