package ypfunding.common.notifications;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Notification {
    protected NotificationType type;
    private String message;
}
