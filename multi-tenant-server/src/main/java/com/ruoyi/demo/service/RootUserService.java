package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.RootUser;

import java.util.List;

public interface RootUserService {

    public List<RootUser> selectAll();

    public int insert(RootUser rootUser);
}
