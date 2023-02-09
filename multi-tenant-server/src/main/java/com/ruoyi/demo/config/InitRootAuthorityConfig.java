package com.ruoyi.demo.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.google.common.collect.Lists;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.NodeFieldConstant;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.util.ApiOperationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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

    @PostConstruct
    public void init() {
        int number = 0;
        apiOperationUtil.getAccessToken(
                ApiOperationConstant.GET_ACCESS_TOKEN_URL,
                ApiOperationConstant.CLIENT_CREDENTIALS,
                ApiOperationConstant.CLIENT_ID,
                ApiOperationConstant.CLIENT_SECRET);
        //TODO: 简单逻辑处理，后期待优化
//        List<NodeInfo> nodeList = nodeInfoService().;
//        if (nodeList.isEmpty()) {
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
                    if (tempList.size() > 1000) {
                        List<List<NodeInfo>> partitions = Lists.partition(tempList, 1000);
                        for (List<NodeInfo> partition : partitions) {
                            nodeInfoService.insertBatch(partition);
                        }
                    }
                }
//            }
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

}
