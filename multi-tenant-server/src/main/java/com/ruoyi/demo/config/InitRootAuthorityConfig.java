package com.ruoyi.demo.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.constant.RedisConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.RootUser;
import com.ruoyi.demo.domain.TreeNode;
import com.ruoyi.demo.service.MapUserNodeService;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.service.RootUserService;
import com.ruoyi.demo.util.ApiOperationUtil;
import com.ruoyi.demo.util.BuildTreeUtil;
import com.ruoyi.framework.redis.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    @Autowired
    private RootUserService rootUserService;
    @Autowired
    private BuildTreeUtil buildTreeUtil;

    @PostConstruct
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
        for (JSONObject organizationInfo : organizationInfos) {
            String providerId = organizationInfo.get("id").toString();
            JSONObject root = new JSONObject(organizationInfo.get("root"));
            Integer id = Integer.parseInt(root.get("id").toString());
            String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, providerId, id);
            // 进入递归收集
            getOrganizationChildren(tempList, new JSONObject(organizationChildren), providerId, 0);
        }
        // 这里是存在逻辑。进行数据对比，获取不同的数据
        // 差集 (list1 - list2)
        List<NodeInfo> reduce1 = tempList.stream().filter(item -> !nodeList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce1 (list1 - list2)---：{}", reduce1);
        // 判断有没有新增节点，插入后进行有则进行权限授予，无则跳过
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
            if (!reduce1.isEmpty()) {
                // 获取主要岗位

                //


                grantedPermissionsByNodeList(rootUser.getProviderId(), rootUser.getUserId(), ApiOperationConstant.AUTHORITY_MANAGER, reduce1);
            }
        }
        // 看看需要删除多少节点
        // 差集 (list2 - list1)
        List<NodeInfo> reduce2 = nodeList.stream().filter(item -> !tempList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce2 (list2 - list1)---:{}", reduce2);
        // 判断有没有删减节点，有则进行授权删除以及节点删除，无则跳过
        List<Integer> ids = reduce2.stream().map(NodeInfo::getId).collect(Collectors.toList());
        if (!reduce2.isEmpty()) {
            // 删除授权
            mapUserNodeService.deleteByNodeIds(ids);
            // 删除节点
            nodeInfoService.deleteByIds(ids);
        }
        // 第四步，根据Root账号去遍历出他们被授权的节点结合
        for (RootUser rootUser : rootUsers) {
            List<NodeInfo> nodeInfos = nodeInfoService.selectByMap(rootUser.getProviderId(), rootUser.getUserId());
            // 第五步：根据此集合构建树结构
            String treeJSON = buildTreeUtil.buildShowTree(nodeInfos);
            // 第六步：存放redis即可
            String key = RedisConstant.REDIS_USER_TREE_PREFIX + rootUser.getProviderId() + ":" + rootUser.getUserId();
            BuildTreeUtil.rootTree.put(key,treeJSON);
//            stringRedisTemplate.opsForValue().set(key, treeJSON);
        }
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
                getOrganizationChildren(nodes, child, providerId, nodeInfo.getNodeId());
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
}
