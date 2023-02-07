package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.NodeInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author codedan
* @description 针对表【node_info(组织树节点表)】的数据库操作Mapper
* @createDate 2023-02-07 14:37:12
* @Entity com.ruoyi.demo.domain.NodeInfo
*/
@Mapper
public interface NodeInfoMapper extends BaseMapper<NodeInfo> {

}




