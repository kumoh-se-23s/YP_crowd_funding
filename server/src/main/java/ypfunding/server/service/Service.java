package ypfunding.server.service;

import lombok.RequiredArgsConstructor;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.DAO;

@RequiredArgsConstructor
public class Service<T extends DAO> {
    protected final TransactionManager transactionManager;
    protected final T dao;

}
