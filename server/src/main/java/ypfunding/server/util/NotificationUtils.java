package ypfunding.server.util;

import ypfunding.common.notifications.Notification;

public class NotificationUtils {

    private NotificationUtils(){

    }


    public static void sendNotification(Notification notification){
        // TODO
        System.out.println(notification.getMessage());
    }

}
