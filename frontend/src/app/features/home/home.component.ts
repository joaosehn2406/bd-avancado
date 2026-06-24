import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  trackingCode = '';

  constructor(private router: Router) {}

  search() {
    const code = this.trackingCode.trim().toUpperCase();
    if (code) {
      this.router.navigate(['/rastreio', code]);
    }
  }

  goToAdmin() {
    this.router.navigate(['/admin']);
  }
}
