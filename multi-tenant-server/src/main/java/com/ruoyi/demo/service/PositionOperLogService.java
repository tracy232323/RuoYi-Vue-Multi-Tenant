package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.PositionOperLog;

import java.util.List;

/**
 * @ClassName: PositionOperLogService
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/27 09:27
 * @Version: 1.0.0
 **/
public interface PositionOperLogService {
    List<PositionOperLog> selectListByNodeId(Integer nodeId);
}
