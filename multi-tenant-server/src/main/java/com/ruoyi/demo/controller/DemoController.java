package com.ruoyi.demo.controller;

import com.ruoyi.demo.constant.RedisConstant;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private DemoService demoService;

    @PostMapping("/getOringTree")
    public AjaxResult getOringTree(@RequestBody ReqRootTree reqRootTree) {
        return AjaxResult.success("success",demoService.getOringTree(reqRootTree));
    }

    @GetMapping("/get/{providerId}/{userId}/tree")
    public AjaxResult getTreeByUserId(@PathVariable String providerId, @PathVariable Integer userId){
        String treeJSON = demoService.getTreeByUserId(providerId,userId);
        return AjaxResult.success("success",treeJSON);
    }

    @PostMapping("/addAuthUser")
    public String addAuthUser(@RequestBody ReqUserAuth reqUserAuth) {
        return null;
    }
}
