package com.ruoyi.demo.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.*;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.demo.mapper.NodeOperLogMapper;
import com.ruoyi.demo.mapper.PositionOperLogMapper;
import com.ruoyi.demo.service.MapUserNodeService;
import com.ruoyi.demo.service.NodeInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @ClassName: CommonUtil
 * @Description: 通用方法工具类
 * @Author: CodeDan
 * @Date: 2023/2/10 16:10
 * @Version: 1.0.0
 **/
@Component
public class CommonUtil {

    public static Map<String,List<UserInfo>> providerUsers = new HashMap<String,List<UserInfo>>();

    @Autowired
    private NodeOperLogMapper nodeOperLogMapper;

    @Autowired
    private PositionOperLogMapper positionOperLogMapper;

    @Autowired
    private NodeInfoService nodeInfoService;

    private static NodeInfo rootNode;

    /**
     * 根据获取的组织列表，构建一个用户所在组织路径
     * @param data 组织列表
     * @return 用户所在组织路径
     */
    public String buildUserPathFromTree(String data){
        List<JSONObject> nodeList = new JSONArray(data).toList(JSONObject.class);
        StringBuilder str = new StringBuilder();
        for(JSONObject node : nodeList) {
            if( node.containsKey("virtual") && Boolean.TRUE.equals(node.get("virtual",Boolean.class))){
                continue;
            }
            str.insert(0,node.get("name"));
            str.insert(0,"/");
        }
        str = str.deleteCharAt(0);
        return str.toString();
    }

    public Integer getInsertIndex(List<TreeNode> list, Integer order){
        if( list.isEmpty() ){
            return 0;
        }
        int leftIndex = 0;
        int rightIndex = list.size() - 1;
        while( leftIndex <= rightIndex ){
            int mid = leftIndex + ((rightIndex - leftIndex) / 2);
            if (list.get(mid).getOrder() > order) {
                // target 在左区间，所以[low, mid - 1]
                rightIndex = mid - 1;
            } else{
                // target 在右区间，所以[mid + 1, high]
                leftIndex = mid + 1;
            }
        }
        return rightIndex + 1;
    }

    public NodeInfo getRootNode() {
        if(!StringUtils.isEmpty(rootNode)){
            return rootNode;
        }
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(1);
        nodeInfo.setNodeId(0);
        nodeInfo.setFatherId(-1);
        nodeInfo.setName("总公司");
        nodeInfo.setOrder(1);
        nodeInfo.setProviderId("demo");
        nodeInfo.setType(1);
        rootNode = nodeInfo;
        return nodeInfo;
    }

    /**
     * 解析组织树，获取每个节点的信息
     * @param nodes      节点集合
     * @param data       数据
     * @param providerId
     * @param id
     */
    public void getOrganizationChildren(List<NodeInfo> nodes, JSONObject data, String providerId, Integer id) {
        Integer type = data.get(NodeFieldConstant.TYPE_FIELD_NAME, Integer.class);
        if (ApiOperationConstant.TYPE_POSITION.equals(type)) {
            NodeInfo nodeInfo = buildNodeInfoByJSON(data, providerId, id);
            nodes.add(nodeInfo);
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


    public void getNodeAllLog(List<NodeOperLog> nodeOperLogs, List<PositionOperLog> positionOperLogs, List<NodeInfo> deleteModeInfos, NodeInfo node){
        // 插入当前节点的删除日志
        deleteModeInfos.add(node);
        // 获取当前节点的两个日志，并存储
        List<NodeOperLog> currentNodeOperLogs = nodeOperLogMapper.selectListByNodeId(node.getNodeId());
        List<PositionOperLog> currentPositionOperLogs = positionOperLogMapper.selectListByNodeId(node.getNodeId());
        if( !currentNodeOperLogs.isEmpty() ){
            nodeOperLogs.addAll(currentNodeOperLogs);
        }
        if( !currentPositionOperLogs.isEmpty()){
            positionOperLogs.addAll(currentPositionOperLogs);
        }
        List<NodeInfo> childrenNodeInfos = nodeInfoService.selectListByFatherId(node);
        Iterator<NodeInfo> iterator = childrenNodeInfos.iterator();
        while(  iterator.hasNext() ){
            NodeInfo next = iterator.next();
            getNodeAllLog(nodeOperLogs, positionOperLogs, deleteModeInfos, next);
        }
    }


    public void organizationAndUserAll(List<UserInfo> userInfos, JSONObject data, String providerId, StringBuilder path, String dept) {
        StringBuilder pathStr = new StringBuilder(path);
        Integer type = data.get(NodeFieldConstant.TYPE_FIELD_NAME, Integer.class);
        String name = data.get(NodeFieldConstant.NAME_FIELD_NAME, String.class);
        if(!ApiOperationConstant.TYPE_POSITION.equals(type)){
            if( ApiOperationConstant.TYPE_DEPT.equals(type) ){
                dept = name;
            }
            String virtual = data.get(NodeFieldConstant.VIRTUAL_FIELD_NAME,String.class);
            if( "false".equals(virtual) ){
                pathStr.append("/").append(name);
            }
            String childrenJson = data.get(NodeFieldConstant.CHILDREN_FIELD_NAME, String.class);
            if (!"null".equals(childrenJson)) {
                List<JSONObject> childrens = new JSONArray(childrenJson).toList(JSONObject.class);
                for (JSONObject child : childrens) {
                    organizationAndUserAll(userInfos, child,providerId, pathStr, dept);
                }
            }
        }
        String childrenJson = data.get(NodeFieldConstant.USERS_FIELD_NAME, String.class);
        if (!"null".equals(childrenJson)) {
            List<JSONObject> childrens = new JSONArray(childrenJson).toList(JSONObject.class);
            Integer nodeId = data.get(NodeFieldConstant.ID_FIELD_NAME, Integer.class);
            for (JSONObject child : childrens) {
                //读取岗位信息，并开始收集用户数据
                UserInfo userInfo = new UserInfo();
                userInfo.setPosition(name);
                userInfo.setNodeId(nodeId);
                userInfo.setProviderId(providerId);
                userInfo.setDept(dept);
                Integer userId = child.get(NodeFieldConstant.ID_FIELD_NAME, Integer.class);
                userInfo.setUserId(userId);
                String userName = child.get(NodeFieldConstant.NAME_FIELD_NAME, String.class);
                userInfo.setName(userName);
                userInfo.setUnit(path + " " + userName);
                userInfos.add(userInfo);
            }
        }
    }
}
