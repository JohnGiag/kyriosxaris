import { Component, OnInit } from '@angular/core';
import { PushNotificationService } from '../push-notification.service';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})
export class HomePage implements OnInit {
  fcmToken: string | null = null;
  notificationHistory: any[] = [];

  constructor(private pushNotificationService: PushNotificationService) {}

  ngOnInit() {
    this.loadNotificationData();
  }

  loadNotificationData() {
    this.fcmToken = this.pushNotificationService.getToken();
    this.notificationHistory =
      this.pushNotificationService.getNotificationHistory();

    // Update token if it becomes available later
    setTimeout(() => {
      this.fcmToken = this.pushNotificationService.getToken();
    }, 2000);
  }

  copyToken() {
    if (this.fcmToken) {
      navigator.clipboard.writeText(this.fcmToken);
      // You could add a toast notification here
      alert('Token copied to clipboard!');
    }
  }

  clearHistory() {
    this.pushNotificationService.clearNotificationHistory();
    this.notificationHistory = [];
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleString();
  }
}
