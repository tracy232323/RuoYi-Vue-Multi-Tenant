package com.ruoyi.demo.service.impl;

import com.ruoyi.demo.domain.RootUser;
import com.ruoyi.demo.mapper.RootUserMapper;
import com.ruoyi.demo.service.RootUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RootUserServiceImpl implements RootUserService {
    @Autowired
    private RootUserMapper rootUserMapper;

    @Override
    public List<RootUser> selectAll() {
        return rootUserMapper.selectAll();
    }

    @Transactional
    @Override
    public int insert(RootUser rootUser) {
        return rootUserMapper.insert(rootUser);
    }
}
