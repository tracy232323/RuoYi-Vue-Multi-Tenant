package com.ruoyi.demo.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.constant.RedisConstant;
import com.ruoyi.demo.domain.*;
import com.ruoyi.demo.mapper.NodeOperLogMapper;
import com.ruoyi.demo.mapper.PositionOperLogMapper;
import com.ruoyi.demo.mapper.UserInfoMapper;
import com.ruoyi.demo.service.MapUserNodeService;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.service.RootUserService;
import com.ruoyi.demo.util.ApiOperationUtil;
import com.ruoyi.demo.util.BuildTreeUtil;
import com.ruoyi.demo.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: InitTest
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/15 09:38
 * @Version: 1.0.0
 **/
@Configuration
@Slf4j
public class InitRootTreeConfig {
    @Autowired
    private ApiOperationUtil apiOperationUtil;
    @Autowired
    private NodeInfoService nodeInfoService;
    @Autowired
    private MapUserNodeService mapUserNodeService;
    @Autowired
    private RootUserService rootUserService;
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private BuildTreeUtil buildTreeUtil;
    @Autowired
    private NodeOperLogMapper nodeOperLogMapper;
    @Autowired
    private PositionOperLogMapper positionOperLogMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;

//    @PostConstruct
    public void init() {
        apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
        List<NodeInfo> nodeList = nodeInfoService.selectAll();
        String allOrganizationInfo = apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        List<JSONObject> organizationInfos = new JSONArray(allOrganizationInfo).toList(JSONObject.class);
        List<NodeInfo> tempList = new ArrayList<>();
        List<String> providerIds =  new ArrayList<>();
        tempList.add(commonUtil.getRootNode());
        for (JSONObject organizationInfo : organizationInfos) {
            String providerId = organizationInfo.get("id").toString();
            providerIds.add(providerId);
            JSONObject root = new JSONObject(organizationInfo.get("root"));
            Integer id = Integer.parseInt(root.get("id").toString());
            String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, providerId, id);
            // 进入递归收集
            commonUtil.getOrganizationChildren(tempList, new JSONObject(organizationChildren), providerId, 0);
        }
        // 这里是存在逻辑。进行数据对比，获取不同的数据
        // 看看需要删除多少节点
        // 差集 (list2 - list1)
        List<NodeInfo> reduce2 = nodeList.stream().filter(item -> !tempList.contains(item)).collect(Collectors.toList());
        log.info("---得到更新时的差集 reduce2 (list2 - list1)---:{}", reduce2);
        // 判断有没有删减节点，有则进行授权删除以及节点删除，无则跳过
        List<Integer> ids = reduce2.stream().map(NodeInfo::getId).collect(Collectors.toList());
        if (!reduce2.isEmpty()) {
            // 先去掉当前差集中，属于相同树中的节点，只保留此删除树形结构的相对根节点即可。
            buildTreeUtil.removeUnimportantNode(reduce2);
            Iterator<NodeInfo> iterator = reduce2.iterator();
            while( iterator.hasNext() ) {
                NodeInfo next = iterator.next();
                // 获取此节点的上一个节点，作为日志转移节点
                NodeInfo fatherNode = nodeInfoService.selectNodeIdByFatherId(next);
                // 迭代当前节点下的树叶以及树支节点，获取他们的历史变更日志以及历史节点删除日志，将其都修改到fatherNode节点下
                ArrayList<NodeOperLog> nodeOperLogs = new ArrayList<>();
                ArrayList<PositionOperLog> positionOperLogs = new ArrayList<>();
                ArrayList<NodeInfo> deleteNodeInfos = new ArrayList<>();
                commonUtil.getNodeAllLog(nodeOperLogs,positionOperLogs,deleteNodeInfos,next);
                // 将节点日志修改到父亲节点下
                Iterator<NodeOperLog> nodeOperLogIterator = nodeOperLogs.iterator();
                while( nodeOperLogIterator.hasNext()){
                    NodeOperLog nodeOperLog = nodeOperLogIterator.next();
                    nodeOperLog.setNodeId(fatherNode.getId());
                    // 更新
                    nodeOperLogMapper.updateById(nodeOperLog);
                }
                // 将岗位日志修改到父亲节点下
                Iterator<PositionOperLog> positionOperLogIterator = positionOperLogs.iterator();
                while( positionOperLogIterator.hasNext()){
                    PositionOperLog positionOperLog = positionOperLogIterator.next();
                    positionOperLog.setNodeId(fatherNode.getId());
                    // 更新
                    positionOperLogMapper.updateById(positionOperLog);
                }
                // 记录这些节点的删除到父亲节点的历史删除日志中
                Iterator<NodeInfo> deleteNodeOperIterator = deleteNodeInfos.iterator();
                while( deleteNodeOperIterator.hasNext()){
                    NodeInfo nodeInfo = deleteNodeOperIterator.next();
                    NodeOperLog nodeOperLog = new NodeOperLog();
                    nodeOperLog.setNodeId(fatherNode.getId());
                    nodeOperLog.setContext("节点："+nodeInfo.getName()+"被删除");
                    // 添加
                    nodeOperLogMapper.add(nodeOperLog);
                }
            }
            // 删除授权
            mapUserNodeService.deleteByNodeIds(ids);
            // 删除节点
            nodeInfoService.deleteByIds(ids);
        }
        // 差集 (list1 - list2)
        List<NodeInfo> reduce1 = tempList.stream().filter(item -> !nodeList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce1 (list1 - list2)---：{}", reduce1);
        // 判断有没有新增节点，插入
        if (!reduce1.isEmpty()) {
            // 开始解析组织树
            if (reduce1.size() > 1000) {
                List<List<NodeInfo>> partitions = Lists.partition(reduce1, 1000);
                for (List<NodeInfo> partition : partitions) {
                    nodeInfoService.insertBatch(partition);
                }
            } else {
                nodeInfoService.insertBatch(reduce1);
            }
        }
        // 查询有多少个root账号,就给他们都授权了
        List<RootUser> rootUsers = rootUserService.selectAll();
        for (RootUser rootUser : rootUsers) {
            // 获取主要岗位
            String mainPositionByUser = apiOperationUtil.getMainPositionByUser(ApiOperationConstant.GET_MAIN_POSITION_URL, rootUser.getProviderId(), rootUser.getUserId());
            Integer id = new JSONObject(mainPositionByUser).get("id", Integer.class);
            // 根据主要岗位获取路径
            String orgPath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, rootUser.getProviderId(), id);
            String path = commonUtil.buildUserPathFromTree(orgPath);
            // 获取当前用户名称
            String userInfo = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, rootUser.getProviderId(), rootUser.getUserId());
            String name = new JSONObject(userInfo).get("name", String.class);
            // 拼接获取path
            path = path + " " + name;
            grantedPermissionsByNodeList(rootUser.getProviderId(), rootUser.getUserId(), ApiOperationConstant.AUTHORITY_MANAGER, commonUtil.getRootNode(), path,id);
        }
//        log.info("---得到差集 reduce2 (list2 - list1)---:{}", reduce2);
//        // 判断有没有删减节点，有则进行授权删除以及节点删除，无则跳过
//        List<Integer> ids = reduce2.stream().map(NodeInfo::getId).collect(Collectors.toList());
//        if (!reduce2.isEmpty()) {
//            // 删除授权
//            mapUserNodeService.deleteByNodeIds(ids);
//            // 删除节点
//            nodeInfoService.deleteByIds(ids);
//        }
        // 第四步，根据Root账号去遍历出他们被授权的节点结合
        TreeNode treeNode = new TreeNode();
        treeNode.setNodeInfo(commonUtil.getRootNode());
        for (RootUser rootUser : rootUsers) {
            // 第五步：根据此集合构建树结构
            log.info("正在构建树");
            String treeJSON = buildShowTree(commonUtil.getRootNode());
            log.info("构建完成");
            // 第六步：存放内存即可
            String key = RedisConstant.REDIS_USER_TREE_PREFIX + rootUser.getProviderId() + ":" + rootUser.getUserId();
            BuildTreeUtil.rootTree.put(key, treeJSON);
        }
        for (String providerId : providerIds) {
            // 获取hr系统中二级节点下的完整路径。去迭代遍历并收集用户信息
            log.info("正在构建:{}的用户缓存",providerId);
            CommonUtil.providerUsers.put(providerId,userInfoMapper.selectListByProviderId(providerId));
        }
    }


    public void grantedPermissionsByNodeList(String providerId, Integer userId, String type, NodeInfo nodeInfo, String path,Integer positionId) {
        // 使用iterator在大数据量时，效率高
        MapUserNode node = mapUserNodeService.selectOne(providerId, userId, nodeInfo.getId());
        if (!StringUtils.isEmpty(node)) {
            return;
        }
        MapUserNode mapUserNode = new MapUserNode();
        mapUserNode.setCompanyId(providerId);
        mapUserNode.setUserId(userId);
        mapUserNode.setPosProviderId(providerId);
        mapUserNode.setPosId(positionId);
        mapUserNode.setNodeId(nodeInfo.getId());
        mapUserNode.setPath(path);
        // 判断授权
        if (ApiOperationConstant.AUTHORITY_MANAGER.equals(type)) {
            mapUserNode.setIsManage(ApiOperationConstant.AUTHORITY_MANAGER_VALUE);
            mapUserNode.setIsShow(ApiOperationConstant.AUTHORITY_SHOW_VALUE);
        } else if (ApiOperationConstant.AUTHORITY_SHOW.equals(type)) {
            mapUserNode.setIsManage(ApiOperationConstant.AUTHORITY_NOT_MANAGER_VALUE);
            mapUserNode.setIsShow(ApiOperationConstant.AUTHORITY_SHOW_VALUE);
        } else {
            mapUserNode.setIsManage(ApiOperationConstant.AUTHORITY_NOT_MANAGER_VALUE);
            mapUserNode.setIsShow(ApiOperationConstant.AUTHORITY_NOT_SHOW_VALUE);
        }
        mapUserNodeService.insertBatch(Arrays.asList(mapUserNode));
    }

    public String buildShowTree(NodeInfo rootNode) {
        List<NodeInfo> nodeInfos = nodeInfoService.selectListByFatherId(rootNode);
        Iterator<NodeInfo> nodeInfoIterator = nodeInfos.iterator();
        TreeNode rootTree = new TreeNode();
        rootTree.setNodeInfo(rootNode);
        while (nodeInfoIterator.hasNext()) {
            NodeInfo node = nodeInfoIterator.next();
            buildShowTree(node,rootTree);
        }
        return JSONUtil.toJsonStr(rootTree);
    }

    public void buildShowTree(NodeInfo nodeInfo,TreeNode rootNode) {
        String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, nodeInfo.getProviderId(), nodeInfo.getNodeId());
        // 遍历organizationChildren将其封装为TreeNode
        JSONObject childrenJSON = new JSONObject(organizationChildren);
        getOrganizationChildren(rootNode,childrenJSON,nodeInfo.getProviderId(),commonUtil.getRootNode().getNodeId());
    }

    public void getOrganizationChildren(TreeNode treeNode, JSONObject data, String providerId, Integer id) {
        Integer type = data.get(NodeFieldConstant.TYPE_FIELD_NAME, Integer.class);
        if (ApiOperationConstant.TYPE_POSITION.equals(type)) {
            return ;
        }
        // 提取字段组成NodeInfo
        NodeInfo nodeInfo = commonUtil.buildNodeInfoByJSON(data, providerId, id);
        TreeNode currentTreeNode = new TreeNode();
        currentTreeNode.setNodeInfo(nodeInfo);
        Integer insertIndex = commonUtil.getInsertIndex(treeNode.getChildren(), nodeInfo.getOrder());
        treeNode.getChildren().add(insertIndex,currentTreeNode);
        // 判断是否存在children，没有或者为数量为空，则返回上一层，有则进行数组JSON解析，并迭代
        String childrenJson = data.get(NodeFieldConstant.CHILDREN_FIELD_NAME, String.class);
        if (!"null".equals(childrenJson)) {
            List<JSONObject> childrens = new JSONArray(childrenJson).toList(JSONObject.class);
            for (JSONObject child : childrens) {
                getOrganizationChildren(currentTreeNode, child, providerId, nodeInfo.getNodeId());
            }
        }
    }
}
