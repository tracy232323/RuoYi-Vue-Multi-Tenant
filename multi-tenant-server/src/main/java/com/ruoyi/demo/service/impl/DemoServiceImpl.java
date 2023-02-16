package com.ruoyi.demo.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.constant.RedisConstant;
import com.ruoyi.demo.domain.TreeNode;
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
import com.ruoyi.demo.util.IdObject;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.mapper.SysOperLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
        List<ReqUserAuth.UserAuth> list = reqUserAuth.getList();
        if( list.isEmpty() ){
            throw new CustomException("无添加人员");
        }
        //需要插入的人员
        ArrayList<MapUserNode> addNodes = new ArrayList<>();
        List<SysOperLog> addOperLogs = new ArrayList<>();
        NodeInfo nodeInfo = null;
        ReqUserAuth.UserAuth tempUserAuth = list.get(0);
        nodeInfo = nodeInfoMapper.selectOne(tempUserAuth.getNodeProviderId(), tempUserAuth.getOrgId());
        // 首先校验权限，即在当前节点，或者是前序节点中是否存在管理权限
        Boolean aBoolean = checkAuthority(nodeInfo,providerId,userId);
        if( Boolean.FALSE.equals(aBoolean) ){
            throw new CustomException("无管理权限");
        }
        String loginUser = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, providerId, userId);
        String loginUserName = new JSONObject(loginUser).get("name", String.class);
        if ( StringUtils.isEmpty(nodeInfo) ){
            throw new CustomException("添加节点不存在");
        }
        for (ReqUserAuth.UserAuth userAuth : list) {
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
            String nodePath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, userAuth.getNodeProviderId(), userAuth.getOrgId());
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
            sysOperLog.setOperInfo("在" + nodePath + "添加" + path + "人员");
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
        // 检索出用户拥有权限的节点
        List<NodeInfo> nodeInfos = nodeInfoService.selectByMap(providerId, userId);
        // 可能存在父子节点均有不同权限的情况，比如zld在A节点有浏览权限，B节点有管理权限，所以要将B去掉，只构建A即可
        HashMap<IdObject, NodeInfo> infoHashMap = new HashMap<>();
        Iterator<NodeInfo> nodeInfoIterator = nodeInfos.iterator();
        while (nodeInfoIterator.hasNext()) {
            NodeInfo next = nodeInfoIterator.next();
            infoHashMap.put(new IdObject(next.getNodeId(), next.getProviderId()), next);
        }
        Set<Map.Entry<IdObject, NodeInfo>> entries = infoHashMap.entrySet();
        Iterator<Map.Entry<IdObject, NodeInfo>> entryIterator = entries.iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<IdObject, NodeInfo> next = entryIterator.next();
            boolean b = buildTreeUtil.saveGrandfatherNode(infoHashMap, next.getValue());
            if (!b) {
                nodeInfos.remove(next);
            }
        }
        // 去除好之后进行树的构建
        TreeNode root = new TreeNode();
        root.setNodeInfo(commonUtil.getRootNode());
        return buildShowTree(nodeInfos, root);
    }

    @Override
    public void addAuth(ReqAuth reqAuth, String providerId, Integer userId) {
        Integer[] ids = reqAuth.getIds();
        if( ids.length <= 0 ){
            throw new CustomException("无修改人员");
        }
        MapUserNode tempNodeMap = mapUserNodeMapper.selectOneById(ids[0]);
        NodeInfo tempNode = nodeInfoMapper.selectOneById(tempNodeMap.getNodeId());
        Boolean aBoolean = checkAuthority(tempNode,providerId,userId);
        if( Boolean.FALSE.equals(aBoolean) ){
            throw new CustomException("无管理权限");
        }
        String loginUser = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, providerId, userId);
        String loginUserName = new JSONObject(loginUser).get("name", String.class);
        for (Integer id : ids) {
            mapUserNodeMapper.update2Auth(id, reqAuth.getIsManage(), reqAuth.getIsShow());
            // 根据id获取映射表信息，从中获取nodeId
            MapUserNode mapUserNode = mapUserNodeMapper.selectOneById(id);
            // 根据id获取node信息
            NodeInfo nodeInfo = nodeInfoMapper.selectOneById(mapUserNode.getId());
            String nodePath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, nodeInfo.getProviderId(), nodeInfo.getNodeId());
            nodePath = commonUtil.buildUserPathFromTree(nodePath);
            StringBuilder info = new StringBuilder("在" + nodePath + "对" + mapUserNode.getPath());
            StringBuilder str = new StringBuilder("");
            if (reqAuth.getIsManage().equals(ApiOperationConstant.AUTHORITY_MANAGER_VALUE)) {
                str.append(" 管理权限 ");
            }
            if (reqAuth.getIsShow().equals(ApiOperationConstant.AUTHORITY_SHOW_VALUE)) {
                str.append(" 浏览权限 ");
            }
            if (!StringUtils.isEmpty(str)) {
                str.insert(0, "授予");
                info.append(str);
            }
            StringBuilder str1 = new StringBuilder("");
            if (reqAuth.getIsManage().equals(ApiOperationConstant.AUTHORITY_NOT_MANAGER_VALUE)) {
                str1.append(" 管理权限 ");
            }
            if (reqAuth.getIsShow().equals(ApiOperationConstant.AUTHORITY_NOT_SHOW_VALUE)) {
                str1.append(" 浏览权限 ");
            }
            if (!StringUtils.isEmpty(str1)) {
                str1.insert(0, "撤销");
                info.append(str1);
            }
            SysOperLog sysOperLog = new SysOperLog();
            sysOperLog.setTitle(ApiOperationConstant.OPERATION_TITLE_MODIFY_AUTHORITY);
            sysOperLog.setBusinessType(2);
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
    public void delAuth(ReqAuth reqAuth, String providerId, Integer userId) {
//        String loginName = reqAuth.getLoginName();
//        Object cacheObject = redisCache.getCacheObject(loginName);
//        if (StringUtils.isEmpty(cacheObject)) {
//            throw new CustomException("检索失败，当前无登陆用户");
//        }
        // 查询当前用户拥有多少预览权限节点
//        String[] arr = cacheObject.toString().split("\\|");
        List<Integer> ids = Arrays.asList(reqAuth.getIds());
        if( ids.isEmpty() ){
            throw new CustomException("无修改人员");
        }
        MapUserNode tempNodeMap = mapUserNodeMapper.selectOneById(ids.get(0));
        NodeInfo tempNode = nodeInfoMapper.selectOneById(tempNodeMap.getNodeId());
        Boolean aBoolean = checkAuthority(tempNode,providerId,userId);
        if( Boolean.FALSE.equals(aBoolean) ){
            throw new CustomException("无管理权限");
        }
        String loginUser = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, providerId, userId);
        String loginUserName = new JSONObject(loginUser).get("name", String.class);        List<SysOperLog> addOperLogs = new ArrayList<>();
        for (Integer id : ids) {
            // 根据id获取映射表信息，从中获取nodeId
            MapUserNode mapUserNode = mapUserNodeMapper.selectOneById(id);
            // 根据id获取node信息
            NodeInfo nodeInfo = nodeInfoMapper.selectOneById(mapUserNode.getId());
            String nodePath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, nodeInfo.getProviderId(), nodeInfo.getNodeId());
            nodePath = commonUtil.buildUserPathFromTree(nodePath);
            SysOperLog sysOperLog = new SysOperLog();
            sysOperLog.setTitle(ApiOperationConstant.OPERATION_TITLE_DELETE_AUTHORITY);
            sysOperLog.setBusinessType(3);
            sysOperLog.setMethod("delAuth");
            sysOperLog.setRequestMethod("POST");
            sysOperLog.setOperatorType(1);
            sysOperLog.setOperName(loginUserName);
            sysOperLog.setOperInfo("删除" + nodePath + "上" + mapUserNode.getPath());
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


    public String buildShowTree(List<NodeInfo> nodeInfos, TreeNode rootTree) {
        Iterator<NodeInfo> nodeInfoIterator = nodeInfos.iterator();
        while (nodeInfoIterator.hasNext()) {
            NodeInfo node = nodeInfoIterator.next();
            buildShowTree(node, rootTree);
        }
        return JSONUtil.toJsonStr(rootTree);
    }

    public void buildShowTree(NodeInfo nodeInfo, TreeNode rootNode) {
        String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, nodeInfo.getProviderId(), nodeInfo.getNodeId());
        // 遍历organizationChildren将其封装为TreeNode
        JSONObject childrenJSON = new JSONObject(organizationChildren);
        getOrganizationChildren(rootNode, childrenJSON, nodeInfo.getProviderId(), commonUtil.getRootNode().getNodeId());
    }

    public void getOrganizationChildren(TreeNode treeNode, JSONObject data, String providerId, Integer id) {
        Integer type = data.get(NodeFieldConstant.TYPE_FIELD_NAME, Integer.class);
        if (ApiOperationConstant.TYPE_POSITION.equals(type)) {
            return;
        }
        // 提取字段组成NodeInfo
        NodeInfo nodeInfo = buildNodeInfoByJSON(data, providerId, id);
        TreeNode currentTreeNode = new TreeNode();
        currentTreeNode.setNodeInfo(nodeInfo);
        Integer insertIndex = commonUtil.getInsertIndex(treeNode.getChildren(), nodeInfo.getOrder());
        treeNode.getChildren().add(insertIndex, currentTreeNode);
        // 判断是否存在children，没有或者为数量为空，则返回上一层，有则进行数组JSON解析，并迭代
        String childrenJson = data.get(NodeFieldConstant.CHILDREN_FIELD_NAME, String.class);
        if (!"null".equals(childrenJson)) {
            List<JSONObject> childrens = new JSONArray(childrenJson).toList(JSONObject.class);
            for (JSONObject child : childrens) {
                getOrganizationChildren(currentTreeNode, child, providerId, nodeInfo.getNodeId());
            }
        }
    }

    public NodeInfo buildNodeInfoByJSON(JSONObject data, String providerId, Integer fatherId) {
        Integer type = data.get(NodeFieldConstant.TYPE_FIELD_NAME, Integer.class);
        Integer id = data.get(NodeFieldConstant.ID_FIELD_NAME, Integer.class);
        String name = data.get(NodeFieldConstant.NAME_FIELD_NAME, String.class);
        Integer order = data.get(NodeFieldConstant.ORDER_FIELD_NAME, Integer.class);
        NodeInfo nodeInfo = NodeInfo.builder().type(type).nodeId(id).name(name).order(order).fatherId(fatherId).providerId(providerId).build();
        return nodeInfo;
    }

    public Boolean checkAuthority(NodeInfo nodeInfo, String providerId, Integer userId) {
        if( StringUtils.isEmpty(nodeInfo) ){
            return false;
        }
        // 校验当前节点上有没有权限
        MapUserNode mapUserNode = mapUserNodeMapper.selectOne(providerId, userId, nodeInfo.getId());
        if( StringUtils.isEmpty(mapUserNode) ){
            if( nodeInfo.getFatherId() == 0 ){
                nodeInfo.setProviderId("");
            }
            NodeInfo node = nodeInfoService.selectByNodeId(nodeInfo);
            return checkAuthority(node,providerId,userId);
        }
        // 有就返回true
        if( ApiOperationConstant.AUTHORITY_MANAGER_VALUE.equals(mapUserNode.getIsManage()) ){
            return true;
        }
        return false;
    }
}
