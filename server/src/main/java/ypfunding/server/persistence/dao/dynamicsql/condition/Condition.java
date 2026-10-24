package ypfunding.server.persistence.dao.dynamicsql.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract sealed class Condition permits
        BasicCondition,
        CombinedCondition {

    List<Object> userParameters;

    protected Condition(List<Object> userParameters) {
        this.userParameters = List.copyOf(userParameters);
    }

    public static Condition and(Condition... conditions) {
        List<Object> combined = Arrays.stream(conditions).flatMap(e -> e.getUserParameters().stream()).toList();
        return new CombinedCondition(Arrays.stream(conditions).map(e -> "(" + e + ")").collect(Collectors.joining(" AND ")), combined);
    }

    public static Condition or(Condition... conditions) {
        List<Object> combined = Arrays.stream(conditions).flatMap(e -> e.getUserParameters().stream()).toList();
        return new CombinedCondition(Arrays.stream(conditions).map(e -> "(" + e + ")").collect(Collectors.joining(" OR ")), combined);
    }

    public static Condition not(Condition v) {
        return new CombinedCondition("NOT (" + v + ")", v.userParameters);
    }

    public List<Object> getUserParameters() {
        return Collections.unmodifiableList(userParameters);
    }

    public static String whereChain(List<Condition> conditions) {
        StringBuilder whereBuilder = new StringBuilder();
        for (Condition condition : conditions) {
            whereBuilder.append(" where ").append(condition);
        }
        return whereBuilder.toString();
    }
}
