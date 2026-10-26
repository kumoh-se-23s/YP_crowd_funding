package ypfunding.server.service;

import ypfunding.common.dto.ImageDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ImageDAO;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.RewardsDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class RewardService {

    private final TransactionManager tx;
    private final ProjectsDAO projectsDAO;
    private final RewardsDAO rewardsDAO;
    private final ImageDAO imageDAO;

    public RewardService(
            TransactionManager tx,
            ProjectsDAO projectsDAO,
            RewardsDAO rewardsDAO,
            ImageDAO imageDAO) {
        this.tx = Objects.requireNonNull(tx, "transactionManager must not be null.");
        this.projectsDAO = Objects.requireNonNull(projectsDAO, "projectsDAO must not be null.");
        this.rewardsDAO = Objects.requireNonNull(rewardsDAO, "rewardsDAO must not be null.");
        this.imageDAO = Objects.requireNonNull(imageDAO, "imageDAO must not be null.");
    }

    // =================================================================
    // 1. PUBLIC 비즈니스 메서드
    // =================================================================

    /**
     * 특정 프로젝트에 속한 리워드 목록을 조회
     *
     * @param loginUser 요청한 로그인 사용자
     * @param projectId 조회할 프로젝트 ID
     * @return 리워드 DTO 목록 (STOCK이 null이면 무제한, 0이면 품절)
     */
    public List<RewardDTO> getRewardsByProject(LoginUser loginUser, long projectId) {
        requireLoginUser(loginUser);
        validateProjectId(projectId);

        return tx.execute(conn -> {
            validateProjectViewAccess(conn, loginUser, projectId);
            return rewardsDAO.findByProjectId(conn, projectId);
        });
    }

    /**
     * 특정 리워드의 상세 정보를 단건 조회
     *
     * @param loginUser 요청한 로그인 사용자
     * @param rewardId  조회할 리워드 ID
     * @return 리워드 DTO
     */
    public RewardDTO getReward(LoginUser loginUser, long rewardId) {
        requireLoginUser(loginUser);
        validateRewardId(rewardId);

        return tx.execute(conn -> {
            RewardDTO reward = findRewardOrThrow(conn, rewardId);
            validateProjectViewAccess(conn, loginUser, reward.getProjectId());
            return reward;
        });
    }

    /**
     * 특정 리워드의 이미지 메타데이터(파일명, MIME 타입)만 가볍게 조회 (IMAGE_DATA BLOB 제외).
     *
     * @param loginUser 요청한 로그인 사용자
     * @param rewardId  리워드 ID
     * @return IMAGE_DATA가 비어있는 ImageDTO (이미지가 없으면 null)
     */
    public ImageDTO getRewardImageMetadata(LoginUser loginUser, long rewardId) {
        requireLoginUser(loginUser);
        validateRewardId(rewardId);

        return tx.execute(conn -> {
            RewardDTO reward = findRewardOrThrow(conn, rewardId);
            validateProjectViewAccess(conn, loginUser, reward.getProjectId());
            return imageDAO.findMetadataByRewardId(conn, rewardId);
        });
    }

    /**
     * 특정 리워드에 첨부된 이미지 전체(IMAGE_DATA BLOB 포함)를 조회
     * 반환된 ImageDTO의 imageData는 SerialBlob으로 감싸져 있어 트랜잭션 종료 후에도 바이트를 읽을 수 있음
     *
     * @param loginUser 요청한 로그인 사용자
     * @param rewardId  리워드 ID
     * @return 이미지 데이터가 담긴 ImageDTO (이미지가 없으면 null)
     */
    public ImageDTO getRewardImage(LoginUser loginUser, long rewardId) {
        requireLoginUser(loginUser);
        validateRewardId(rewardId);

        return tx.execute(conn -> {
            RewardDTO reward = findRewardOrThrow(conn, rewardId);
            validateProjectViewAccess(conn, loginUser, reward.getProjectId());
            return imageDAO.findByRewardId(conn, rewardId);
        });
    }

    // =================================================================
    // 2. 권한 및 입력값 검증 메서드 (Validation)
    // =================================================================

    private void requireLoginUser(LoginUser loginUser) {
        if (loginUser == null) {
            throw new NotAllowedException("로그인이 필요한 기능입니다.");
        }
    }

    private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException("올바르지 않은 프로젝트 ID입니다.");
        }
    }

    private void validateRewardId(Long rewardId) {
        if (rewardId == null || rewardId <= 0) {
            throw new BusinessException("올바르지 않은 리워드 ID입니다.");
        }
    }

    private void validateProjectViewPermission(LoginUser loginUser, ProjectDTO project) {
        if (project.getApprovalStatus() == ApprovalStatus.APPROVED) {
            return;
        }
        boolean isOwnerMaker = loginUser.userType() == UserType.MAKER
                && Objects.equals(project.getUserId(), loginUser.userId());
        boolean isAdmin = loginUser.userType() == UserType.ADMIN;
        if (!isOwnerMaker && !isAdmin) {
            throw new NotAllowedException("승인되지 않은 프로젝트의 리워드는 조회할 수 없습니다.");
        }
    }

    // =================================================================
    // 3. 트랜잭션 내부 DB 조회 헬퍼 (Persistence Helpers)
    // =================================================================

    private RewardDTO findRewardOrThrow(Connection conn, long rewardId) throws SQLException {
        RewardDTO reward = rewardsDAO.findById(conn, rewardId);
        if (reward == null) {
            throw new BusinessException("존재하지 않는 리워드입니다.");
        }
        return reward;
    }

    private void validateProjectViewAccess(Connection conn, LoginUser loginUser, long projectId) throws SQLException {
        ProjectDTO project = projectsDAO.findById(conn, projectId);
        if (project == null) {
            throw new BusinessException("소속 프로젝트를 찾을 수 없습니다.");
        }
        validateProjectViewPermission(loginUser, project);
    }
}