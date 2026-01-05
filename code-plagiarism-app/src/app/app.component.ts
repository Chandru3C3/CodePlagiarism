import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Router } from '@angular/router';

// Angular Material Imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from './services/auth.service'; // Assuming you have an AuthService

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
 constructor(public authService: AuthService, private router: Router) {}

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}