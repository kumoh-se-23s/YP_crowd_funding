package ypfunding.client.view;

import ypfunding.client.session.ClientSession;
import ypfunding.client.util.ConsoleIO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.ProjectDetailDTO;
import ypfunding.common.dto.ReviewDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.Category;
import ypfunding.common.type.UserType;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.service.FundService;
import ypfunding.server.service.LikeService;
import ypfunding.server.service.ProjectService;
import ypfunding.server.service.RewardService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * 메이커·서포터·관리자 공통으로 호출되는 프로젝트 상세 보기 화면
 * - [메이커(B) 담당 구현]: 프로젝트 상세 정보(제공시기=종료일), 리워드 목록, 최신순 리뷰 출력 + 리워드 대표 이미지 다른 로컬 디렉토리 다운로드
 * - [서포터(D) 담당 연동]: 리워드 후원 신청 및 좋아요 등록/취소 토글
 */
public class ProjectDetailCliView {

    /**
     * 같은 클라이언트 세션에서 메이커가 이미지를 업로드할 때 사용한 원본 디렉토리를 리워드 순서/이름별로 기억해 두어,
     * 다운로드 시 사용자가 엔터를 쳐도 원본 디렉토리와 다른지 자동으로 비교할 수 있게 돕는 보조 캐시.
     */
    private static Path lastUploadedSourceDir;

    private final ProjectService projectService;
    private final RewardService rewardService;
    private final FundService fundService;
    private final LikeService likeService;

    public ProjectDetailCliView(AppContext context) {
        this.projectService = context.getProjectService();
        this.rewardService = context.getRewardService();
        this.fundService = context.getFundService();
        this.likeService = context.getLikeService();
    }

    /**
     * 메이커가 대표 이미지를 업로드할 때 사용한 원본 디렉토리 저장
     *
     * @param sourceDir 원본 이미지가 위치한 디렉토리 경로
     */
    public static void recordUploadedSourceDir(Path sourceDir) {
        if (sourceDir != null) {
            lastUploadedSourceDir = sourceDir.toAbsolutePath().normalize();
        }
    }

    /**
     * 특정 프로젝트의 상세 정보, 리워드 목록, 최신순 리뷰를 출력하고 상세 작업 메뉴를 제공
     *
     * @param projectId 조회할 프로젝트 ID
     */
    public void showProjectDetail(long projectId) {
        LoginUser loginUser = ClientSession.getLoginUser();
        if (loginUser == null) {
            ConsoleIO.printlnError("로그인이 필요한 기능입니다.");
            return;
        }

        while (true) {
            ProjectDetailDTO detail;
            try {
                detail = projectService.getProjectDetail(loginUser, projectId);
            } catch (BusinessException e) {
                ConsoleIO.printlnError(e.getMessage());
                return;
            } catch (Exception e) {
                ConsoleIO.printlnError("프로젝트 상세 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
                return;
            }

            printProjectDetailContent(detail);

            System.out.println(" [상세 작업 메뉴]");
            System.out.println(" 1. 리워드 대표 이미지 다운로드 (로컬 디렉토리 저장)");
            if (loginUser.userType() == UserType.SUPPORTER) {
                System.out.println(" 2. 리워드 후원 신청 (서포터 전용 — 리워드/수량 선택)");
                System.out.println(" 3. 좋아요 등록 / 취소 토글 (서포터 전용)");
            }
            System.out.println(" 0. 이전 화면으로 돌아가기");
            ConsoleIO.printSingleLine();

            int choice = ConsoleIO.readIntInRange("선택 > ", 0, 3);
            switch (choice) {
                case 1 -> handleDownloadRewardImage(detail);
                case 2 -> {
                    // 리워드 후원
                }
                case 3 -> {
                    // 좋아요
                }
                case 0 -> {
                    return;
                }
            }
        }
    }

    /**
     * 프로젝트 기본 정보, 리워드 목록, 최신순 리뷰 목록을 명세서 포맷에 맞춰 콘솔에 출력
     */
    private void printProjectDetailContent(ProjectDetailDTO detail) {
        ProjectDTO p = detail.getProject();
        Category category = detail.getCategoryName();
        String categoryText = (category != null)
                ? category.getDisplayName() + " (" + category.name() + ")"
                : "미지정";

        String periodText = (p.getStartDate() != null && p.getEndDate() != null)
                ? String.format("%s ~ %s (%d일)",
                DateTimeUtil.formatDate(p.getStartDate()),
                DateTimeUtil.formatDate(p.getEndDate()),
                p.getDuration())
                : String.format("승인 대기 중 (펀딩 기간: %d일)", p.getDuration());

        String deliveryDateText = (detail.getRewardDeliveryDate() != null)
                ? DateTimeUtil.formatDate(detail.getRewardDeliveryDate()) + " (펀딩 종료일)"
                : "펀딩 종료일 (승인 후 확정)";

        System.out.println();
        ConsoleIO.printDoubleLine();
        System.out.printf(" [프로젝트 상세 보기] (#%d)%n", p.getProjectId());
        ConsoleIO.printDoubleLine();
        System.out.println("• 프로젝트명 : " + p.getTitle());
        System.out.println("• 카테고리 : " + categoryText);
        System.out.println("• 설명 : " + p.getDescription());
        System.out.println("• 목표 금액 : " + ConsoleIO.formatWon(p.getGoal()));
        System.out.println("• 펀딩 기간 : " + periodText);
        System.out.println("• 리워드 제공 시기 : " + deliveryDateText);
        System.out.println("• 승인 상태 : " + p.getApprovalStatus().name());

        System.out.println();

        //상세보기 리워드 파트
        System.out.println("---------------- [리워드 목록] ----------------");
        List<RewardDTO> rewards = detail.getRewards() != null ? detail.getRewards() : Collections.emptyList();
        List<Long> imageRewardIds = detail.getRewardIdsWithImage() != null
                ? detail.getRewardIdsWithImage()
                : Collections.emptyList();

        if (rewards.isEmpty()) {
            System.out.println(" 등록된 리워드가 없습니다.");
        } else {
            for (RewardDTO r : rewards) {
                boolean hasImage = imageRewardIds.contains(r.getRewardId());
                String stockText = (r.getStock() == null || r.getStock() == 0L)
                        ? "무제한(0)"
                        : r.getStock() + "개";
                System.out.printf(" [리워드 #%d] %s%n", r.getRewardId(), r.getName());
                System.out.printf(" - 가격: %s | 총 제공수량: %s | 대표이미지: %s%n",
                        ConsoleIO.formatWon(r.getPrice()),
                        stockText,
                        hasImage ? "[있음]" : "[없음]");
                System.out.printf(" - 제공내용: %s%n", r.getDescription());
            }
        }
        System.out.println();

        //상세보기 리뷰 파트
        System.out.println("---------------- [리뷰 목록 (최신순)] ----------------");
        List<ReviewDTO> reviews = detail.getReviews() != null ? detail.getReviews() : Collections.emptyList();
        if (reviews.isEmpty()) {
            System.out.println(" 아직 작성된 리뷰가 없습니다.");
        } else {
            for (ReviewDTO rev : reviews) {
                String content = (rev.getContent() != null && !rev.getContent().isBlank())
                        ? rev.getContent()
                        : "(내용 없음)";
                System.out.printf(" • ★%d | \"%s\" (%s)%n",
                        rev.getStar(),
                        content,
                        DateTimeUtil.formatDateTime(rev.getDate()));
            }
        }
        ConsoleIO.printDoubleLine();
    }

    // 리워드 대표 이미지를 원본과 다른 로컬 디렉토리에 다운로드하여 저장
    private void handleDownloadRewardImage(ProjectDetailDTO detail) {
        List<Long> imageRewardIds = detail.getRewardIdsWithImage();
        if (imageRewardIds == null || imageRewardIds.isEmpty()) {
            ConsoleIO.printlnError("이 프로젝트에는 대표 이미지가 등록된 리워드가 없습니다.");
            return;
        }

        long rewardId = ConsoleIO.readLong("• 이미지를 다운로드할 리워드 번호 입력 : ");
        if (!imageRewardIds.contains(rewardId)) {
            ConsoleIO.printlnError("해당 리워드(#" + rewardId + ")에는 등록된 대표 이미지가 없습니다.");
            return;
        }

        Path sourceDir = ConsoleIO.readPath("• 원본 이미지 디렉토리 경로 확인 (비교용, 모르면 엔터) : ", true);
        if (sourceDir == null) {
            // 사용자가 엔터를 친 경우, 최근 업로드된 원본 폴더가 있으면 그것을 비교 기준으로 사용하고 없으면 현재 작업 폴더를 사용
            sourceDir = (lastUploadedSourceDir != null)
                    ? lastUploadedSourceDir
                    : Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }

        Path targetDir = ConsoleIO.readPath("• 다운로드 저장할 로컬 디렉토리 경로 입력 (원본과 다른 디렉토리) : ", false);

        try {
            LoginUser loginUser = ClientSession.getLoginUser();
            Path savedPath = rewardService.downloadRewardImageToDirectory(loginUser, rewardId, sourceDir, targetDir);
            long fileSize = Files.size(savedPath);

            System.out.println("=> [다운로드 완료] 원본과 다른 로컬 디렉토리에 이미지가 저장되었습니다!");
            System.out.println(" - 저장된 파일 절대 경로 : " + savedPath.toAbsolutePath().normalize());
            System.out.printf(" - 파일 크기 : %,d bytes%n", fileSize);
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
        } catch (Exception e) {
            ConsoleIO.printlnError("이미지 다운로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}