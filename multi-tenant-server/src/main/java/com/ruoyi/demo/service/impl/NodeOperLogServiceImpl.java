package com.ruoyi.demo.service.impl;

import com.ruoyi.demo.domain.NodeOperLog;
import com.ruoyi.demo.mapper.NodeOperLogMapper;
import com.ruoyi.demo.service.NodeOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: NodeOperLogServiceImpl
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/27 09:26
 * @Version: 1.0.0
 **/
@Service
public class NodeOperLogServiceImpl implements NodeOperLogService {

    @Autowired
    private NodeOperLogMapper nodeOperLogMapper;

    @Override
    public void updateById(NodeOperLog nodeOperLog) {

    }

    @Override
    public List<NodeOperLog> selectListByNodeId(Integer nodeId) {
        return nodeOperLogMapper.selectListByNodeId(nodeId);
    }
}
