package ypfunding.server.service;

import lombok.RequiredArgsConstructor;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.DAO;

@RequiredArgsConstructor
public class Service {
    protected final TransactionManager transactionManager;

}
