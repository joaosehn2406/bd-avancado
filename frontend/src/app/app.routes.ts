import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { TrackingComponent } from './features/tracking/tracking.component';
import { AdminComponent } from './features/admin/admin.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'rastreio/:code', component: TrackingComponent },
  { path: 'admin', component: AdminComponent },
  { path: '**', redirectTo: '' }
];
