package com.ruoyi.demo.mapper;

import com.ruoyi.demo.domain.RootUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RootUserMapper {
    public List<RootUser> selectAll();

    public int insert(RootUser rootUser);
}
