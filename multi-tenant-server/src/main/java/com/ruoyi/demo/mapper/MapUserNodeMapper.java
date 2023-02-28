package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.MapUserNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author codedan
 * @description 针对表【map_user_node(用户与节点映射的节点权限表)】的数据库操作Mapper
 * @createDate 2023-02-07 14:38:28
 * @Entity com.ruoyi.demo.domain.MapUserNode
 */
@Mapper
public interface MapUserNodeMapper {
    public Integer insertBatch(List<MapUserNode> list);

    public List<MapUserNode> selectAll();

    void deleteByNodeIds(@Param("ids") List<Integer> ids);

    public MapUserNode selectOne(@Param("companyId") String companyId, @Param("userId") Integer userId, @Param("nodeId") Integer nodeId);

    public void update2Auth(@Param("id")Integer id, @Param("isManage")Integer isManage, @Param("isShow")Integer isShow);

    List<MapUserNode> selectListByNodeId(Integer nodeId);

    MapUserNode selectOneById(Integer id);

    List<MapUserNode> selectListByShow(MapUserNode mapUserNode);

    List<MapUserNode> selectListByManager(String providerId, Integer userId,Integer isManage);

    List<MapUserNode> selectAllUser();

    List<MapUserNode> selectPositionListByUser(@Param("companyId") String companyId, @Param("userId")Integer userId);

    void updatePathByPosition(@Param("companyId")String companyId, @Param("userId")Integer userId, @Param("positionId")Integer positionId, @Param("path")String path);
}




