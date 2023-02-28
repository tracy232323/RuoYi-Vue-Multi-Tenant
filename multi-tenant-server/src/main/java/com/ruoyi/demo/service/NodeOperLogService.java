package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.NodeOperLog;

import java.util.List;

/**
 * @ClassName: NodeOperLogService
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/27 09:26
 * @Version: 1.0.0
 **/
public interface NodeOperLogService {


    void updateById(NodeOperLog nodeOperLog);

    List<NodeOperLog> selectListByNodeId(Integer nodeId);
}
