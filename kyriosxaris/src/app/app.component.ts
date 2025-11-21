import { Component, OnInit } from '@angular/core';
import { Platform } from '@ionic/angular';
import { PushNotificationService } from './push-notification.service';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
  standalone: false,
})
export class AppComponent implements OnInit {
  constructor(
    private platform: Platform,
    private pushNotificationService: PushNotificationService
  ) {}

  async ngOnInit() {
    await this.platform.ready();

    if (this.platform.is('capacitor')) {
      try {
        await this.pushNotificationService.initializePushNotifications();
        console.log('Push notifications initialized successfully');
      } catch (error) {
        console.error('Error initializing push notifications:', error);
      }
    }
  }
}
