package ypfunding.common.notifications;

public class PermissionDeniedNotification extends Notification {

    public PermissionDeniedNotification() {
        type = NotificationType.ERROR;
    }

    @Override
    public String getMessage() {
        return "Permission Denied. You are not an Administrator.";
    }
}
