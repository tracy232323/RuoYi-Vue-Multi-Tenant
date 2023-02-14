package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.NodeInfo;
import java.util.List;

/**
 * @author codedan
 * @description 针对表【node_info(组织树节点表)】的数据库操作Service
 * @createDate 2023-02-07 14:37:12
 */
public interface NodeInfoService {

    public Integer insertBatch(List<NodeInfo> list);

    public List<NodeInfo> selectAll();

    public List<NodeInfo> selectByMap(String companyId, Integer userId);

    void deleteByIds(List<Integer> ids);

    List<NodeInfo> selectMapByPositionId(String providerId, Integer positionId);

    NodeInfo selectOne(NodeInfo nodeInfo);
}
