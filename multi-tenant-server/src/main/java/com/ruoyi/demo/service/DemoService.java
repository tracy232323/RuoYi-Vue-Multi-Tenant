package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;

import java.util.List;

public interface DemoService {
    String getOringTree(ReqRootTree reqRootTree);
    void addAuthUser(ReqUserAuth reqUserAuth);
    /**
     * 根据用户确定其独特的组织树结构
     * @param providerId
     * @param userId
     * @return
     */
    String getTreeByUserId(String providerId, Integer userId);

    void addAuth(ReqAuth reqAuth);

    void delAuth(ReqAuth reqAuth);

    /**
     * 获取指定节点对应的被授权人员信息
     * @param providerId
     * @param nodeId
     */
    List<MapUserNode> getNodeMap(String providerId, Integer nodeId);
}
