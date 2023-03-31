package com.ruoyi.demo.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.google.common.collect.Lists;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.UserInfo;
import com.ruoyi.demo.mapper.UserInfoMapper;
import com.ruoyi.demo.util.ApiOperationUtil;
import com.ruoyi.demo.util.CommonUtil;
import com.sun.scenario.animation.shared.FiniteClipEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: NodeAllUserInitConfig
 * @Description: 初始化节点上用户列表
 * @Author: CodeDan
 * @Date: 2023/3/1 09:34
 * @Version: 1.0.0
 **/
@Configuration
@Slf4j
@EnableScheduling
public class NodeAllUserInitConfig {

    @Autowired
    private ApiOperationUtil apiOperationUtil;
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private UserInfoMapper userInfoMapper;

    @PostConstruct
//    @Scheduled(cron = "0 0 0/1 * * ?")
    @Scheduled(cron = "0 0/30 * * * ?")
    @Transactional
    public void initAndUpdateNodeAllUser(){
        // 获取当前全部的二级节点，遍历二级节点
        log.info("正在更新用户列表");
        String allOrganizationInfo = apiOperationUtil.getAllOrganizationInfo(ApiOperationConstant.GET_ALL_ORGANIZATION_URL);
        List<JSONObject> organizationInfos = new JSONArray(allOrganizationInfo).toList(JSONObject.class);
        List<UserInfo> tempList = new ArrayList<>();
        List<String> providerIds =  new ArrayList<>();
        for (JSONObject organizationInfo : organizationInfos) {
            // 获取hr系统中二级节点下的完整路径。去迭代遍历并收集用户信息
            String providerId = organizationInfo.get("id").toString();
            providerIds.add(providerId);
            JSONObject root = new JSONObject(organizationInfo.get("root"));
            Integer id = Integer.parseInt(root.get("id").toString());
            String organizationAndUserAll = apiOperationUtil.getOrganizationAndUserAll(ApiOperationConstant.GET_ORGANIZATION_USER_ALL_URL, providerId, id);
            // 进入递归收集
            commonUtil.organizationAndUserAll(tempList, new JSONObject(organizationAndUserAll),providerId,new StringBuilder(),"");
        }
        // 遍历结束
        // 获取数据库中所有的用户信息
        List<UserInfo> userInfos = userInfoMapper.selectAll();
        log.info("正在对比获取移除员工列表");
        // 将hr系统中的用户信息与数据库中用户数据进行差集对比，得到修改、删除、新增
        List<UserInfo> removeList = userInfos.stream().filter(item -> !tempList.contains(item)).collect(Collectors.toList());
        log.info("移除员工列表:{}",removeList);
        Iterator<UserInfo> removeIterator = removeList.iterator();
        while( removeIterator.hasNext() ){
            UserInfo next = removeIterator.next();
            userInfoMapper.delete(next);
        }
        log.info("正在对比获取新增员工列表");
        List<UserInfo> addList = tempList.stream().filter(item -> !userInfos.contains(item)).collect(Collectors.toList());
        log.info("新增员工列表:{}",addList);
        if(!addList.isEmpty()){
            if (addList.size() > 1000) {
                List<List<UserInfo>> partitions = Lists.partition(addList, 1000);
                for (List<UserInfo> partition : partitions) {
                    userInfoMapper.insertBatch(partition);
                }
            } else {
                userInfoMapper.insertBatch(addList);
            }
        }
        for (String providerId : providerIds) {
            // 获取hr系统中二级节点下的完整路径。去迭代遍历并收集用户信息
            log.info("正在构建:{}的用户缓存",providerId);
            CommonUtil.providerUsers.put(providerId,userInfoMapper.selectListByProviderId(providerId));
        }
    }
}
