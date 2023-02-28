package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.NodeOperLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @ClassName: NodeOperLogMapper
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/24 16:18
 * @Version: 1.0.0
 **/
@Mapper
public interface NodeOperLogMapper {

    List<NodeOperLog> selectListByNodeId(Integer nodeId);

    void add(NodeOperLog nodeOperLog);

    void updateById(NodeOperLog nodeOperLog);
}
