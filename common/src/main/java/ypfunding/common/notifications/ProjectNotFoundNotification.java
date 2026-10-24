package ypfunding.common.notifications;

public class ProjectNotFoundNotification extends Notification {

    private final long projectID;
    public ProjectNotFoundNotification(long projectID) {
        type = NotificationType.ERROR;
        this.projectID = projectID;
    }

    @Override
    public String getMessage() {
        return "Project " + projectID + " not found.";
    }
}
