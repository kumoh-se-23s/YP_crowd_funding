package ypfunding.server.service;

import ypfunding.server.util.UserUtils;
import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.ImageDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.PermissionDeniedException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ImageDAO;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.RewardsDAO;
import ypfunding.server.util.FileUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * 리워드 등록·추가·수정 및 리워드 이미지 조회를 담당하는 서비스 (B 역할).
 * 모든 public 메서드는 첫 인자로 LoginUser를 받아 접근 권한과 프로젝트 상태를 검증한다.
 */
public class RewardService extends Service{

    private final ProjectsDAO projectsDAO;
    private final RewardsDAO rewardsDAO;
    private final ImageDAO imageDAO;

    public RewardService(
            TransactionManager transactionManager,
            ProjectsDAO projectsDAO,
            RewardsDAO rewardsDAO,
            ImageDAO imageDAO) {
        super(Objects.requireNonNull(transactionManager, "transactionManager must not be null."));
        this.projectsDAO = Objects.requireNonNull(projectsDAO, "projectsDAO must not be null.");
        this.rewardsDAO = Objects.requireNonNull(rewardsDAO, "rewardsDAO must not be null.");
        this.imageDAO = Objects.requireNonNull(imageDAO, "imageDAO must not be null.");
    }

    // =================================================================
    // 1. PUBLIC 비즈니스 메서드
    // =================================================================

    /**
     * 특정 프로젝트 아래에 리워드 목록(최소 2개)을 일괄 등록한다 (이미지 없는 버전).
     * (11월 2일 DB 검사 3번 항목 test03RegisterReward 전용)
     */
    public void createRewards(
            LoginUser loginUser,
            long projectId,
            List<RewardDTO> rewards) {
        createRewards(loginUser, projectId, rewards, Collections.emptyMap());
    }

    /**
     * 특정 프로젝트 아래에 리워드 목록(최소 2개)과 선택적 이미지를 일괄 등록한다.
     */
    public void createRewards(
            LoginUser loginUser,
            long projectId,
            List<RewardDTO> rewards,
            Map<Integer, ImageDTO> rewardImagesByIndex) {
        UserUtils.assertMaker(loginUser);
        validateProjectId(projectId);
        validateRewardList(rewards);
        Map<Integer, ImageDTO> sanitizedImages = sanitizeRewardImages(rewards.size(), rewardImagesByIndex);

        transactionManager.execute(conn -> {
            ProjectDTO project = findProjectOrThrow(conn, projectId);
            validateRewardEditableState(loginUser, project);

            for (int i = 0; i < rewards.size(); i++) {
                insertSingleRewardWithImage(conn, projectId, rewards.get(i), sanitizedImages.get(i));
            }
            return null;
        });
    }

    /**
     * 특정 프로젝트 아래에 단일 리워드 1개를 추가 등록한다 (이미지 없는 버전).
     */
    public long createReward(
            LoginUser loginUser,
            long projectId,
            RewardDTO rewardDTO) {
        return createReward(loginUser, projectId, rewardDTO, null);
    }

    /**
     * 특정 프로젝트 아래에 단일 리워드 1개와 선택적 이미지를 추가 등록한다.
     */
    public long createReward(
            LoginUser loginUser,
            long projectId,
            RewardDTO rewardDTO,
            ImageDTO imageDTO) {
        UserUtils.assertMaker(loginUser);
        validateProjectId(projectId);
        validateSingleReward(rewardDTO);
        ImageDTO sanitizedImage = sanitizeSingleImage(imageDTO);

        return transactionManager.execute(conn -> {
            ProjectDTO project = findProjectOrThrow(conn, projectId);
            validateRewardEditableState(loginUser, project);

            return insertSingleRewardWithImage(conn, projectId, rewardDTO, sanitizedImage);
        });
    }

    public List<RewardDTO> getRewardsByProject(LoginUser loginUser, long projectId) {
        requireLoginUser(loginUser);
        validateProjectId(projectId);

        return transactionManager.execute(conn -> {
            validateProjectViewAccess(conn, loginUser, projectId);
            return rewardsDAO.findByProjectId(conn, projectId);
        });
    }

    public RewardDTO getReward(LoginUser loginUser, long rewardId) {
        requireLoginUser(loginUser);
        validateRewardId(rewardId);

        return transactionManager.execute(conn -> {
            RewardDTO reward = findRewardOrThrow(conn, rewardId);
            validateProjectViewAccess(conn, loginUser, reward.getProjectId());
            return reward;
        });
    }

    public ImageDTO getRewardImageMetadata(LoginUser loginUser, long rewardId) {
        requireLoginUser(loginUser);
        validateRewardId(rewardId);

        return transactionManager.execute(conn -> {
            RewardDTO reward = findRewardOrThrow(conn, rewardId);
            validateProjectViewAccess(conn, loginUser, reward.getProjectId());
            return imageDAO.findMetadataByRewardId(conn, rewardId);
        });
    }

    public ImageDTO getRewardImage(LoginUser loginUser, long rewardId) {
        requireLoginUser(loginUser);
        validateRewardId(rewardId);

        return transactionManager.execute(conn -> {
            RewardDTO reward = findRewardOrThrow(conn, rewardId);
            validateProjectViewAccess(conn, loginUser, reward.getProjectId());
            return imageDAO.findByRewardId(conn, rewardId);
        });
    }

    public void updateReward(LoginUser loginUser, RewardDTO rewardDTO) {
        UserUtils.assertMaker(loginUser);
        validateSingleReward(rewardDTO);
        validateRewardId(rewardDTO.getRewardId());

        transactionManager.execute(conn -> {
            RewardDTO existingReward = findRewardOrThrow(conn, rewardDTO.getRewardId());
            ProjectDTO project = findProjectOrThrow(conn, existingReward.getProjectId());
            validateRewardEditableState(loginUser, project);

            rewardDTO.setProjectId(existingReward.getProjectId());
            rewardDTO.setName(rewardDTO.getName().trim());
            rewardDTO.setDescription(rewardDTO.getDescription().trim());

            rewardsDAO.update(conn, rewardDTO);
            return null;
        });
    }

    // =================================================================
    // 4. 권한 및 입력값 검증 메서드 (Validation)
    // =================================================================

    private void requireLoginUser(LoginUser loginUser) {
        if (loginUser == null) {
            throw new PermissionDeniedException("로그인이 필요한 기능입니다.");
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

    private void validateRewardList(List<RewardDTO> rewards) {
        if (rewards == null || rewards.size() < DomainRules.MIN_REWARDS_PER_PROJECT) {
            throw new BusinessException("리워드는 최소 " + DomainRules.MIN_REWARDS_PER_PROJECT + "개 이상 등록해야 합니다.");
        }
        for (RewardDTO reward : rewards) {
            validateSingleReward(reward);
        }
    }

    private Map<Integer, ImageDTO> sanitizeRewardImages(int rewardCount, Map<Integer, ImageDTO> rawImages) {
        if (rawImages == null || rawImages.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ImageDTO> sanitized = new HashMap<>();
        for (Map.Entry<Integer, ImageDTO> entry : rawImages.entrySet()) {
            Integer index = entry.getKey();
            ImageDTO raw = entry.getValue();
            if (raw == null) {
                continue;
            }
            if (index == null || index < 0 || index >= rewardCount) {
                throw new BusinessException("유효하지 않은 리워드 인덱스에 이미지가 지정되었습니다: " + index);
            }
            sanitized.put(index, sanitizeSingleImage(raw));
        }
        return sanitized;
    }

    private void validateSingleReward(RewardDTO reward) {
        if (reward == null) {
            throw new BusinessException("리워드 정보가 비어 있습니다.");
        }
        if (reward.getName() == null || reward.getName().isBlank()) {
            throw new BusinessException("리워드 이름을 입력해 주세요.");
        }
        if (reward.getName().trim().length() > DomainRules.MAX_REWARD_NAME_LENGTH) {
            throw new BusinessException("리워드 이름은 " + DomainRules.MAX_REWARD_NAME_LENGTH + "자를 초과할 수 없습니다.");
        }
        if (reward.getPrice() == null || reward.getPrice() <= 0) {
            throw new BusinessException("리워드 가격은 1원 이상이어야 합니다.");
        }
        if (reward.getDescription() == null || reward.getDescription().isBlank()) {
            throw new BusinessException("리워드 설명을 입력해 주세요.");
        }
        if (reward.getStock() != null && reward.getStock() < 0) {
            throw new BusinessException("리워드 재고 수량은 0 이상이어야 합니다. (무제한은 null)");
        }
    }

    private void validateProjectViewPermission(LoginUser loginUser, ProjectDTO project) {
        if (project.getApprovalStatus() == ApprovalStatus.APPROVED || !UserUtils.checkNotAdmin(loginUser)) {
            return;
        }
        if (project.getUserId() != loginUser.userId()) {
            throw new PermissionDeniedException("승인되지 않은 프로젝트의 리워드는 조회할 수 없습니다.");
        }
    }

    private void validateRewardEditableState(LoginUser loginUser, ProjectDTO project) {
        if (project.getUserId() != loginUser.userId()) {
            throw new PermissionDeniedException("본인이 등록한 프로젝트의 리워드만 추가하거나 수정할 수 있습니다.");
        }
        if (project.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new BusinessException("승인된(APPROVED) 프로젝트의 리워드는 추가하거나 수정할 수 없습니다.");
        }
    }

    /**
     * 단일 이미지 DTO를 검증·정제
     */
    private ImageDTO sanitizeSingleImage(ImageDTO raw) {
        if (raw == null) {
            return null;
        }
        String cleanFileName = validateAndExtractFileName(raw.getName());
        byte[] imageBytes = validateAndExtractImageBytes(raw.getImageData());
        String detectedMimeType = FileUtil.detectImageMimeType(imageBytes);

        if (detectedMimeType == null) {
            throw new BusinessException("지원하지 않는 이미지 형식입니다. (PNG, JPEG, GIF, WEBP, BMP만 허용)");
        }

        ImageDTO cleanDto = new ImageDTO();
        cleanDto.setName(cleanFileName);
        cleanDto.setType(detectedMimeType);
        cleanDto.setImageData(imageBytes); // SerialBlob 없이 순수 byte[] 그대로 세팅!
        return cleanDto;
    }

    private String validateAndExtractFileName(String rawPath) {
        String fileName = FileUtil.extractFileName(rawPath);
        if (fileName.isBlank() || fileName.equals(".") || fileName.equals("..")) {
            throw new BusinessException("올바르지 않은 이미지 파일명입니다.");
        }
        if (fileName.length() > DomainRules.MAX_IMAGE_NAME_LENGTH) {
            throw new BusinessException("이미지 파일명은 " + DomainRules.MAX_IMAGE_NAME_LENGTH + "자를 초과할 수 없습니다.");
        }
        return fileName;
    }

    private byte[] validateAndExtractImageBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("이미지 데이터가 비어 있습니다.");
        }
        if (bytes.length > DomainRules.MAX_IMAGE_BYTES) {
            throw new BusinessException("이미지 크기는 최대 " + (DomainRules.MAX_IMAGE_BYTES / (1024 * 1024)) + "MB를 초과할 수 없습니다.");
        }
        return bytes;
    }

    // =================================================================
    // 5. 트랜잭션 내부 DB 작업 헬퍼 (Persistence Helpers)
    // =================================================================

    private long insertSingleRewardWithImage(
            Connection conn,
            long projectId,
            RewardDTO reward,
            ImageDTO sanitizedImage) throws SQLException {
        reward.setProjectId(projectId);
        reward.setName(reward.getName().trim());
        reward.setDescription(reward.getDescription().trim());

        rewardsDAO.insert(conn, reward);
        long rewardId = reward.getRewardId();

        if (sanitizedImage != null) {
            sanitizedImage.setRewardId(rewardId);
            imageDAO.insert(conn, sanitizedImage);
        }
        return rewardId;
    }

    private RewardDTO findRewardOrThrow(Connection conn, long rewardId) throws SQLException {
        RewardDTO reward = rewardsDAO.findById(conn, rewardId);
        if (reward == null) {
            throw new BusinessException("존재하지 않는 리워드입니다.");
        }
        return reward;
    }

    private ProjectDTO findProjectOrThrow(Connection conn, long projectId) throws SQLException {
        ProjectDTO project = projectsDAO.findById(conn, projectId);
        if (project == null) {
            throw new BusinessException("소속 프로젝트를 찾을 수 없습니다.");
        }
        return project;
    }

    private void validateProjectViewAccess(Connection conn, LoginUser loginUser, long projectId) throws SQLException {
        ProjectDTO project = findProjectOrThrow(conn, projectId);
        validateProjectViewPermission(loginUser, project);
    }
}