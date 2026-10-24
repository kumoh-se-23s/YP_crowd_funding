package ypfunding.common.notifications;

import lombok.Getter;
import lombok.Setter;

public class CompleteNotification extends Notification{
    
    @Setter
    private String message;

    public static CompleteNotification from(Notification notification) {
        CompleteNotification result = new CompleteNotification();
        result.setType(notification.getType());
        result.setMessage(notification.getMessage());
        return result;
    }
    @Override
    public String getMessage() {
        return message;
    }
}
