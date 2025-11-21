import { Injectable } from '@angular/core';
import { Platform } from '@ionic/angular';
import { BehaviorSubject, Observable } from 'rxjs';
import {
  PushNotifications,
  PushNotificationSchema,
  ActionPerformed,
  Token,
} from '@capacitor/push-notifications';

@Injectable({
  providedIn: 'root',
})
export class PushNotificationService {
  private fcmToken: string | null = null;
  private fcmTokenSubject = new BehaviorSubject<string | null>(null);
  public fcmToken$: Observable<string | null> =
    this.fcmTokenSubject.asObservable();
  private notificationHistory: any[] = [];

  constructor(private platform: Platform) {}

  async initializePushNotifications(): Promise<void> {
    if (!this.platform.is('capacitor')) {
      console.log('Push notifications are only available on native platforms');
      return;
    }

    // Request permission to use push notifications
    let permStatus = await PushNotifications.requestPermissions();

    if (permStatus.receive === 'prompt') {
      permStatus = await PushNotifications.requestPermissions();
    }

    if (permStatus.receive !== 'granted') {
      throw new Error('User denied permissions!');
    }

    // Register with Apple / Google to receive push via APNS/FCM
    await PushNotifications.register();

    // On success, we should be able to receive notifications
    PushNotifications.addListener('registration', (token: Token) => {
      console.log('Push registration success, token: ' + token.value);
      this.fcmToken = token.value;
      this.fcmTokenSubject.next(token.value);
    });

    // Some issue with our setup and push will not work
    PushNotifications.addListener('registrationError', (error: any) => {
      console.error('Error on registration: ' + JSON.stringify(error));
    });

    // Show us the notification payload if the app is open on our device
    PushNotifications.addListener(
      'pushNotificationReceived',
      (notification: PushNotificationSchema) => {
        console.log('Push notification received: ', notification);
        this.notificationHistory.unshift({
          title: notification.title,
          body: notification.body,
          data: notification.data,
          timestamp: new Date(),
        });
      }
    );

    // Method called when tapping on a notification
    PushNotifications.addListener(
      'pushNotificationActionPerformed',
      (action: ActionPerformed) => {
        console.log('Push action performed: ' + JSON.stringify(action));
        this.notificationHistory.unshift({
          title: action.notification.title,
          body: action.notification.body,
          data: action.notification.data,
          timestamp: new Date(),
          tapped: true,
        });
      }
    );
  }

  getToken(): string | null {
    return this.fcmToken;
  }

  getNotificationHistory(): any[] {
    return this.notificationHistory;
  }

  clearNotificationHistory(): void {
    this.notificationHistory = [];
  }
}
