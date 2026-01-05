import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment'; 

export interface LoginRequest {
    emailOrUsername: string;
    password: string;
}

export interface RegisterRequest {
    fullName: string;
    username: string;
    email: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    user: {
        id: number;
        username: string;
        email: string;
        fullName: string;
    };
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = environment.apiUrl+'/auth';
    private currentUserSubject: BehaviorSubject<any>;
    public currentUser: Observable<any>;

    constructor(
        private http: HttpClient,
        @Inject(PLATFORM_ID) private platformId: Object // Inject Platform ID
    ) {
        let storedUser = null;

        // Check if we are in the browser before touching localStorage
        if (isPlatformBrowser(this.platformId)) {
            const data = localStorage.getItem('currentUser');
            storedUser = data ? JSON.parse(data) : null;
        }

        this.currentUserSubject = new BehaviorSubject<any>(storedUser);
        this.currentUser = this.currentUserSubject.asObservable();
    }

    register(userData: RegisterRequest): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/register`, userData);
    }
    login(credentials: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
            .pipe(
                tap(response => {
                    if (isPlatformBrowser(this.platformId)) {
                        localStorage.setItem('currentUser', JSON.stringify(response.user));
                        localStorage.setItem('token', response.token);
                    }
                    this.currentUserSubject.next(response.user);
                })
            );
    }

    logout(): void {
        if (isPlatformBrowser(this.platformId)) {
            localStorage.removeItem('currentUser');
            localStorage.removeItem('token');
        }
        this.currentUserSubject.next(null);
    }

    getToken(): string | null {
        if (isPlatformBrowser(this.platformId)) {
            return localStorage.getItem('token');
        }
        return null;
    }

    isLoggedIn(): boolean {
        return !!this.getToken();
    }
}