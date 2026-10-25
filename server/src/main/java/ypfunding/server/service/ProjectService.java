package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.CategoryDTO;
import ypfunding.common.dto.ImageDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.ProjectDetailDTO;
import ypfunding.common.dto.ReviewDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.CategoryDAO;
import ypfunding.server.persistence.dao.ImageDAO;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.ReviewsDAO;
import ypfunding.server.persistence.dao.RewardsDAO;
import ypfunding.server.util.FileUtil;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProjectService {

    private final TransactionManager tx;
    private final ProjectsDAO projectsDAO;
    private final CategoryDAO categoryDAO;
    private final RewardsDAO rewardsDAO;
    private final ImageDAO imageDAO;
    private final ReviewsDAO reviewsDAO;

    public ProjectService(
            TransactionManager tx,
            ProjectsDAO projectsDAO,
            CategoryDAO categoryDAO,
            RewardsDAO rewardsDAO,
            ImageDAO imageDAO,
            ReviewsDAO reviewsDAO) {
        //Objects.requireNonNull -> 널 체크를 위한 메소드, 빠른 실패와 가독성 등 여러 부분에 있어 디버깅에 용이
        this.tx = Objects.requireNonNull(tx, "transactionManager must not be null");
        this.projectsDAO = Objects.requireNonNull(projectsDAO, "projectsDAO must not be null");
        this.categoryDAO = Objects.requireNonNull(categoryDAO, "categoryDAO must not be null");
        this.rewardsDAO = Objects.requireNonNull(rewardsDAO, "rewardsDAO must not be null");
        this.imageDAO = Objects.requireNonNull(imageDAO, "imageDAO must not be null");
        this.reviewsDAO = Objects.requireNonNull(reviewsDAO, "reviewsDAO must not be null");
    }

    // =================================================================
    // 1. PUBLIC 비즈니스 메서드
    // =================================================================

    /**
     * 프로젝트 기본 정보, 카테고리, 리워드를 한 번에 받아 단일 트랜잭션으로 신청
     *
     * @param loginUser  요청한 로그인 사용자 (MAKER만 허용)
     * @param projectDTO 프로젝트 기본 정보
     * @param categories 선택한 카테고리 목록
     * @param rewards    등록할 리워드 목록 (최소 2개)
     * @return 생성된 프로젝트 ID
     */
    public long createProject(
            LoginUser loginUser,
            ProjectDTO projectDTO,
            List<Category> categories,
            List<RewardDTO> rewards) {
        return createProject(loginUser, projectDTO, categories, rewards, Collections.emptyMap());
    }

    /**
     * 프로젝트 기본 정보, 카테고리, 리워드 및 선택적 이미지를 한 트랜잭션으로 저장하고 즉시 PENDING 상태로 신청
     *
     * @param loginUser           요청한 로그인 사용자 (MAKER만 허용)
     * @param projectDTO          프로젝트 기본 정보
     * @param categories          프로젝트 카테고리 목록
     * @param rewards             프로젝트 리워드 목록 (최소 2개)
     * @param rewardImagesByIndex 리워드 인덱스(0부터 시작)별 선택적 이미지 맵
     * @return 생성된 프로젝트 ID
     */
    public long createProject(
            LoginUser loginUser,
            ProjectDTO projectDTO,
            List<Category> categories,
            List<RewardDTO> rewards,
            Map<Integer, ImageDTO> rewardImagesByIndex) {
        requireMaker(loginUser);
        validateProjectBundleInput(projectDTO, categories, rewards);
        Map<Integer, ImageDTO> sanitizedImages = sanitizeRewardImages(rewards.size(), rewardImagesByIndex);

        return tx.execute(conn -> {
            ProjectDTO toInsert = buildNewPendingProject(loginUser.userId(), projectDTO);
            projectsDAO.insert(conn, toInsert);
            long projectId = toInsert.getProjectId();

            insertCategories(conn, projectId, categories);
            insertRewardsWithImages(conn, projectId, rewards, sanitizedImages);
            return projectId;
        });
    }

    /**
     * 반려된 프로젝트를 수정하여 다시 신청
     *
     * @param loginUser  요청한 로그인 사용자 (본인 소유 MAKER만 허용)
     * @param projectDTO 수정할 프로젝트 기본 정보 (projectId 필수)
     * @param categories 교체할 카테고리 목록
     * @param rewards    교체할 리워드 목록 (최소 2개)
     */
    public void resubmitRejectedProject(
            LoginUser loginUser,
            ProjectDTO projectDTO,
            List<Category> categories,
            List<RewardDTO> rewards) {
        resubmitRejectedProject(loginUser, projectDTO, categories, rewards, Collections.emptyMap());
    }

    /**
     * 반려된 프로젝트를 수정하여 다시 신청(이미지)
     *
     * @param loginUser           요청한 로그인 사용자 (본인 소유 MAKER만 허용)
     * @param projectDTO          수정할 프로젝트 정보 (projectId 필수)
     * @param categories          새로 설정할 카테고리 목록
     * @param rewards             새로 설정할 리워드 목록 (최소 2개)
     * @param rewardImagesByIndex 리워드 인덱스별 선택적 이미지 맵
     */
    public void resubmitRejectedProject(
            LoginUser loginUser,
            ProjectDTO projectDTO,
            List<Category> categories,
            List<RewardDTO> rewards,
            Map<Integer, ImageDTO> rewardImagesByIndex) {
        requireMaker(loginUser);
        validateProjectIdPresent(projectDTO);
        validateProjectBundleInput(projectDTO, categories, rewards);
        Map<Integer, ImageDTO> sanitizedImages = sanitizeRewardImages(rewards.size(), rewardImagesByIndex);

        long projectId = projectDTO.getProjectId();

        tx.execute(conn -> {
            ProjectDTO existing = findProjectOrThrow(conn, projectId);
            validateResubmitPermissionAndState(loginUser, existing);

            updateRejectedProjectToPending(conn, loginUser.userId(), projectDTO);
            deleteExistingProjectChildren(conn, projectId);
            insertCategories(conn, projectId, categories);
            insertRewardsWithImages(conn, projectId, rewards, sanitizedImages);
            return null;
        });
    }

    /**
     * 프로젝트 상세 정보를 조회
     * 서포터에게는 승인된 프로젝트만 보이며, 메이커 본인과 관리자는 신청/반려 프로젝트도 조회 가능
     *
     * @param loginUser 요청한 로그인 사용자
     * @param projectId 조회할 프로젝트 ID
     * @return 프로젝트 상세 정보 DTO
     */
    public ProjectDetailDTO getProjectDetail(LoginUser loginUser, long projectId) {
        requireLoginUser(loginUser);
        validateProjectId(projectId);

        return tx.execute(conn -> {
            ProjectDTO project = findProjectOrThrow(conn, projectId);
            validateProjectViewPermission(loginUser, project);

            List<Category> categories = findCategoryTypes(conn, projectId);
            List<RewardDTO> rewards = rewardsDAO.findByProjectId(conn, projectId);
            List<Long> rewardIdsWithImage = findRewardIdsWithImage(conn, rewards);
            List<ReviewDTO> reviews = reviewsDAO.findAll(conn, projectId);

            return assembleProjectDetail(project, categories, rewards, rewardIdsWithImage, reviews);
        });
    }

    /**
     * 특정 프로젝트의 리뷰 목록을 최신순으로 조회한다.
     *
     * @param loginUser 요청한 로그인 사용자
     * @param projectId 조회할 프로젝트 ID
     * @return 최신순 리뷰 DTO 목록
     */
    public List<ReviewDTO> getProjectReviews(LoginUser loginUser, long projectId) {
        requireLoginUser(loginUser);
        validateProjectId(projectId);

        return tx.execute(conn -> {
            ProjectDTO project = findProjectOrThrow(conn, projectId);
            validateProjectViewPermission(loginUser, project);
            return reviewsDAO.findAll(conn, projectId);
        });
    }

    /**
     * 메이커 본인이 등록한 모든 프로젝트 목록을 최신순으로 조회
     *
     * @param loginUser 요청한 로그인 사용자 (MAKER만 허용)
     * @return 본인이 등록한 프로젝트 목록
     */
    public List<ProjectDTO> getMyProjects(LoginUser loginUser) {
        requireMaker(loginUser);
        return tx.execute(conn -> projectsDAO.findByUserId(conn, loginUser.userId()));
    }

    // =================================================================
    // 2. 권한 및 상태 검증 메서드 (Auth & State Validation)
    // =================================================================

    private void requireLoginUser(LoginUser loginUser) {
        if (loginUser == null) {
            throw new NotAllowedException("로그인이 필요한 기능입니다.");
        }
    }

    private void requireMaker(LoginUser loginUser) {
        requireLoginUser(loginUser);
        if (loginUser.userType() != UserType.MAKER) {
            throw new NotAllowedException("메이커만 이용할 수 있는 기능입니다.");
        }
    }

    /**
     * 프로젝트 열람 권한을 검증
     * 승인 전(PENDING, REJECTED) 프로젝트는 작성 메이커 본인 또는 관리자만 열람 가능
     */
    private void validateProjectViewPermission(LoginUser loginUser, ProjectDTO project) {
        if (project.getApprovalStatus() == ApprovalStatus.APPROVED) {
            return;
        }
        boolean isOwnerMaker = loginUser.userType() == UserType.MAKER
                && Objects.equals(project.getUserId(), loginUser.userId());
        boolean isAdmin = loginUser.userType() == UserType.ADMIN;
        if (!isOwnerMaker && !isAdmin) {
            throw new NotAllowedException("승인되지 않은 프로젝트는 조회할 수 없습니다.");
        }
    }

    /**
     * 반려 프로젝트 수정·재신청 시 본인 소유 여부와 REJECTED 상태를 검증
     */
    private void validateResubmitPermissionAndState(LoginUser loginUser, ProjectDTO existing) {
        if (!Objects.equals(existing.getUserId(), loginUser.userId())) {
            throw new NotAllowedException("본인이 등록한 프로젝트만 수정할 수 있습니다.");
        }
        if (existing.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new BusinessException("반려(REJECTED) 상태인 프로젝트만 수정 및 재신청할 수 있습니다.");
        }
    }

    // =================================================================
    // 3. 입력값 검증 메서드 (Input Validation)
    // =================================================================

    private void validateProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException("올바르지 않은 프로젝트 ID입니다.");
        }
    }

    private void validateProjectIdPresent(ProjectDTO projectDTO) {
        if (projectDTO == null || projectDTO.getProjectId() == null || projectDTO.getProjectId() <= 0) {
            throw new BusinessException("수정할 프로젝트 ID가 올바르지 않습니다.");
        }
    }

    private void validateProjectBundleInput(
            ProjectDTO projectDTO,
            List<Category> categories,
            List<RewardDTO> rewards) {
        validateProjectFields(projectDTO);
        validateCategories(categories);
        validateRewards(rewards);
    }

    private void validateProjectFields(ProjectDTO projectDTO) {
        if (projectDTO == null) {
            throw new BusinessException("프로젝트 정보가 비어 있습니다.");
        }
        if (projectDTO.getTitle() == null || projectDTO.getTitle().isBlank()) {
            throw new BusinessException("프로젝트 제목을 입력해 주세요.");
        }
        if (projectDTO.getTitle().trim().length() > DomainRules.MAX_PROJECT_TITLE_LENGTH) {
            throw new BusinessException("프로젝트 제목은 " + DomainRules.MAX_PROJECT_TITLE_LENGTH + "자를 초과할 수 없습니다.");
        }
        if (projectDTO.getDescription() == null || projectDTO.getDescription().isBlank()) {
            throw new BusinessException("프로젝트 설명을 입력해 주세요.");
        }
        if (projectDTO.getGoal() == null || projectDTO.getGoal() <= 0) {
            throw new BusinessException("목표 금액은 1원 이상이어야 합니다.");
        }
        if (projectDTO.getDuration() == null || projectDTO.getDuration() <= 0) {
            throw new BusinessException("펀딩 기간(일)은 1일 이상이어야 합니다.");
        }
    }

    private void validateCategories(List<Category> categories) {
        if (categories == null
                || categories.size() < DomainRules.MIN_CATEGORIES_PER_PROJECT
                || categories.size() > DomainRules.MAX_CATEGORIES_PER_PROJECT) {
            throw new BusinessException(String.format("카테고리는 %d개 이상 %d개 이하로 선택해야 합니다.",
                    DomainRules.MIN_CATEGORIES_PER_PROJECT, DomainRules.MAX_CATEGORIES_PER_PROJECT));
        }

        Set<Category> uniqueCategories = new HashSet<>();
        for (Category category : categories) {
            if (category == null) {
                throw new BusinessException("유효하지 않은 카테고리가 포함되어 있습니다.");
            }
            if (!uniqueCategories.add(category)) {
                throw new BusinessException("중복된 카테고리를 선택할 수 없습니다: " + category);
            }
        }
    }

    private void validateRewards(List<RewardDTO> rewards) {
        if (rewards == null || rewards.size() < DomainRules.MIN_REWARDS_PER_PROJECT) {
            throw new BusinessException("리워드는 최소 " + DomainRules.MIN_REWARDS_PER_PROJECT + "개 이상 등록해야 합니다.");
        }
        for (RewardDTO reward : rewards) {
            validateSingleReward(reward);
        }
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

    // =================================================================
    // 4. 데이터 정제 및 DTO 조립 메서드 (Sanitization & Mapping)
    // =================================================================

    private ProjectDTO buildNewPendingProject(long makerUserId, ProjectDTO input) {
        ProjectDTO toInsert = new ProjectDTO();
        toInsert.setUserId(makerUserId);
        toInsert.setTitle(input.getTitle().trim());
        toInsert.setDescription(input.getDescription().trim());
        toInsert.setGoal(input.getGoal());
        toInsert.setDuration(input.getDuration());
        toInsert.setStartDate(null);
        toInsert.setApprovalStatus(ApprovalStatus.PENDING);
        return toInsert;
    }

    private RewardDTO buildRewardToInsert(long projectId, RewardDTO input) {
        RewardDTO toInsert = new RewardDTO();
        toInsert.setProjectId(projectId);
        toInsert.setName(input.getName().trim());
        toInsert.setPrice(input.getPrice());
        toInsert.setDescription(input.getDescription().trim());
        toInsert.setStock(input.getStock());
        return toInsert;
    }

    //List를 쓰면 순회를 돌아야해서 이미지 데이터의 접근 효율이 떨어짐
    //map을 쓰면 Integer(rewardId)로 바로 접근하여 이미지 데이터를 가질 수 있음
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

    //정제된 형태의 이미지 DTO로 변환하는 역할
    private ImageDTO sanitizeSingleImage(ImageDTO raw) {
        String cleanFileName = FileUtil.extractFileName(raw.getName());
        byte[] imageBytes = FileUtil.extractAndValidateBytes(raw.getImageData());
        String detectedMimeType = FileUtil.detectImageMimeType(imageBytes);

        try {
            ImageDTO cleanDto = new ImageDTO();
            cleanDto.setName(cleanFileName);
            cleanDto.setType(detectedMimeType);
            cleanDto.setImageData(new SerialBlob(imageBytes));
            return cleanDto;
        } catch (SQLException e) {
            throw new BusinessException("이미지 데이터 변환 중 오류가 발생했습니다.", e);
        }
    }

    private ProjectDetailDTO assembleProjectDetail(
            ProjectDTO project,
            List<Category> categories,
            List<RewardDTO> rewards,
            List<Long> rewardIdsWithImage,
            List<ReviewDTO> reviews) {
        ProjectDetailDTO detailDTO = new ProjectDetailDTO();
        detailDTO.setProject(project);
        detailDTO.setCategories(categories);
        detailDTO.setRewards(rewards);
        detailDTO.setRewardIdsWithImage(rewardIdsWithImage);
        detailDTO.setReviews(reviews);
        return detailDTO;
    }

    // =================================================================
    // 5. 트랜잭션 내부 DB 작업 헬퍼 (Persistence Helpers)
    // =================================================================

    private ProjectDTO findProjectOrThrow(Connection conn, long projectId) throws SQLException {
        ProjectDTO project = projectsDAO.findById(conn, projectId);
        if (project == null) {
            throw new BusinessException("존재하지 않는 프로젝트입니다.");
        }
        return project;
    }

    private void updateRejectedProjectToPending(Connection conn, long makerUserId, ProjectDTO input) throws SQLException {
        ProjectDTO toUpdate = new ProjectDTO();
        toUpdate.setProjectId(input.getProjectId());
        toUpdate.setUserId(makerUserId);
        toUpdate.setTitle(input.getTitle().trim());
        toUpdate.setDescription(input.getDescription().trim());
        toUpdate.setGoal(input.getGoal());
        toUpdate.setDuration(input.getDuration());

        int updatedRows = projectsDAO.resubmitRejectedProjectUpdate(conn, toUpdate);
        if (updatedRows == 0) {
            throw new BusinessException("프로젝트 상태가 변경되어 재신청할 수 없습니다.");
        }
    }

    /**
     * 반려 프로젝트 수정 시 기존 하위 데이터를 삭제한다.
     * 반드시 IMAGE -> REWARDS)-> CATEGORY 순서로 삭제
     */
    private void deleteExistingProjectChildren(Connection conn, long projectId) throws SQLException {
        List<RewardDTO> existingRewards = rewardsDAO.findByProjectId(conn, projectId);
        //프로젝트 아래 여러 reward의 각 이미지 먼저 삭제
        for (RewardDTO reward : existingRewards) {
            imageDAO.deleteByRewardId(conn, reward.getRewardId());
        }
        rewardsDAO.deleteByProjectId(conn, projectId);
        categoryDAO.deleteByProjectId(conn, projectId);
    }

    private void insertCategories(Connection conn, long projectId, List<Category> categories) throws SQLException {
        for (Category category : categories) {
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setProjectId(projectId);
            categoryDTO.setCategoryName(category);
            categoryDAO.insert(conn, categoryDTO);
        }
    }

    private void insertRewardsWithImages(
            Connection conn,
            long projectId,
            List<RewardDTO> rewards,
            Map<Integer, ImageDTO> sanitizedImages) throws SQLException {
        for (int i = 0; i < rewards.size(); i++) {
            RewardDTO rewardToInsert = buildRewardToInsert(projectId, rewards.get(i));
            rewardsDAO.insert(conn, rewardToInsert);

            ImageDTO imageDTO = sanitizedImages.get(i);
            if (imageDTO != null) {
                imageDTO.setRewardId(rewardToInsert.getRewardId());
                imageDAO.insert(conn, imageDTO);
            }
        }
    }

    private List<Category> findCategoryTypes(Connection conn, long projectId) throws SQLException {
        List<CategoryDTO> categoryDTOs = categoryDAO.findByProjectId(conn, projectId);
        List<Category> categories = new ArrayList<>(categoryDTOs.size());
        for (CategoryDTO dto : categoryDTOs) {
            categories.add(dto.getCategoryName());
        }
        return categories;
    }

    private List<Long> findRewardIdsWithImage(Connection conn, List<RewardDTO> rewards) throws SQLException {
        List<Long> rewardIdsWithImage = new ArrayList<>();
        for (RewardDTO reward : rewards) {
            if (imageDAO.isExistsByRewardId(conn, reward.getRewardId())) {
                rewardIdsWithImage.add(reward.getRewardId());
            }
        }
        return rewardIdsWithImage;
    }
}