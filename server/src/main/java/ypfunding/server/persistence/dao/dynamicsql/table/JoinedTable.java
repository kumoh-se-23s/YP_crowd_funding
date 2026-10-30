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


    private boolean noParenthesesRequired(ReferenceTable table){
        return table instanceof DirectReferenceTable || table instanceof PredefinedReferenceTable
               || table instanceof InlineViewReferenceTable;
    }

    @Override
    public String toString() {

        String lhsTableStr = lhs instanceof PredefinedReferenceTable t ? t.getVarName() : lhs.toString();
        String rhsTableStr = rhs instanceof PredefinedReferenceTable t ? t.getVarName() : rhs.toString();

        String lhsStr = noParenthesesRequired(lhs) ? lhsTableStr : ("(" + lhsTableStr + ")");
        String rhsStr = noParenthesesRequired(rhs) ? rhsTableStr : ("(" + rhsTableStr + ")");

        return "(" + lhsStr + " " + type + " " + rhsStr + " on " + joinCondition + ")";
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
