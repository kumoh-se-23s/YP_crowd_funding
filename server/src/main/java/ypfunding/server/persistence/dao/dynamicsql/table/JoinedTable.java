package ypfunding.server.persistence.dao.dynamicsql.table;

import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.token.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JoinedTable extends ReferenceTable{

    private final ReferenceTable lhs;
    private final JoinType type;
    private final ReferenceTable rhs;
    private final Condition joinCondition;

    public JoinedTable(ReferenceTable lhs, JoinType type, ReferenceTable rhs, Condition joinCondition) {
        this.lhs = lhs;
        this.type = type;
        this.rhs = rhs;
        this.joinCondition = joinCondition;

    }

    @Override
    public String getVarPrefix() {
        return "__jt";
    }


    private boolean noParenthesesRequired(ReferenceTable table){
        return table instanceof DirectReferenceTable || table instanceof PredefinedReferenceTable;
    }

    @Override
    public String toString() {

        String lhsStr = noParenthesesRequired(lhs) ? lhs.toString() : ("(" + lhs + ")");
        String rhsStr = noParenthesesRequired(rhs) ? rhs.toString() : ("(" + rhs + ")");

        return "(" + lhsStr + " " + type + " " + rhsStr + " on " + joinCondition + ") as " + getVarName();
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
