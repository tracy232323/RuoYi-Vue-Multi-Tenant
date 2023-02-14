package com.ruoyi.demo.util;

import cn.hutool.json.JSONUtil;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.TreeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: BuildTreeUtil
 * @Description: 构建树工具类
 * @Author: CodeDan
 * @Date: 2023/2/9 18:33
 * @Version: 1.0.0
 **/
@Component
@Slf4j
public class BuildTreeUtil {

    @Autowired
    private CommonUtil commonUtil;

    public static Map<String,String> rootTree = new HashMap<String,String>();

    public String buildShowTree(List<NodeInfo> analogData) {
        // 根据查询出来的授权节点进行树的构建
        // 获取一下此树的根节点id
        Iterator<NodeInfo> iterator = analogData.iterator();
        HashMap<Integer, NodeInfo> allNodeInfoMap = new HashMap<>();
        while (iterator.hasNext()) {
            NodeInfo next = iterator.next();
            if(ApiOperationConstant.TYPE_POSITION.equals(next.getType()) ){
                continue;
            }
            allNodeInfoMap.put(next.getNodeId(), next);
        }
        Integer rootNodeId = 0;
//        if (analogData.size() >= 1) {
//            NodeInfo nodeInfo = analogData.get(0);
//            rootNodeId = getRootNodeId(allNodeInfoMap, nodeInfo.getNodeId());
//        }
        Map<Integer, TreeNode> nodeInfoMap = new HashMap<Integer, TreeNode>();
        for (NodeInfo nodeInfo : analogData) {
            buildShowTree(allNodeInfoMap, nodeInfoMap, nodeInfo.getNodeId());
        }
        TreeNode treeNode = nodeInfoMap.get(rootNodeId);
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
        log.info("当前id{},对应节点:{}",id,nodeInfo);
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
        Integer insertIndex = commonUtil.getInsertIndex(fatherNode.getChildren(), nodeInfo.getOrder());
        fatherNode.getChildren().add(insertIndex,treeNode);
    }
}
