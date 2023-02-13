package com.ruoyi.demo.controller;

import cn.hutool.json.JSONObject;
import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.framework.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private DemoService demoService;

    @ApiOperation("获取HR树，每个节点调一次")
    @PostMapping("/getOringTree")
    public String getOringTree(@RequestBody ReqRootTree reqRootTree) {
        return demoService.getOringTree(reqRootTree);
    }

    @ApiOperation("根据当前用户信息获取其的组织树信息")
    @GetMapping("/get/{providerId}/{userId}/tree")
    public String getTreeByUserId(@PathVariable String providerId, @PathVariable Integer userId){
        return demoService.getTreeByUserId(providerId,userId);
    }

    @ApiOperation("把用户添加到映射表")
    @PostMapping("/addAuthUser")
    public AjaxResult addAuthUser(@RequestBody ReqUserAuth reqUserAuth) {
        demoService.addAuthUser(reqUserAuth);
        return AjaxResult.success("success");
    }

    @ApiOperation("用户设置管理和浏览权限")
    @PostMapping("/addAuth")
    public AjaxResult addAuth(@RequestBody ReqAuth reqAuth) {
        demoService.addAuth(reqAuth);
        return AjaxResult.success("success");
    }

    @ApiOperation("删除用户管理和浏览权限")
    @PostMapping("/delAuth")
    public AjaxResult delAuth(@RequestBody ReqAuth reqAuth) {
        demoService.delAuth(reqAuth);
        return AjaxResult.success("success");
    }

    @ApiOperation("根据providerId和nodeId获取当前节点中被授权对象信息列表")
    @GetMapping("/get/{providerId}/{nodeId}/node/map")
    public AjaxResult getNodeMap(@PathVariable String providerId, @PathVariable Integer nodeId){
         return AjaxResult.success(demoService.getNodeMap(providerId, nodeId));
    }

    @ApiOperation("查询节点下所有的用户信息")
    @PostMapping("/getNodeAllUser")
    public List<JSONObject> getNodeAllUser(@RequestBody ReqRootTree reqRootTree) {
        return demoService.getNodeAllUser(reqRootTree);
    }

}
