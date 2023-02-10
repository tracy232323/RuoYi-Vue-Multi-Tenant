package com.ruoyi.demo.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.constant.RedisConstant;
import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.demo.mapper.NodeInfoMapper;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.util.ApiOperationUtil;
import com.ruoyi.demo.util.BuildTreeUtil;
import com.ruoyi.framework.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
public class DemoServiceImpl implements DemoService {
    @Autowired
    private ApiOperationUtil apiOperationUtil;
    @Autowired
    private NodeInfoMapper nodeInfoMapper;
    @Autowired
    private MapUserNodeMapper mapUserNodeMapper;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private NodeInfoService nodeInfoService;
    @Autowired
    private BuildTreeUtil buildTreeUtil;

    @Override
    public String getOringTree(ReqRootTree reqRootTree) {
        //获取所有二级集团单位集合
        if (reqRootTree.getType() == null) {
            return apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        }
        if (1 == reqRootTree.getType() || 2 == reqRootTree.getType()) {
            return apiOperationUtil.getCompanyAllOrg(ApiOperationConstant.GET_COMPANY_ALL_ORG_URL, reqRootTree.getProviderId(), reqRootTree.getOrgId());
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
        ArrayList<MapUserNode> addNodes = new ArrayList<>();
        for (ReqUserAuth.UserAuth userAuth : list) {
            NodeInfo nodeInfo = nodeInfoMapper.selectOne(userAuth.getProviderId(), userAuth.getOrgId());
            MapUserNode mapUserNode = MapUserNode.builder()
                    .userId(userAuth.getUserId())
                    .companyId(userAuth.getProviderId())
                    .nodeId(nodeInfo.getId())
                    .isManage(ApiOperationConstant.AUTHORITY_NOT_MANAGER_VALUE)
                    .isShow(ApiOperationConstant.AUTHORITY_NOT_SHOW_VALUE)
                    .build();
            addNodes.add(mapUserNode);
        }
        mapUserNodeMapper.insertBatch(addNodes);
    }

    @Override
    public String getTreeByUserId(String providerId, Integer userId) {
        // 首先判断当前用户在redis中有无缓存，有的话直接取
        String key = RedisConstant.REDIS_USER_TREE_PREFIX + providerId + ":" + userId;
        Object cacheObject = redisCache.getCacheObject(key);
        if (StringUtils.isEmpty(cacheObject)) {
            return cacheObject.toString();
        }
        // 检索出用户以及用户全部岗位的节点，进行一个去重并集之后再进行树的构建
        List<NodeInfo> nodeInfos = nodeInfoService.selectByMap(providerId, userId);
        String userAllPosition = apiOperationUtil.getUserAllPosition(ApiOperationConstant.GET_USER_ALL_POSITION_URL, providerId, userId);
        List<JSONObject> userPositions = new JSONArray(userAllPosition).toList(JSONObject.class);
        for (JSONObject userPosition : userPositions) {
            Integer positionId = userPosition.get(NodeFieldConstant.POSITION_ID, Integer.class);
            List<NodeInfo> tempNodeInfos = nodeInfoService.selectMapByPositionId(providerId, positionId);
            nodeInfos.addAll(tempNodeInfos);
            // 进行去重并集处理，获取新的nodeInfos
            nodeInfos = nodeInfos.stream().distinct().collect(Collectors.toList());
        }
        // 构建树结构
        return buildTreeUtil.buildShowTree(nodeInfos);
    }

    @Override
    public void addAuth(ReqAuth reqAuth) {
        Integer[] ids = reqAuth.getIds();
        for (Integer id : ids) {
            mapUserNodeMapper.update2Auth(id, reqAuth.getIsManage(), reqAuth.getIsShow());
        }
    }

    @Override
    public void delAuth(ReqAuth reqAuth) {
        List<Integer> ids = Arrays.asList(reqAuth.getIds());
        mapUserNodeMapper.deleteByNodeIds(ids);
    }
}
