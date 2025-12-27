import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PlagiarismService } from '../../services/plagiarism.service';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { StorageService } from '../../services/storage.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatButtonModule
  ],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css']
})
export class UploadComponent {
  selectedFiles: File[] = [];
  uploading = false;
  uploadedIds: number[] = [];
  dragOver = false;

  constructor(
    private plagiarismService: PlagiarismService,
    private router: Router,
    private snackBar: MatSnackBar,
    private storage: StorageService
  ) { }

  onFileSelected(event: any) {
    const files: FileList = event.target.files;
    this.addFiles(files);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragOver = false;

    if (event.dataTransfer?.files) {
      this.addFiles(event.dataTransfer.files);
    }
  }

  addFiles(fileList: FileList) {
    const files = Array.from(fileList);
    const validExtensions = ['.java', '.py', '.cpp', '.c', '.js'];

    files.forEach(file => {
      const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
      if (validExtensions.includes(extension)) {
        if (!this.selectedFiles.find(f => f.name === file.name)) {
          this.selectedFiles.push(file);
        }
      } else {
        this.snackBar.open(`Invalid file type: ${file.name}`, 'Close', { duration: 3000 });
      }
    });
  }

  removeFile(index: number) {
    this.selectedFiles.splice(index, 1);
  }

  uploadFiles() {
    if (this.selectedFiles.length < 2) {
      this.snackBar.open('Please select at least 2 files to compare', 'Close', { duration: 3000 });
      return;
    }

    this.uploading = true;
    const username = 'student_user';

    this.plagiarismService.uploadFiles(this.selectedFiles, username).subscribe({
      next: (response) => {
        this.uploadedIds = response.ids;
        localStorage.setItem('submissionIds', JSON.stringify(this.uploadedIds));
        this.snackBar.open('Files uploaded successfully!', 'Close', { duration: 2000 });
        this.analyzeFiles();
      },
      error: (error) => {
        console.error('Upload failed:', error);
        this.uploading = false;
        this.snackBar.open('Upload failed. Please try again.', 'Close', { duration: 3000 });
      }
    });
  }

  analyzeFiles() {
    this.plagiarismService.analyzeSubmissions(this.uploadedIds).subscribe({
      next: (results) => {
        this.uploading = false;
        this.storage.setItem('analysisResults', results);
        this.storage.setItem('submissionIds', this.uploadedIds);

        this.snackBar.open('Analysis completed!', 'Close', { duration: 2000 });
        this.router.navigate(['/results']);
      },
      error: (error) => {
        console.error('Analysis failed:', error);
        this.uploading = false;
        this.snackBar.open('Analysis failed. Please try again.', 'Close', { duration: 3000 });
      }
    });
  }

  getFileSize(file: File): string {
    const kb = file.size / 1024;
    return kb > 1024 ? (kb / 1024).toFixed(2) + ' MB' : kb.toFixed(2) + ' KB';
  }

  getFileIcon(fileName: string): string {
    const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    const iconMap: { [key: string]: string } = {
      '.java': 'code',
      '.py': 'code',
      '.cpp': 'code',
      '.c': 'code',
      '.js': 'code'
    };
    return iconMap[extension] || 'insert_drive_file';
  }
}