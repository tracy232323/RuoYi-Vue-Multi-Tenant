package com.ruoyi.demo.service.impl;

import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.demo.util.ApiOperationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemoServiceImpl implements DemoService {
    @Autowired
    private ApiOperationUtil apiOperationUtil;


    @Override
    public String getOringTree(ReqRootTree reqRootTree) {
        //获取所有二级集团单位集合
        if (reqRootTree.getType() == null) {
            return apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        }

        if (1 == reqRootTree.getType() || 2 == reqRootTree.getType()) {
            return apiOperationUtil.getCompanyAllOrg(ApiOperationConstant.GET_COMPANY_ALL_ORG_URL,reqRootTree.getProviderId(), reqRootTree.getOrgId());
        }

        if (3 == reqRootTree.getType()) {
            return apiOperationUtil.getOrgAllUsers(ApiOperationConstant.GET_POSITION_ALL_USER_URL, reqRootTree.getProviderId(), reqRootTree.getPositionId());
        }

        return null;
    }
}
