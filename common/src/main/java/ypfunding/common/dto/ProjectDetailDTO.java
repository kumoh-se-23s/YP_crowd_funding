package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ypfunding.common.type.Category;

import java.time.LocalDateTime;
import java.util.List;

// 프로젝트 상세 보기(평가표 14번) 결과를 담는 복합 DTO.
@Getter
@Setter
@ToString
public class ProjectDetailDTO {
    // 프로젝트 기본 정보 (제목, 설명, 목표 금액, 기간, 시작일, 종료일, 승인 상태 등)
    private ProjectDTO project;

    //프로젝트 카테고리 목록 (1~2개)
    private List<Category> categories;

    // 프로젝트 리워드 목록 (STOCK이 null이면 무제한, 0이면 품절)
    private List<RewardDTO> rewards;

    //이미지가 첨부되어 있는 리워드 ID 목록 (상세 화면에서 이미지 존재 여부 표시용, BLOB 제외)
    private List<Long> rewardIdsWithImage;

    // 최신순으로 정렬된 프로젝트 리뷰 목록 (평가표 5번)
    private List<ReviewDTO> reviews;

    /**
     * 리워드 제공 시기를 반환
     *
     * @return 펀딩 종료일 (승인 전이면 null)
     */
    public LocalDateTime getRewardDeliveryDate() {
        return project != null ? project.getEndDate() : null;
    }
}