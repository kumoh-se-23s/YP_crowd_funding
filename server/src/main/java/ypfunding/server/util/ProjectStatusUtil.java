package ypfunding.server.util;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.type.ApprovalStatus;

import java.time.LocalDateTime;

public final class ProjectStatusUtil {
    private ProjectStatusUtil(){}

    //진행 중인지: 승인, 시작 시각 이후, 종료 정각 전
    public static boolean isOngoing(ProjectDTO project, LocalDateTime now) {
        return project.getApprovalStatus() == ApprovalStatus.APPROVED
                && !now.isBefore(project.getStartDate())
                && now.isBefore(project.getEndDate());
    }
}
