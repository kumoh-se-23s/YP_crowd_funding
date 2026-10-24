package ypfunding.common.notifications;

public enum NotificationType {
    INFO("Info"),
    WARNING("Warning"),
    ERROR("Error");

    private final String message;

    NotificationType(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
