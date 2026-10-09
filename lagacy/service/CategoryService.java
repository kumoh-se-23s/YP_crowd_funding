package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.CategoriesDAO;

public class CategoryService extends Service<CategoriesDAO>{
    public CategoryService(CategoriesDAO dao) {
        super(dao);
    }
}
