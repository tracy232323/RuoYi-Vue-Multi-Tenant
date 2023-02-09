package com.ruoyi.demo.service.impl;

import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.demo.mapper.NodeInfoMapper;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.demo.util.ApiOperationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.ruoyi.demo.constant.ApiOperationConstant.AUTHORITY_NOT_SHOW_VALUE;

@Service
public class DemoServiceImpl implements DemoService {
    @Autowired
    private ApiOperationUtil apiOperationUtil;
    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private MapUserNodeMapper mapUserNodeMapper;

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

    @Transactional
    @Override
    public void addAuthUser(ReqUserAuth reqUserAuth) {
        List<ReqUserAuth.UserAuth> list = reqUserAuth.getList();
        //需要插入的人员
        ArrayList<MapUserNode> mapUserNodes = new ArrayList<>();
        for (ReqUserAuth.UserAuth userAuth : list) {
            NodeInfo nodeInfo = nodeInfoMapper.selectOne(userAuth.getProviderId(), userAuth.getUserId());
            MapUserNode mapUserNode = MapUserNode.builder().userId(userAuth.getUserId()).companyId(userAuth.getProviderId()).nodeId(nodeInfo.getId()).isManage(0).isShow(0).build();
            mapUserNodes.add(mapUserNode);
        }
        mapUserNodeMapper.insertBatch(mapUserNodes);
    }
}
