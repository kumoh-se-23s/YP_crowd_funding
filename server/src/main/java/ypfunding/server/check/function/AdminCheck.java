package ypfunding.server.check.function;

import ypfunding.common.type.Category;
import ypfunding.server.check.CheckBase;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckUsers;
import ypfunding.server.persistence.dao.dynamicsql.col.SimpleOrderBySelector;

//C 담당 - 01, 04, 06, 07번
public class AdminCheck extends CheckBase {

    private static final int SAMPLE_PROJECT_ID = 6;
    public AdminCheck(AppContext context, CheckUsers users) {
        super(context, users);
    }

    public void test01ViewFundingStatus() {
        context.getProjectQueryService().getMyProjects(users.maker(), null, null);
    }

    public void test04ApproveProject() {
        context.getAdminService().approveProject(users.admin(), SAMPLE_PROJECT_ID);
    }

    public void test06ViewProjects() {
        context.getProjectQueryService().getAllActiveProjectsRecent(users.supporter());
    }

    public void test07FilterProjects() {
        context.getProjectQueryService().filterActiveProjects(users.supporter(), Category.TECH_HOME_APPLIANCES);
        context.getProjectQueryService().filterActiveProjects(users.supporter(), Category.SOCIAL);
    }
}
