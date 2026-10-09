package kr.yp_crowdfunding.service;

import lombok.RequiredArgsConstructor;
import kr.yp_crowdfunding.persistence.dao.DAO;

@RequiredArgsConstructor
public class Service<T extends DAO> {
    protected final T dao;
}
