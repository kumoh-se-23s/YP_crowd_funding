package ypfunding.server.persistence.demo_dynamicsql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.UsersDAO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLBuilder;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.col.DirectColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.ScalarSubqueryColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.InlineViewReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinType;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinedTable;
import ypfunding.server.persistence.dao.dynamicsql.token.*;
import ypfunding.server.support.DbTestBase;

import java.util.List;

public class SQLBuilderIntegrityTest extends DbTestBase {

    @Test
    @DisplayName("SQL Builder 무결성 검사")
    public void test() {
        // 메인 쿼리의 테이블 참조
        var project = new DirectReferenceTable(Tables.PROJECTS);
        var user = new DirectReferenceTable(Tables.USERS);

// 프로젝트 조회용 Inline View
        var projectView = new InlineViewReferenceTable(
                new SQLSelect(
                        List.of(
                                new DirectColumnSelector(ProjectsDAO.Columns.PROJECT_ID),
                                new DirectColumnSelector(ProjectsDAO.Columns.USER_ID),
                                new DirectColumnSelector(ProjectsDAO.Columns.TITLE),
                                new DirectColumnSelector(ProjectsDAO.Columns.GOAL),
                                new DirectColumnSelector(ProjectsDAO.Columns.DURATION),
                                new DirectColumnSelector(ProjectsDAO.Columns.APPROVAL_STATUS)
                        ),
                        new DirectReferenceTable(Tables.PROJECTS)
                )
                        .addCondition(
                                BasicCondition.and(
                                        new BasicCondition(
                                                new ColumnValueToken(ProjectsDAO.Columns.APPROVAL_STATUS),
                                                ArithmeticConditionOperator.EQ,
                                                new UserValueToken("APPROVED")
                                        ),
                                        new BasicCondition(
                                                new ColumnValueToken(ProjectsDAO.Columns.GOAL),
                                                ArithmeticConditionOperator.GE,
                                                new UserValueToken(100)
                                        )
                                )
                        )
        );

// 프로젝트 소유자와 프로젝트를 연결
        var joined = new JoinedTable(
                projectView,
                JoinType.INNER,
                user,
                new BasicCondition(
                        new ColumnValueToken(ProjectsDAO.Columns.USER_ID),
                        ArithmeticConditionOperator.EQ,
                        new ColumnValueToken(UsersDAO.Columns.USER_ID)
                )
        );

// 프로젝트 ID가 현재 프로젝트와 일치하는지 검증하는 상관 서브쿼리
        var correlatedProject = new DirectReferenceTable(Tables.PROJECTS);

        var correlatedProjectQuery = new SQLSelect(
                List.of(
                        new DirectColumnSelector(ProjectsDAO.Columns.PROJECT_ID)
                ),
                correlatedProject
        ).addCondition(
                BasicCondition.and(
                        new BasicCondition(
                                new CorrelativeTableColumnToken(
                                        project,
                                        ProjectsDAO.Columns.PROJECT_ID
                                ),
                                ArithmeticConditionOperator.EQ,
                                new ColumnValueToken(ProjectsDAO.Columns.PROJECT_ID)
                        ),
                        new BasicCondition(
                                new ColumnValueToken(ProjectsDAO.Columns.APPROVAL_STATUS),
                                ArithmeticConditionOperator.EQ,
                                new UserValueToken("APPROVED")
                        ),
                        new BasicCondition(
                                new ColumnValueToken(ProjectsDAO.Columns.END_DATE),
                                ArithmeticConditionOperator.GE,
                                new UserValueToken("2026-01-01")
                        )
                )
        );

// 사용자별 프로젝트 조건을 검증하는 중첩 서브쿼리
        var userProjectQuery = new SQLSelect(
                List.of(
                        new DirectColumnSelector(ProjectsDAO.Columns.PROJECT_ID)
                ),
                new InlineViewReferenceTable(
                        new SQLSelect(
                                List.of(
                                        new DirectColumnSelector(ProjectsDAO.Columns.PROJECT_ID),
                                        new DirectColumnSelector(ProjectsDAO.Columns.USER_ID),
                                        new DirectColumnSelector(ProjectsDAO.Columns.GOAL),
                                        new DirectColumnSelector(ProjectsDAO.Columns.APPROVAL_STATUS)
                                ),
                                new DirectReferenceTable(Tables.PROJECTS)
                        ).addCondition(
                                BasicCondition.and(
                                        new BasicCondition(
                                                new ColumnValueToken(ProjectsDAO.Columns.GOAL),
                                                ArithmeticConditionOperator.GE,
                                                new UserValueToken(500)
                                        ),
                                        new BasicCondition(
                                                new ColumnValueToken(ProjectsDAO.Columns.APPROVAL_STATUS),
                                                ArithmeticConditionOperator.EQ,
                                                new UserValueToken("APPROVED")
                                        )
                                )
                        )
                )
        ).addCondition(
                BasicCondition.and(
                        new BasicCondition(
                                new CorrelativeTableColumnToken(
                                        user,
                                        UsersDAO.Columns.USER_ID
                                ),
                                ArithmeticConditionOperator.EQ,
                                new ColumnValueToken(ProjectsDAO.Columns.USER_ID)
                        ),
                        new BasicCondition(
                                new ColumnValueToken(ProjectsDAO.Columns.PROJECT_ID),
                                ArithmeticConditionOperator.EQ,
                                new CorrelativeTableColumnToken(
                                        project,
                                        ProjectsDAO.Columns.PROJECT_ID
                                )
                        )
                )
        );

// 최종 조회
        SQLBuilder<?> builder = new SQLSelect(
                List.of(
                        new DirectColumnSelector(ProjectsDAO.Columns.PROJECT_ID),
                        new DirectColumnSelector(ProjectsDAO.Columns.TITLE),
                        new DirectColumnSelector(ProjectsDAO.Columns.GOAL),
                        new DirectColumnSelector(ProjectsDAO.Columns.DURATION),
                        new DirectColumnSelector(ProjectsDAO.Columns.APPROVAL_STATUS),
                        new DirectColumnSelector(UsersDAO.Columns.USER_ID),
                        new DirectColumnSelector(UsersDAO.Columns.NAME),
                        new DirectColumnSelector(UsersDAO.Columns.ADDRESS),

                        new ScalarSubqueryColumnSelector(
                                new SQLSelect(
                                        List.of(
                                                new DirectColumnSelector(UsersDAO.Columns.LOGIN_ID)
                                        ),
                                        new DirectReferenceTable(Tables.USERS)
                                ).addCondition(
                                        new BasicCondition(
                                                new ColumnValueToken(UsersDAO.Columns.USER_ID),
                                                ArithmeticConditionOperator.EQ,
                                                new CorrelativeTableColumnToken(
                                                        project,
                                                        ProjectsDAO.Columns.USER_ID
                                                )
                                        )
                                ),
                                "owner_login_id"
                        )
                ),
                joined
        )
                .addCondition(
                        BasicCondition.and(
                                new BasicCondition(
                                        new ColumnValueToken(ProjectsDAO.Columns.GOAL),
                                        ArithmeticConditionOperator.GE,
                                        new UserValueToken(100)
                                ),
                                new BasicCondition(
                                        new ColumnValueToken(ProjectsDAO.Columns.DURATION),
                                        ArithmeticConditionOperator.LE,
                                        new UserValueToken(90)
                                ),
                                new BasicCondition(
                                        new ColumnValueToken(UsersDAO.Columns.USERTYPE),
                                        ArithmeticConditionOperator.EQ,
                                        new UserValueToken("ADMIN")
                                ),
                                new BasicCondition(
                                        new ColumnValueToken(ProjectsDAO.Columns.PROJECT_ID),
                                        ArithmeticConditionOperator.EQ,
                                        new SubqueryToken(correlatedProjectQuery)
                                ),
                                new BasicCondition(
                                        new ColumnValueToken(ProjectsDAO.Columns.PROJECT_ID),
                                        ArithmeticConditionOperator.EQ,
                                        new SubqueryToken(userProjectQuery)
                                )
                        )
                );

        SQLLogger.logQuery(builder);
    }
}
