package ypfunding.server.persistence.dao.dynamicsql.table;

import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JoinedTable extends ReferenceTable{

    private final ReferenceTable lhs;
    private final JoinType type;
    private final ReferenceTable rhs;
    private final Condition joinCondition;
    private static final String VAR_PREFIX = "__jt";
    private static int cnt = 0;
    private final int num;

    public JoinedTable(ReferenceTable lhs, JoinType type, ReferenceTable rhs, Condition joinCondition) {
        this.lhs = lhs;
        this.type = type;
        this.rhs = rhs;
        cnt = (cnt + 1) % 1_000_000_007; //arithmetic overflow protection
        this.num = cnt;
        this.joinCondition = joinCondition;

    }


    public String getVarName() {
        return VAR_PREFIX + num;
    }

    @Override
    public String toString() {
        return "(" + lhs + ") " + type + " (" + rhs + ") as " + getVarName() + " on " + joinCondition;
    }

    @Override
    public List<Object> getUserParameters() {
        List<Object> result = new ArrayList<>();
        result.addAll(lhs.getUserParameters());
        result.addAll(rhs.getUserParameters());
        result.addAll(joinCondition.getUserParameters());
        return Collections.unmodifiableList(result);
    }
}
