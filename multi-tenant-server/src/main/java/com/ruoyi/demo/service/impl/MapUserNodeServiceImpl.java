package com.ruoyi.demo.service.impl;


import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.demo.service.MapUserNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author codedan
* @description 针对表【map_user_node(用户与节点映射的节点权限表)】的数据库操作Service实现
* @createDate 2023-02-07 14:38:28
*/
@Service
public class MapUserNodeServiceImpl implements MapUserNodeService{
    @Autowired
    private MapUserNodeMapper mapUserNodeMapper;

    @Override
    public Integer insertBatch(List<MapUserNode> list) {
        return mapUserNodeMapper.insertBatch(list);
    }
}




