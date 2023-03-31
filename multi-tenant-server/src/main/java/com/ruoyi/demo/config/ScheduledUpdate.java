package com.ruoyi.demo.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.google.common.collect.Lists;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.*;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.demo.mapper.NodeOperLogMapper;
import com.ruoyi.demo.mapper.PositionOperLogMapper;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.service.NodeOperLogService;
import com.ruoyi.demo.service.PositionOperLogService;
import com.ruoyi.demo.util.ApiOperationUtil;
import com.ruoyi.demo.util.BuildTreeUtil;
import com.ruoyi.demo.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: ScheduledUpdate
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/23 17:47
 * @Version: 1.0.0
 **/
@Component
@Slf4j
@EnableScheduling
public class ScheduledUpdate {
    @Autowired
    private MapUserNodeMapper mapUserNodeMapper;
    @Autowired
    private ApiOperationUtil apiOperationUtil;
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private NodeInfoService nodeInfoService;
    @Autowired
    private PositionOperLogMapper positionOperLogMapper;
    @Autowired
    private NodeOperLogMapper nodeOperLogMapper;
    @Autowired
    private BuildTreeUtil buildTreeUtil;

    /**
     * 定时更新,每隔一个小时执行一次
     */
//    @PostConstruct
//    @Scheduled(cron = "0 0/10 * * * ?")
    @Scheduled(cron = "0 0/5 * * * ?")
    @Transactional
    public void update(){
        // 获取当前被授权节点上的所有用户信息(去重)
        List<MapUserNode> userList = mapUserNodeMapper.selectAllUser();
        // 遍历这些用户信息，并查询当前这个用户的所有岗位信息
        Iterator<MapUserNode> userIterator = userList.iterator();
        while(userIterator.hasNext()){
            MapUserNode user = userIterator.next();
            // 查询岗位信息
            String userAllPosition = apiOperationUtil.getUserAllPosition(ApiOperationConstant.GET_USER_ALL_POSITION_URL, user.getCompanyId(), user.getUserId());
            // 将查询出来的岗位信息转化为岗位id的集合
            List<Integer> positionIds = getPositions(userAllPosition);
            // 检索出当前用户在被授权节点中所拥有的岗位
            List<MapUserNode> nodePositionIds = mapUserNodeMapper.selectPositionListByUser(user.getCompanyId(),user.getUserId());
            // 进行对比(用nodePositionIds去对比positionIds即可，知道当前用户被授权节点上记录的岗位是否发生变化)
            List<MapUserNode> changePositionIds = nodePositionIds.stream().filter(item -> !positionIds.contains(item.getPosId())).collect(Collectors.toList());
            // changePositionIds中记录的岗位id，都是需要进行替换数据的岗位id，由于同步方式无法知道更换到什么岗位上去的了，所以一律换成主岗位
            // 获取当前用户的主岗位
            if( changePositionIds.isEmpty() ){
                continue;
            }
            String mainPositionInfo = apiOperationUtil.getMainPositionByUser(ApiOperationConstant.GET_MAIN_POSITION_URL, user.getCompanyId(), user.getUserId());
            Integer id = new JSONObject(mainPositionInfo).get("id", Integer.class);
            String orgPath = apiOperationUtil.getOrgPath(ApiOperationConstant.GET_ORG_PATH_URL, user.getCompanyId(), id);
            String userInfo = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, user.getCompanyId(), user.getUserId());
            String path = commonUtil.buildUserPathFromTree(orgPath);
            String name = new JSONObject(userInfo).get("name", String.class);
            path = path + " " + name;
            for( MapUserNode changePositionId : changePositionIds ){
                // 去修改并记录
                log.info("修改记录,companyId：{},userId:{},position:{},path:{}",user.getCompanyId(),user.getUserId(),changePositionId.getPosId(),path);
                mapUserNodeMapper.updatePathByPosition(user.getCompanyId(),user.getUserId(),changePositionId.getPosId(),path);
                // 记录日志
                String context = changePositionId.getPath() + " 被修改为 "+ path;
                Integer nodeId = changePositionId.getNodeId();
                log.info("日志记录：node:{},context:{}",nodeId,context);
                positionOperLogMapper.add(nodeId,context);
            }
        }
    }

    /**
     * 定时更新,每隔一个小时执行一次
     */
//    @Scheduled(cron = "0 0 0/2 * * ?")
//    @Scheduled(cron = "0 0/20 * * * ?")
    @Transactional
    public void updateNode(){
        log.info("开始触发节点更新操作");
        List<NodeInfo> nodeList = nodeInfoService.selectAll();
        String allOrganizationInfo = apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        List<JSONObject> organizationInfos = new JSONArray(allOrganizationInfo).toList(JSONObject.class);
        List<NodeInfo> tempList = new ArrayList<>();
        tempList.add(commonUtil.getRootNode());
        for (JSONObject organizationInfo : organizationInfos) {
            String providerId = organizationInfo.get("id").toString();
            JSONObject root = new JSONObject(organizationInfo.get("root"));
            Integer id = Integer.parseInt(root.get("id").toString());
            String organizationChildren = apiOperationUtil.getOrganizationChildren(ApiOperationConstant.GET_ORGANIZATION_CHILDREN_URL, providerId, id);
            // 进入递归收集
            commonUtil.getOrganizationChildren(tempList, new JSONObject(organizationChildren), providerId, 0);
        }
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
            mapUserNodeMapper.deleteByNodeIds(ids);
            // 删除节点
            nodeInfoService.deleteByIds(ids);
        }
    }

    public List<Integer> getPositions(String jsonData) {
        ArrayList<Integer> positionIds = new ArrayList<>();
        List<JSONObject> positions = new JSONArray(jsonData).toList(JSONObject.class);
        for (JSONObject position : positions) {
            Integer id = position.get("id", Integer.class);
            positionIds.add(id);
        }
        return positionIds;
    }
}
