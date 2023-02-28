package com.ruoyi.demo.service.impl;

import com.ruoyi.demo.domain.PositionOperLog;
import com.ruoyi.demo.mapper.PositionOperLogMapper;
import com.ruoyi.demo.service.PositionOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: PositionOperLogServiceImpl
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/27 09:47
 * @Version: 1.0.0
 **/
@Service
public class PositionOperLogServiceImpl implements PositionOperLogService {

    @Autowired
    private PositionOperLogMapper positionOperLogMapper;

    @Override
    public List<PositionOperLog> selectListByNodeId(Integer nodeId) {
        return positionOperLogMapper.selectListByNodeId(nodeId);
    }
}
