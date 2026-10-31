package ypfunding.server.service;

import ypfunding.common.dto.FundingStatusDTO;
import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.FundingStatusDAO;
import ypfunding.server.persistence.dao.ProjectQueryDAO;
import ypfunding.server.util.UserUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class FundingStatusService extends Service {
    private final ProjectQueryDAO projectQueryDAO;
    private final FundingStatusDAO fundingStatusDAO;

    public FundingStatusService(TransactionManager transactionManager,
                                ProjectQueryDAO projectQueryDAO,
                                FundingStatusDAO fundingStatusDAO) {
        super(transactionManager);
        this.projectQueryDAO = projectQueryDAO;
        this.fundingStatusDAO = fundingStatusDAO;
    }

    //메이커 본인 프로젝트 전체의 펀딩 현황 (프로젝트 ID 순)
    public List<FundingStatusDTO> getMyFundingStatuses(LoginUser maker) {
        UserUtils.assertMaker(maker);

        return transactionManager.execute(conn -> {
            List<FundingStatusDTO> result = new ArrayList<>();
            for (ProjectQueryDTO project : findMyProjects(conn, maker)) {
                result.add(toFundingStatus(conn, project));
            }
            return result;
        });
    }

    //메이커 본인 프로젝트 하나의 펀딩 현황 (CLI용)
    public FundingStatusDTO getFundingStatus(LoginUser maker, long projectId) {
        UserUtils.assertMaker(maker);

        return transactionManager.execute(conn -> {
            ProjectQueryDTO project = null;
            for (ProjectQueryDTO p : findMyProjects(conn, maker)) {
                if (p.getProjectId() == projectId) {
                    project = p;
                }
            }
            if (project == null) {
                throw new BusinessException("본인 프로젝트의 펀딩 현황만 볼 수 있습니다.");
            }
            return toFundingStatus(conn, project);
        });
    }

    // 메이커 본인 프로젝트 목록 (승인 상태와 상관없이 전부)
    private List<ProjectQueryDTO> findMyProjects(Connection conn, LoginUser maker) throws SQLException {
        List<ProjectQueryDTO> projects = new ArrayList<>(projectQueryDAO.getMyProjects(conn, maker.userId(), null, null));
        projects.sort(Comparator.comparing(ProjectQueryDTO::getProjectId));
        return projects;
    }

    private FundingStatusDTO toFundingStatus(Connection conn, ProjectQueryDTO project) throws SQLException {
        FundingStatusDTO dto = new FundingStatusDTO();
        dto.setProject(project);
        dto.setRewards(fundingStatusDAO.findRewardStatuses(conn, project.getProjectId()));
        dto.setSupporters(fundingStatusDAO.findSupporters(conn, project.getProjectId()));
        return dto;
    }
}