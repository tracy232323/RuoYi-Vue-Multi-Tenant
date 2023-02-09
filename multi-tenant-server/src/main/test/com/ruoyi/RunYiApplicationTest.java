package com.ruoyi;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.TreeNode;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.util.ApiOperationUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @ClassName: RunYiApplicationTest
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/7 11:51
 * @Version: 1.0.0
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class RunYiApplicationTest {

    @Autowired
    private ApiOperationUtil apiOperationUtil;


    @Test
    public void test1() {

    }

    @Test
    public void test() {
        String token = apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
        log.info("Token: {}", token);
        String allOrganizationInfo = apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        List<JSONObject> organizationInfos = new JSONArray(allOrganizationInfo).toList(JSONObject.class);
        JSONObject jsonObject = organizationInfos.get(0);
        String mainId = jsonObject.get("id").toString();
        JSONObject root = new JSONObject(jsonObject.get("root"));
        String id = root.get("id").toString();
        log.info("mainId: {}", mainId);
        log.info("id: {}", id);
//        apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL,mainId,id);

    }


    @Autowired
    private NodeInfoService nodeInfoService;

    @Test
    public void init() {
        int number = 0;
        apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
        //TODO: 简单逻辑处理，后期待优化
//        List<NodeInfo> nodeList = nodeInfoService.list();
//        if( nodeList.isEmpty() ){
        // 开始解析组织树
        String allOrganizationInfo = apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        List<JSONObject> organizationInfos = new JSONArray(allOrganizationInfo).toList(JSONObject.class);
        for (JSONObject organizationInfo : organizationInfos) {
            List<NodeInfo> tempList = new ArrayList<NodeInfo>();
            String providerId = organizationInfo.get("id").toString();
            JSONObject root = new JSONObject(organizationInfo.get("root"));
            Integer id = Integer.parseInt(root.get("id").toString());
            String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, providerId, id);
            // 进入递归收集
            getOrganizationChildren(tempList, new JSONObject(organizationChildren), providerId, id);
            if (!tempList.isEmpty()) {
                log.info("完成节点节点的数据集合:{}", id);
                number += tempList.size();
//                    nodeInfoService.saveBatch(tempList);
            }
//            }
        }
        log.info("number:{}", number);
    }

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
        if (ObjectUtils.isEmpty(childrenJson)) {
            log.info("---------------------------childeren-----------------");
        }
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

    public List<NodeInfo> getAnalogData() {
        List<NodeInfo> nodeInfos = new ArrayList<>();
        NodeInfo nodeInfo1 = new NodeInfo(1, 1, 0, "张凌迪", 1, "hr");
        NodeInfo nodeInfo1_1 = new NodeInfo(2, 1, 1, "张凌迪1-1", 1, "hr");
        NodeInfo nodeInfo1_2 = new NodeInfo(3, 1, 1, "张凌迪1-2", 1, "hr");
        NodeInfo nodeInfo2_1 = new NodeInfo(4, 1, 2, "张凌迪2-1", 1, "hr");
        NodeInfo nodeInfo2_2 = new NodeInfo(5, 1, 2, "张凌迪2-2", 1, "hr");
        NodeInfo nodeInfo3_1 = new NodeInfo(6, 1, 3, "张凌迪3-1", 1, "hr");
        NodeInfo nodeInfo3_2 = new NodeInfo(7, 1, 3, "张凌迪3-2", 1, "hr");
        nodeInfos.add(nodeInfo1);
        nodeInfos.add(nodeInfo1_1);
        nodeInfos.add(nodeInfo1_2);
        nodeInfos.add(nodeInfo2_1);
        nodeInfos.add(nodeInfo2_2);
        nodeInfos.add(nodeInfo3_1);
        nodeInfos.add(nodeInfo3_2);
        return nodeInfos;
    }
    @Test
    public void buildShowTree() {
        // 查询当前用户所在岗位集合

        // 根据用户以及岗位查询出存在授权的节点集合

        // 根据查询出来的授权节点进行树的构建
        List<NodeInfo> analogData = getAnalogData();
        // 获取一下此树的根节点id
        Iterator<NodeInfo> iterator = analogData.iterator();
        HashMap<Integer, NodeInfo> allNodeInfoMap = new HashMap<>();
        while (iterator.hasNext()) {
            NodeInfo next = iterator.next();
            allNodeInfoMap.put(next.getNodeId(), next);
        }
        Integer rootNodeId = 0;
        if( analogData.size() >= 1 ){
            NodeInfo nodeInfo = analogData.get(0);
            rootNodeId = getRootNodeId(allNodeInfoMap, nodeInfo.getNodeId());
        }
        Map<Integer, TreeNode> nodeInfoMap = new HashMap<Integer, TreeNode>();
        for (NodeInfo nodeInfo : analogData) {
            buildShowTree(allNodeInfoMap,nodeInfoMap,nodeInfo.getNodeId());
        }
        TreeNode treeNode = nodeInfoMap.get(rootNodeId);
        log.info("Root:{}",JSONUtil.toJsonStr(treeNode));
//        return JSONUtil.toJsonStr(treeNode);
    }

    public Integer getRootNodeId(HashMap<Integer, NodeInfo> allNodeInfoMap,Integer id) {
        NodeInfo nodeInfo = allNodeInfoMap.get(id);
        if( NodeFieldConstant.ROOT_NODE.equals(nodeInfo.getFatherId()) ){
            return nodeInfo.getNodeId();
        }
        return getRootNodeId(allNodeInfoMap,nodeInfo.getFatherId());
    }

    public void buildShowTree(HashMap<Integer, NodeInfo> allNodeInfoMap, Map<Integer, TreeNode> nodeInfoMap, Integer id) {
        NodeInfo nodeInfo = allNodeInfoMap.get(id);
        Integer fatherId = nodeInfo.getFatherId();
        // 定义递归的出口( 当节点的父亲节点为0或者在nodeInfoMap中已存在 )
        if ( nodeInfoMap.containsKey(id) ){
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
        buildShowTree(allNodeInfoMap,nodeInfoMap,fatherId);
        TreeNode fatherNode = nodeInfoMap.get(fatherId);
        fatherNode.getChildren().add(treeNode);
    }
}
