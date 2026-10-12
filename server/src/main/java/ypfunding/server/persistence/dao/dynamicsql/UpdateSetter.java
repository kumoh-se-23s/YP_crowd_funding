package ypfunding.server.persistence.dao.dynamicsql;

import ypfunding.server.persistence.dao.ColumnsEnum;

public record UpdateSetter(ColumnsEnum column, ValueToken token) {

}
