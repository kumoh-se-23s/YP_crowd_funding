package ypfunding.server.check.function;

import ypfunding.common.type.Category;
import ypfunding.server.check.CheckBase;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckUsers;

import java.util.Objects;
import java.util.stream.Collectors;

//C 담당 - 01, 04, 06, 07번
public class AdminCheck extends CheckBase {

    private static final int SAMPLE_PROJECT_ID = 6;

    public AdminCheck(AppContext context, CheckUsers users) {
        super(context, users);
    }

    public void test04ApproveProject() {
        System.out.println("========= TEST 04 ========");
        context.getAdminService().approveProject(users.admin(), SAMPLE_PROJECT_ID);
    }

    public void test06ViewProjects() {
        System.out.println("========= TEST 06 ========");
        System.out.println(context.getProjectQueryService().getAllActiveOrEndedProjectsRecent(users.supporter()).stream().map(Object::toString)
                .collect(Collectors.joining("\n")));
    }

    public void test07FilterProjects() {
        System.out.println("========= TEST 07 ========");
        System.out.println(context.getProjectQueryService().filterActiveProjects(users.supporter(), Category.TECH_HOME_APPLIANCES).stream().map(Objects::toString).collect(Collectors.joining("\n")));
        System.out.println(context.getProjectQueryService().filterActiveProjects(users.supporter(), Category.SOCIAL).stream().map(Objects::toString).collect(Collectors.joining("\n")));
    }
}
