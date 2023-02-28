package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.PositionOperLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @ClassName: PositionOperLogMapper
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/24 16:20
 * @Version: 1.0.0
 **/
@Mapper
public interface PositionOperLogMapper {

    void add(Integer nodeId, String context);

    List<PositionOperLog> selectListByNodeId(Integer nodeId);

    void updateById(PositionOperLog positionOperLog);
}
