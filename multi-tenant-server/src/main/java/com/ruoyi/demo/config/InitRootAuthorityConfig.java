package com.ruoyi.demo.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.TreeNode;
import com.ruoyi.demo.service.MapUserNodeService;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.util.ApiOperationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: InitRootAuthorityConfig
 * @Description: 项目启动时，进行Root账户的初始化
 * @Author: CodeDan
 * @Date: 2023/2/8 09:19
 * @Version: 1.0.0
 **/
@Configuration
@Slf4j
public class InitRootAuthorityConfig {

    @Autowired
    private ApiOperationUtil apiOperationUtil;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private MapUserNodeService mapUserNodeService;

//    @PostConstruct
    public void init() {
        apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
        //TODO: 简单逻辑处理，后期待优化
        List<NodeInfo> nodeList = nodeInfoService.selectAll();
        String allOrganizationInfo = apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        List<JSONObject> organizationInfos = new JSONArray(allOrganizationInfo).toList(JSONObject.class);
        List<NodeInfo> tempList = new ArrayList<NodeInfo>();
        for (JSONObject organizationInfo : organizationInfos) {
            String providerId = organizationInfo.get("id").toString();
            JSONObject root = new JSONObject(organizationInfo.get("root"));
            Integer id = Integer.parseInt(root.get("id").toString());
            String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, providerId, id);
            // 进入递归收集
            getOrganizationChildren(tempList, new JSONObject(organizationChildren), providerId, id);
        }
        if (nodeList.isEmpty() && !tempList.isEmpty()) {
            // 开始解析组织树
            if (tempList.size() > 1000) {
                List<List<NodeInfo>> partitions = Lists.partition(tempList, 1000);
                for (List<NodeInfo> partition : partitions) {
                    nodeInfoService.insertBatch(partition);
                }
            } else {
                nodeInfoService.insertBatch(tempList);
            }
        }
        //TODO:查询有多少个root账号


        // 这里是存在逻辑。进行数据对比，获取不同的数据
        // 差集 (list1 - list2)
        List<NodeInfo> reduce1 = tempList.stream().filter(item -> !nodeList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce1 (list1 - list2)---：{}",reduce1);
        // 判断有没有新增节点，有则进行权限授予，无则跳过
        // 给予Root账号全部节点的权限
        if( !reduce1.isEmpty() ){

            grantedPermissionsByNodeList("hr", 828046, ApiOperationConstant.AUTHORITY_MANAGER, reduce1);

        }

        // 差集 (list2 - list1)
        List<NodeInfo> reduce2 = nodeList.stream().filter(item -> !tempList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce2 (list2 - list1)---:{}",reduce2);
        // 判断有没有删减节点，有则进行授权删除以及节点删除，无则跳过
        //TODO:等待删除接口

        // 第四步，根据Root账号去遍历出他们被授权的节点结合

        // 第五步：根据此集合构建树结构
//        String treeJson = buildShowTree();

    }

    public void grantedPermissionsByNodeList(String providerId, Integer userId, String type, List<NodeInfo> nodeInfos) {
        // 使用iterator在大数据量时，效率高
        Iterator<NodeInfo> iterator = nodeInfos.iterator();
        List<MapUserNode> tempList = new ArrayList<>();
        while (iterator.hasNext()) {
            MapUserNode mapUserNode = new MapUserNode();
            NodeInfo next = iterator.next();
            mapUserNode.setCompanyId(providerId);
            mapUserNode.setUserId(userId);
            mapUserNode.setNodeId(next.getId());
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
            tempList.add(mapUserNode);
        }
        if (tempList.size() > 1000) {
            List<List<MapUserNode>> partitions = Lists.partition(tempList, 1000);
            for (List<MapUserNode> partition : partitions) {
                mapUserNodeService.insertBatch(partition);
            }
        }
    }

    /**
     * 解析组织树，获取每个节点的信息
     *
     * @param nodes      节点集合
     * @param data       数据
     * @param providerId
     * @param id
     */
    public void getOrganizationChildren(List<NodeInfo> nodes, JSONObject data, String providerId, Integer id) {
        boolean key = data.containsKey(NodeFieldConstant.CHILDREN_FIELD_NAME);
        Integer type = data.get(NodeFieldConstant.TYPE_FIELD_NAME, Integer.class);
        if (!key || ApiOperationConstant.TYPE_POSITION.equals(type)) {
            return;
        }
        // 提取字段组成NodeInfo
        NodeInfo nodeInfo = buildNodeInfoByJSON(data, providerId, id);
        // 写入nodes中
        nodes.add(nodeInfo);
        // 判断是否存在children，没有或者为数量为空，则返回上一层，有则进行数组JSON解析，并迭代
        String childrenJson = data.get(NodeFieldConstant.CHILDREN_FIELD_NAME, String.class);
        if (!"null".equals(childrenJson)) {
            List<JSONObject> childrens = new JSONArray(childrenJson).toList(JSONObject.class);
            for (JSONObject child : childrens) {
                getOrganizationChildren(nodes, child, providerId, id);
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

    public String buildShowTree(List<NodeInfo> analogData) {
        // 根据查询出来的授权节点进行树的构建
        // 获取一下此树的根节点id
        Iterator<NodeInfo> iterator = analogData.iterator();
        HashMap<Integer, NodeInfo> allNodeInfoMap = new HashMap<>();
        while (iterator.hasNext()) {
            NodeInfo next = iterator.next();
            allNodeInfoMap.put(next.getNodeId(), next);
        }
        Integer rootNodeId = 0;
        if (analogData.size() >= 1) {
            NodeInfo nodeInfo = analogData.get(0);
            rootNodeId = getRootNodeId(allNodeInfoMap, nodeInfo.getNodeId());
        }
        Map<Integer, TreeNode> nodeInfoMap = new HashMap<Integer, TreeNode>();
        for (NodeInfo nodeInfo : analogData) {
            buildShowTree(allNodeInfoMap, nodeInfoMap, nodeInfo.getNodeId());
        }
        TreeNode treeNode = nodeInfoMap.get(rootNodeId);
        log.info("Root:{}", JSONUtil.toJsonStr(treeNode));
        return JSONUtil.toJsonStr(treeNode);
    }

    public Integer getRootNodeId(HashMap<Integer, NodeInfo> allNodeInfoMap, Integer id) {
        NodeInfo nodeInfo = allNodeInfoMap.get(id);
        if (NodeFieldConstant.ROOT_NODE.equals(nodeInfo.getFatherId())) {
            return nodeInfo.getNodeId();
        }
        return getRootNodeId(allNodeInfoMap, nodeInfo.getFatherId());
    }

    public void buildShowTree(HashMap<Integer, NodeInfo> allNodeInfoMap, Map<Integer, TreeNode> nodeInfoMap, Integer id) {
        NodeInfo nodeInfo = allNodeInfoMap.get(id);
        Integer fatherId = nodeInfo.getFatherId();
        // 定义递归的出口( 当节点的父亲节点为0或者在nodeInfoMap中已存在 )
        if (nodeInfoMap.containsKey(id)) {
            return;
        }
        if (NodeFieldConstant.ROOT_NODE.equals(fatherId)) {
            //加入到节点中
            TreeNode treeNode = new TreeNode();
            treeNode.setNodeInfo(nodeInfo);
            nodeInfoMap.put(nodeInfo.getNodeId(), treeNode);
            return;
        }
        TreeNode treeNode = new TreeNode();
        treeNode.setNodeInfo(nodeInfo);
        nodeInfoMap.put(nodeInfo.getNodeId(), treeNode);
        // 获取父亲节点
        buildShowTree(allNodeInfoMap, nodeInfoMap, fatherId);
        TreeNode fatherNode = nodeInfoMap.get(fatherId);
        fatherNode.getChildren().add(treeNode);
    }
}
