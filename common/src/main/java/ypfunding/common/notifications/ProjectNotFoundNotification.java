package ypfunding.common.notifications;

public class ProjectNotFoundNotification extends Notification {

    private final long projectId;
    public ProjectNotFoundNotification(long projectId) {
        type = NotificationType.ERROR;
        this.projectId = projectId;
    }

    @Override
    public String getMessage() {
        return "Project " + projectId + " not found.";
    }
}
