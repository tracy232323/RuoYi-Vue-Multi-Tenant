package com.ruoyi.demo.service.impl;

import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.service.NodeInfoService;
import com.ruoyi.demo.mapper.NodeInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author codedan
* @description 针对表【node_info(组织树节点表)】的数据库操作Service实现
* @createDate 2023-02-07 14:37:12
*/
@Service
public class NodeInfoServiceImpl implements NodeInfoService{
    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Transactional
    @Override
    public Integer insertBatch(List<NodeInfo> list) {
        return nodeInfoMapper.insertBatch(list);
    }
}




