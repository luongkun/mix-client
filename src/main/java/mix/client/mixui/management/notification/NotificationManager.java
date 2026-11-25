package mix.client.mixui.management.notification;

import java.util.concurrent.LinkedBlockingQueue;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;

public class NotificationManager {

	private LinkedBlockingQueue<Notification> notifications = new LinkedBlockingQueue<Notification>();
	
	public NotificationManager() {
		Mix.getInstance().getEventManager().register(new NotificationHandler(notifications));
	}
	
	public void post(TranslateText title, TranslateText message, NotificationType type) {
		notifications.add(new Notification(title, message, type));
	}
}
