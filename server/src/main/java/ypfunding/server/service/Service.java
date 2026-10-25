package ypfunding.server.service;

import lombok.RequiredArgsConstructor;
import ypfunding.server.persistence.TransactionManager;

@RequiredArgsConstructor
public class Service {
    protected final TransactionManager transactionManager;

}
