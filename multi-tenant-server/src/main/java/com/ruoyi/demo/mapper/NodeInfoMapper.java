package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.NodeInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author codedan
 * @description 针对表【node_info(组织树节点表)】的数据库操作Mapper
 * @createDate 2023-02-07 14:37:12
 * @Entity com.ruoyi.demo.domain.NodeInfo
 */
@Mapper
public interface NodeInfoMapper {

    public Integer insertBatch(List<NodeInfo> list);

    public List<NodeInfo> selectAll();

    public List<NodeInfo> selectByMap(@Param("companyId") String companyId, @Param("userId") Integer userId);

    void deleteByIds(@Param("ids") List<Integer> ids);

    List<NodeInfo> selectMapByPositionId(@Param("providerId")String providerId, @Param("positionId")Integer positionId);

    public NodeInfo selectOne(@Param("providerId") String providerId, @Param("nodeId") Integer nodeId);

    NodeInfo selectOneById(Integer id);
}




