package com.ruoyi.demo.util;

import cn.hutool.json.JSONUtil;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.TreeNode;
import com.ruoyi.demo.service.NodeInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

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
    @Autowired
    private NodeInfoService nodeInfoService;

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
        Map<Integer, TreeNode> nodeInfoMap = new HashMap<Integer, TreeNode>();
        for (NodeInfo nodeInfo : analogData) {
            buildShowTree(allNodeInfoMap, nodeInfoMap, nodeInfo.getNodeId());
        }
        TreeNode treeNode = nodeInfoMap.get(rootNodeId);
        return JSONUtil.toJsonStr(treeNode);
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

    public boolean saveGrandfatherNode(HashMap<IdObject, NodeInfo> allNodeInfoMap, NodeInfo nodeInfo) {
        NodeInfo node = nodeInfoService.selectByNodeId(nodeInfo);
        if (StringUtils.isEmpty(node)) {
            return true;
        }
        if (allNodeInfoMap.containsKey(new IdObject(node.getNodeId(), node.getProviderId()))) {
            return false;
        }
        return saveGrandfatherNode(allNodeInfoMap, node);
    }

    /**
     * 移除一棵树中存在的多个节点，只保留相对根节点
     * @param nodeInfos
     */
    public void removeUnimportantNode(List<NodeInfo> nodeInfos){
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
            boolean b = saveGrandfatherNode(infoHashMap, next.getValue());
            if (!b) {
                nodeInfos.remove(next);
            }
        }
    }
}
