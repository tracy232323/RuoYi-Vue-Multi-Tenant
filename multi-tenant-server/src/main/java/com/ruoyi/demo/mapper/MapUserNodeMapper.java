package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.MapUserNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author codedan
* @description 针对表【map_user_node(用户与节点映射的节点权限表)】的数据库操作Mapper
* @createDate 2023-02-07 14:38:28
* @Entity com.ruoyi.demo.domain.MapUserNode
*/
@Mapper
public interface MapUserNodeMapper{
    public Integer insertBatch(List<MapUserNode> list);
}




