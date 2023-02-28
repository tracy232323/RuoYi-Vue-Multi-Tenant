package com.ruoyi;

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
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.mapper.SysOperLogMapper;
import com.ruoyi.project.monitor.service.impl.SysOperLogServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.xml.soap.Node;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private SysOperLogMapper sysOperLogMapper;


    @Autowired
    private SysOperLogServiceImpl sysOperLogService;

    @Test
    public void test6() {
        List<Integer> a = new ArrayList<Integer>();
        List<Integer> b = new ArrayList<Integer>();
        a.add(1);
        a.add(2);
        a.add(4);
        a.add(5);
        b.add(1);
        List<Integer> collect1 = a.stream().filter(item -> !b.contains(item)).collect(Collectors.toList());
        log.info("Collect1:{}",collect1);
        List<Integer> collect2 = b.stream().filter(item -> !a.contains(item)).collect(Collectors.toList());
        log.info("Collect2:{}",collect2);
    }
//    insert into sys_oper_log(title, business_type, method, request_method, operator_type, oper_name, dept_name, oper_url, oper_ip, oper_location, oper_param, json_result, status, error_msg, oper_time,oper_info)
//    values (#{title}, #{businessType}, #{method}, #{requestMethod}, #{operatorType}, #{operName}, #{deptName}, #{operUrl}, #{operIp}, #{operLocation}, #{operParam}, #{jsonResult}, #{status}, #{errorMsg}, sysdate(), #{operInfo})


    @Test
    public void test() {
        String token = apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
//        String hr = apiOperationUtil.getUserInfo(ApiOperationConstant.GET_USER_INFO_URL, "hr", 828046);
        String hr = apiOperationUtil.getMainPositionByUser(ApiOperationConstant.GET_MAIN_POSITION_URL, "hr", 828046);
        log.info("hr:{}",hr);
    }

    @Test
    public void test4() {
        StringBuilder str = new StringBuilder();
        str.append("123");
        str.insert(0,"321");
        String s = str.toString();
        log.info("str:{}",s);
    }

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private MapUserNodeService mapUserNodeService;


    @Autowired
    private RootUserService rootUserService;

    @Autowired
    private RedisCache redisCache;

    @Test
    public void init() {
        apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
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
            getOrganizationChildren(tempList, new JSONObject(organizationChildren), providerId, 0);
        }
        // 这里是存在逻辑。进行数据对比，获取不同的数据
        // 差集 (list1 - list2)
        List<NodeInfo> reduce1 = tempList.stream().filter(item -> !nodeList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce1 (list1 - list2)---：{}",reduce1);
        // 判断有没有新增节点，插入后进行有则进行权限授予，无则跳过
        if (nodeList.isEmpty() && !reduce1.isEmpty()) {
            // 开始解析组织树
            if (reduce1.size() > 1000) {
                List<List<NodeInfo>> partitions = Lists.partition(reduce1, 1000);
                for (List<NodeInfo> partition : partitions) {
                    nodeInfoService.insertBatch(partition);
                }
            } else {
                nodeInfoService.insertBatch(tempList);
            }
        }
        // 查询有多少个root账号,就给他们都授权了
        List<RootUser> rootUsers = rootUserService.selectAll();
        for( RootUser rootUser : rootUsers ){
            if( !reduce1.isEmpty() ){
                grantedPermissionsByNodeList(rootUser.getProviderId(), rootUser.getUserId(), ApiOperationConstant.AUTHORITY_MANAGER, reduce1);
            }
        }
        // 看看需要删除多少节点
        // 差集 (list2 - list1)
        List<NodeInfo> reduce2 = nodeList.stream().filter(item -> !tempList.contains(item)).collect(Collectors.toList());
        log.info("---得到差集 reduce2 (list2 - list1)---:{}",reduce2);
        // 判断有没有删减节点，有则进行授权删除以及节点删除，无则跳过
        List<Integer> ids = reduce2.stream().map(NodeInfo::getId).collect(Collectors.toList());
        if( !reduce2.isEmpty() ){
            // 删除授权
            mapUserNodeService.deleteByNodeIds(ids);
            // 删除节点
            nodeInfoService.deleteByIds(ids);
        }
        // 第四步，根据Root账号去遍历出他们被授权的节点结合
        for( RootUser rootUser : rootUsers ){
            List<NodeInfo> nodeInfos = nodeInfoService.selectByMap(rootUser.getProviderId(), rootUser.getUserId());
            // 第五步：根据此集合构建树结构
            String treeJSON = buildShowTree(nodeInfos);
            // 第六步：存放redis即可
            String key = RedisConstant.REDIS_USER_TREE_PREFIX+ rootUser.getProviderId()+":"+rootUser.getUserId();
            redisCache.setCacheObject(key,treeJSON);
        }
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


    @Test
    public void testGrantedPermissions(){
        List<NodeInfo> nodeInfos = nodeInfoService.selectAll();
        grantedPermissionsByNodeList("hr",828046,ApiOperationConstant.AUTHORITY_MANAGER,nodeInfos);
    }


    public void grantedPermissionsByNodeList(String providerId, Integer userId, String type,List<NodeInfo> nodeInfos){
        // 使用iterator在大数据量时，效率高
        Iterator<NodeInfo> iterator = nodeInfos.iterator();
        List<MapUserNode> tempList = new ArrayList<>();
        while(iterator.hasNext()) {
            MapUserNode mapUserNode = new MapUserNode();
            NodeInfo next = iterator.next();
            mapUserNode.setCompanyId(providerId);
            mapUserNode.setUserId(userId);
            mapUserNode.setNodeId(next.getId());
            // 判断授权
            if( ApiOperationConstant.AUTHORITY_MANAGER.equals(type) ){
                mapUserNode.setIsManage(ApiOperationConstant.AUTHORITY_MANAGER_VALUE);
                mapUserNode.setIsShow(ApiOperationConstant.AUTHORITY_SHOW_VALUE);
            }else if( ApiOperationConstant.AUTHORITY_SHOW.equals(type) ){
                mapUserNode.setIsManage(ApiOperationConstant.AUTHORITY_NOT_MANAGER_VALUE);
                mapUserNode.setIsShow(ApiOperationConstant.AUTHORITY_SHOW_VALUE);
            }else{
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
    public  void test03() {
        List<String> list1 = new ArrayList();
        list1.add("1111");
        list1.add("2222");
        list1.add("4444");

        List<String> list2 = new ArrayList();
        list2.add("1111");
//        list2.add("2222");
//        list2.add("3333");

        // 交集
        List<String> intersection = list1.stream().filter(item -> list2.contains(item)).collect(Collectors.toList());
        System.out.println("---得到交集 intersection---");
        intersection.parallelStream().forEach(System.out :: println);

        // 差集 (list1 - list2)
        List<String> reduce1 = list1.stream().filter(item -> !list2.contains(item)).collect(Collectors.toList());
        System.out.println("---得到差集 reduce1 (list1 - list2)---");
        reduce1.parallelStream().forEach(System.out :: println);

        // 差集 (list2 - list1)
        List<String> reduce2 = list2.stream().filter(item -> !list1.contains(item)).collect(Collectors.toList());
        System.out.println("---得到差集 reduce2 (list2 - list1)---");
        reduce2.parallelStream().forEach(System.out :: println);

        // 并集
        List<String> listAll = list1.parallelStream().collect(Collectors.toList());
        List<String> listAll2 = list2.parallelStream().collect(Collectors.toList());
        listAll.addAll(listAll2);
        System.out.println("---得到并集 listAll---");
        listAll.parallelStream().forEach(System.out :: println);

        // 去重并集
        List<String> listAllDistinct = listAll.stream().distinct().collect(Collectors.toList());
        System.out.println("---得到去重并集 listAllDistinct---");
        listAllDistinct.parallelStream().forEach(System.out :: println);

        System.out.println("---原来的List1---");
        list1.parallelStream().forEach(System.out :: println);
        System.out.println("---原来的List2---");
        list2.parallelStream().forEach(System.out :: println);
    }

    @Test
    public void test04(){
        List<NodeInfo> nodeInfos = nodeInfoService.selectAll();
        String s = buildShowTree(nodeInfos);
    }

    public String buildShowTree(List<NodeInfo> analogData) {
        // 查询当前用户所在岗位集合

        // 根据用户以及岗位查询出存在授权的节点集合

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
