package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.UserInfo;
import org.apache.catalina.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @ClassName: UserInfoMapper
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/3/1 11:50
 * @Version: 1.0.0
 **/
@Mapper
public interface UserInfoMapper {

    List<UserInfo> selectAll();

    void delete(UserInfo next);

    void add(UserInfo next);

    List<UserInfo> selectList(String providerId, Integer orgId);

    List<UserInfo> selectListByNodes(List<NodeInfo> nodes);

    void insertBatch(List<UserInfo> partition);

    List<UserInfo> selectListByProviderId(String providerId);
}
