package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;

public record UpdateSelector(ColumnsEnum column, ValueToken token) {

}
