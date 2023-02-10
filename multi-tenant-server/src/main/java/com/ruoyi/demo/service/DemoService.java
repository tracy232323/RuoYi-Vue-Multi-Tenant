package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;

public interface DemoService {
    String getOringTree(ReqRootTree reqRootTree);

    void addAuthUser(ReqUserAuth reqUserAuth);

    String getTreeByUserId(String providerId, Integer userId);

    void addAuth(ReqAuth reqAuth);

    void delAuth(ReqAuth reqAuth);
}
