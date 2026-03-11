import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import {
  provideHttpClient,
  withFetch,
  withInterceptorsFromDi,
  HTTP_INTERCEPTORS
} from '@angular/common/http';


import { routes } from './app-routing.module';
import { HttpTimeoutInterceptor } from './services/http-timeout.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    // withInterceptorsFromDi() is required to enable class-based interceptors
    provideHttpClient(withFetch(), withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpTimeoutInterceptor,
      multi: true
    }
  ]
};
