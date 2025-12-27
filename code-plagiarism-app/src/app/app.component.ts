import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

// Angular Material Imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  standalone: true, // This tells Angular NOT to look for an AppModule
  imports: [
    CommonModule,     // For basic Angular features
    RouterOutlet,     // For <router-outlet>
    RouterLink,       // For routerLink attribute
    RouterLinkActive, // Good for styling active links
    MatToolbarModule, // For <mat-toolbar>
    MatIconModule,    // For <mat-icon>
    MatButtonModule   // For mat-button attribute
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'Code Plagiarism Detector';
}