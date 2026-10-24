package ypfunding.common.notifications;

import lombok.Getter;
import lombok.Setter;

public abstract class Notification {

    @Getter
    @Setter
    protected NotificationType type;

    public abstract String getMessage();
}
