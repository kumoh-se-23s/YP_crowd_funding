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

    public static Condition and(List<Condition> conditions) {
        List<Object> combined = conditions.stream().flatMap(e -> e.getUserParameters().stream()).toList();
        return new CombinedCondition(conditions.stream().map(e -> "(" + e + ")").collect(Collectors.joining(" AND ")), combined);
    }

    public static Condition or(List<Condition> conditions) {
        List<Object> combined = conditions.stream().flatMap(e -> e.getUserParameters().stream()).toList();
        return new CombinedCondition(conditions.stream().map(e -> "(" + e + ")").collect(Collectors.joining(" OR ")), combined);
    }

    public static Condition and(Condition... conditions) {
        return and(List.of(conditions));
    }

    public static Condition or(Condition... conditions){
        return or(List.of(conditions));
    }

    public static Condition not(Condition v) {
        return new CombinedCondition("NOT (" + v + ")", v.userParameters);
    }

    public List<Object> getUserParameters() {
        return Collections.unmodifiableList(userParameters);
    }

}
