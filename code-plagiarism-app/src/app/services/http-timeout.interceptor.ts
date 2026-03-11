import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent
} from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { HttpContextToken } from '@angular/common/http';

// Define the token directly here — no separate http.tokens.ts file needed
export const HTTP_TIMEOUT = new HttpContextToken<number>(() => 30_000);

@Injectable()
export class HttpTimeoutInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const timeoutMs = req.context.get(HTTP_TIMEOUT);
    return next.handle(req).pipe(timeout(timeoutMs));
  }
}