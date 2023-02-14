package com.ruoyi.demo.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.NodeInfo;
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
import com.ruoyi.demo.util.CommonUtil;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.mapper.SysOperLogMapper;
import com.ruoyi.project.monitor.service.ISysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.util.StringUtils;


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
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private SysOperLogMapper sysOperLogMapper;

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
    public void addAuthUser(ReqUserAuth reqUserAuth, String providerId, Integer userId) {
        // 获取当前登陆人员信息
        String loginUser = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, providerId, userId);
        String loginUserName = new JSONObject(loginUser).get("name",String.class);
        List<ReqUserAuth.UserAuth> list = reqUserAuth.getList();
        //需要插入的人员
        ArrayList<MapUserNode> addNodes = new ArrayList<>();
        List<SysOperLog> addOperLogs = new ArrayList<>();
        for (ReqUserAuth.UserAuth userAuth : list) {
            NodeInfo nodeInfo = nodeInfoMapper.selectOne(userAuth.getProviderId(), userAuth.getOrgId());
            // 构建当前人员所在组织路径
            String orgPath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, userAuth.getProviderId(), userAuth.getPositionId());
            String userInfo = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, userAuth.getProviderId(), userAuth.getUserId());
            String path = commonUtil.buildUserPathFromTree(orgPath);
            String name = new JSONObject(userInfo).get("name", String.class);
            path = path + " " + name;
            MapUserNode mapUserNode = MapUserNode.builder()
                    .userId(userAuth.getUserId())
                    .companyId(userAuth.getProviderId())
                    .nodeId(nodeInfo.getId())
                    .path(path)
                    .isManage(ApiOperationConstant.AUTHORITY_NOT_MANAGER_VALUE)
                    .isShow(ApiOperationConstant.AUTHORITY_NOT_SHOW_VALUE)
                    .build();
            String nodePath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, userAuth.getProviderId(), userAuth.getOrgId());
            nodePath = commonUtil.buildUserPathFromTree(nodePath);
            addNodes.add(mapUserNode);
            SysOperLog sysOperLog = new SysOperLog();
            sysOperLog.setTitle(ApiOperationConstant.OPERATION_TITLE_ADD_NUMBER);
            sysOperLog.setBusinessType(1);
            sysOperLog.setMethod("addAuthUser");
            sysOperLog.setRequestMethod("POST");
            sysOperLog.setOperatorType(1);
            sysOperLog.setOperName(loginUserName);
//            sysOperLog.setDeptName("神州数码");
            sysOperLog.setNodeId(userAuth.getOrgId());
            sysOperLog.setOperInfo("在"+nodePath+"添加"+path+"人员");
            addOperLogs.add(sysOperLog);
        }
        mapUserNodeMapper.insertBatch(addNodes);
        sysOperLogMapper.insertOperlogBatch(addOperLogs);
    }

    @Override
    public String getTreeByUserId(String providerId, Integer userId) {
        // 首先判断当前用户在redis中有无缓存，有的话直接取
        String key = RedisConstant.REDIS_USER_TREE_PREFIX + providerId + ":" + userId;
        if (BuildTreeUtil.rootTree.containsKey(key)) {
            return BuildTreeUtil.rootTree.get(key);
        }
        // 检索出用户以及用户全部岗位的节点，进行一个去重并集之后再进行树的构建
        List<NodeInfo> nodeInfos = nodeInfoService.selectByMap(providerId, userId);
//        String userAllPosition = apiOperationUtil.getUserAllPosition(ApiOperationConstant.GET_USER_ALL_POSITION_URL, providerId, userId);
//        List<JSONObject> userPositions = new JSONArray(userAllPosition).toList(JSONObject.class);
//        for( JSONObject userPosition : userPositions ){
//            Integer positionId = userPosition.get(NodeFieldConstant.POSITION_ID, Integer.class);
//            List<NodeInfo> tempNodeInfos = nodeInfoService.selectMapByPositionId(providerId, positionId);
//            nodeInfos.addAll(tempNodeInfos);
//            // 进行去重并集处理，获取新的nodeInfos
//            nodeInfos = nodeInfos.stream().distinct().collect(Collectors.toList());
//        }
        // 构建树结构
        return buildTreeUtil.buildShowTree(nodeInfos);
    }

    @Override
    public void addAuth(ReqAuth reqAuth,String providerId, Integer userId) {
        Integer[] ids = reqAuth.getIds();
        String loginUser = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, providerId, userId);
        String loginUserName = new JSONObject(loginUser).get("name",String.class);
        for (Integer id : ids) {
            mapUserNodeMapper.update2Auth(id, reqAuth.getIsManage(), reqAuth.getIsShow());
            // 根据id获取映射表信息，从中获取nodeId
            MapUserNode mapUserNode = mapUserNodeMapper.selectOneById(id);
            // 根据id获取node信息
            NodeInfo nodeInfo = nodeInfoMapper.selectOneById(mapUserNode.getId());
            String nodePath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, nodeInfo.getProviderId(), nodeInfo.getNodeId());
            nodePath = commonUtil.buildUserPathFromTree(nodePath);
            StringBuilder info = new StringBuilder("在"+nodePath+"对"+mapUserNode.getPath());
            StringBuilder str = new StringBuilder("");
            if( reqAuth.getIsManage().equals(ApiOperationConstant.AUTHORITY_MANAGER_VALUE) ){
                str.append(" 管理权限 ");
            }
            if( reqAuth.getIsShow().equals(ApiOperationConstant.AUTHORITY_SHOW_VALUE) ){
                str.append(" 浏览权限 ");
            }
            if(StringUtils.isEmpty(str)){
                str.insert(0,"授予");
                info.append(str);
            }
            StringBuilder str1 = new StringBuilder("");
            if( !reqAuth.getIsManage().equals(ApiOperationConstant.AUTHORITY_MANAGER_VALUE) ){
                str1.append(" 管理权限 ");
            }
            if( !reqAuth.getIsShow().equals(ApiOperationConstant.AUTHORITY_SHOW_VALUE) ){
                str1.append(" 浏览权限 ");
            }
            if(!StringUtils.isEmpty(str)){
                str.insert(0,"撤销");
                info.append(str);
            }
            SysOperLog sysOperLog = new SysOperLog();
            sysOperLog.setTitle(ApiOperationConstant.OPERATION_TITLE_ADD_NUMBER);
            sysOperLog.setBusinessType(1);
            sysOperLog.setMethod("addAuth");
            sysOperLog.setRequestMethod("POST");
            sysOperLog.setOperatorType(1);
            sysOperLog.setOperName(loginUserName);
//            sysOperLog.setDeptName("神州数码");
            sysOperLog.setNodeId(nodeInfo.getNodeId());
            sysOperLog.setOperInfo(info.toString());
            sysOperLogMapper.insertOperlog(sysOperLog);
        }
    }

    @Override
    public void delAuth(ReqAuth reqAuth,String providerId, Integer userId) {
//        String loginName = reqAuth.getLoginName();
//        Object cacheObject = redisCache.getCacheObject(loginName);
//        if (StringUtils.isEmpty(cacheObject)) {
//            throw new CustomException("检索失败，当前无登陆用户");
//        }
        // 查询当前用户拥有多少预览权限节点
//        String[] arr = cacheObject.toString().split("\\|");
        String loginUser = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, providerId, userId);
        String loginUserName = new JSONObject(loginUser).get("name",String.class);
        List<Integer> ids = Arrays.asList(reqAuth.getIds());
        List<SysOperLog> addOperLogs = new ArrayList<>();
        for( Integer id : ids ){
            // 根据id获取映射表信息，从中获取nodeId
            MapUserNode mapUserNode = mapUserNodeMapper.selectOneById(id);
            // 根据id获取node信息
            NodeInfo nodeInfo = nodeInfoMapper.selectOneById(mapUserNode.getId());
            String nodePath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, nodeInfo.getProviderId(), nodeInfo.getNodeId());
            nodePath = commonUtil.buildUserPathFromTree(nodePath);
            SysOperLog sysOperLog = new SysOperLog();
            sysOperLog.setTitle(ApiOperationConstant.OPERATION_TITLE_ADD_NUMBER);
            sysOperLog.setBusinessType(1);
            sysOperLog.setMethod("delAuth");
            sysOperLog.setRequestMethod("POST");
            sysOperLog.setOperatorType(1);
            sysOperLog.setOperName(loginUserName);
            sysOperLog.setOperInfo("删除"+nodePath+"上"+mapUserNode.getPath());
            sysOperLog.setNodeId(nodeInfo.getNodeId());
            addOperLogs.add(sysOperLog);
        }
        mapUserNodeMapper.deleteByNodeIds(ids);
        sysOperLogMapper.insertOperlogBatch(addOperLogs);
    }

    @Override
    public List<MapUserNode> getNodeMap(String providerId, Integer nodeId) {
        // 先通过providerId和nodeId确定唯一节点标识
        NodeInfo nodeInfo = nodeInfoMapper.selectOne(providerId, nodeId);
        if (StringUtils.isEmpty(nodeInfo)) {
            throw new CustomException("被授权节点不存在，无法进行当前节点被授权人员信息展示");
        }
        // 通过唯一标识去映射表中检索出此节点拥有多少被授权人员和岗位
        return mapUserNodeMapper.selectListByNodeId(nodeInfo.getId());
    }

    @Override
    public List<JSONObject> getNodeAllUser(ReqRootTree reqRootTree) {
        List<JSONObject> list = new ArrayList<>();
        getOrgUsers(reqRootTree.getProviderId(), reqRootTree.getType(), reqRootTree.getOrgId(), reqRootTree.getPositionId(), list);
        for (JSONObject jsonObject : list) {
            //判断用户状态
            NodeInfo nodeInfo = nodeInfoMapper.selectOne(reqRootTree.getProviderId(), jsonObject.getInt("positionId"));
            MapUserNode mapUserNode = null;
            if (3 == nodeInfo.getType()) {
                Integer fatherId = nodeInfo.getFatherId();
                NodeInfo node = nodeInfoMapper.selectOne(reqRootTree.getProviderId(), fatherId);
                mapUserNode = mapUserNodeMapper.selectOne(reqRootTree.getProviderId(), jsonObject.getInt("id"), node.getId());
            } else {
                mapUserNode = mapUserNodeMapper.selectOne(reqRootTree.getProviderId(), jsonObject.getInt("id"), nodeInfo.getId());
            }

            if (!ApiOperationConstant.AUTHORITY_SHOW_VALUE.equals(mapUserNode.getIsShow())) {
                continue;
            }
            String orgPath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, reqRootTree.getProviderId(), jsonObject.getInt("positionId"));
            String path = commonUtil.buildUserPathFromTree(orgPath);
            jsonObject.putOpt("path", path);
        }
        redisCache.setCacheObject("getNodeAllUser", JSONUtil.toJsonStr(list), 60, TimeUnit.MINUTES);
        return list;
    }

    private void getOrgUsers(String providerId, Integer type, Integer orgId, Integer positionId, List<JSONObject> list) {
        //获取所有二级集团单位集合
        if (1 == type || 2 == type) {
            String jsonStr = apiOperationUtil.getCompanyAllOrg(ApiOperationConstant.GET_COMPANY_ALL_ORG_URL, providerId, orgId);
            List<JSONObject> jsonObjects = JSONUtil.toList(jsonStr, JSONObject.class);
            for (JSONObject jsonObject : jsonObjects) {
                Integer tp = jsonObject.getInt("type");
                Integer id = jsonObject.getInt("id");
                if (tp == 3) {
                    getOrgUsers(providerId, tp, null, id, list);
                } else {
                    getOrgUsers(providerId, tp, id, null, list);
                }
            }
        } else if (type == 3) {
            String jsonStr = apiOperationUtil.getOrgAllUsers(ApiOperationConstant.GET_POSITION_ALL_USER_URL, providerId, positionId);
            List<JSONObject> jsonObjects = JSONUtil.toList(jsonStr, JSONObject.class);
            for (JSONObject jsonObject : jsonObjects) {
                jsonObject.putOpt("positionId", positionId);
            }
            list.addAll(jsonObjects);
        }
    }

}
