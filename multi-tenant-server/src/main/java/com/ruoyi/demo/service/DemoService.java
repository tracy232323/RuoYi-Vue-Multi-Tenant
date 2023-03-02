package com.ruoyi.demo.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.demo.domain.UserInfo;
import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.domain.vo.UserInfoVo;

import java.util.List;

public interface DemoService {
    String getOringTree(ReqRootTree reqRootTree);
    void addAuthUser(ReqUserAuth reqUserAuth, String providerId, Integer userId);
    /**
     * 根据用户确定其独特的组织树结构
     * @param providerId
     * @param userId
     * @return
     */
    String getTreeByUserId(String providerId, Integer userId);

    void addAuth(ReqAuth reqAuth,String providerId, Integer userId);

    void delAuth(ReqAuth reqAuth,String providerId, Integer userId);

    /**
     * 获取指定节点对应的被授权人员信息
     * @param providerId
     * @param nodeId
     */
    List<MapUserNode> getNodeMap(String providerId, Integer nodeId);

    /**
     * 每个节点去获取子节点，迭代的方式，不合理需求
     * @param reqRootTree
     * @return
     */
//    List<JSONObject> getNodeAllUser(ReqRootTree reqRootTree);

    UserInfoVo getNodeAllUser(ReqRootTree reqRootTree);

    List<UserInfo> getNodeAllUserToExcel(Integer type, String currentProviderId, Integer orgId);

    /**
     * 指定岗位才展示人员
     * @param reqRootTree
     * @return
     */
    List<JSONObject> getPositionAllUser(ReqRootTree reqRootTree);
}
