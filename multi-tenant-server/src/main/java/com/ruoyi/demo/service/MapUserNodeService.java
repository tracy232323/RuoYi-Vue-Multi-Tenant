package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.MapUserNode;

import java.util.List;

/**
* @author codedan
* @description 针对表【map_user_node(用户与节点映射的节点权限表)】的数据库操作Service
* @createDate 2023-02-07 14:38:28
*/
public interface MapUserNodeService  {
    public Integer insertBatch(List<MapUserNode> list);

    public List<MapUserNode> selectAll();

    void deleteByNodeIds(List<Integer> ids);

    MapUserNode selectOne(String providerId, Integer userId, Integer id);
}
