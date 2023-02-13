package com.ruoyi.demo.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private DemoService demoService;
    @Autowired
    private RedisCache redisCache;

    @ApiOperation("获取HR树，每个节点调一次")
    @PostMapping("/getOringTree")
    public String getOringTree(@RequestBody ReqRootTree reqRootTree) {
        return demoService.getOringTree(reqRootTree);
    }

    @ApiOperation("根据当前用户信息获取其的组织树信息")
    @GetMapping("/get/{providerId}/{userId}/tree")
    public String getTreeByUserId(@PathVariable String providerId, @PathVariable Integer userId) {
        return demoService.getTreeByUserId(providerId, userId);
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
    public AjaxResult getNodeMap(@PathVariable String providerId, @PathVariable Integer nodeId) {
        return AjaxResult.success(demoService.getNodeMap(providerId, nodeId));
    }

    @ApiOperation("查询节点下所有的用户信息")
    @PostMapping("/getNodeAllUser")
    public List<JSONObject> getNodeAllUser(@RequestBody ReqRootTree reqRootTree) {
        return demoService.getNodeAllUser(reqRootTree);
    }

    @ApiOperation("查询节点下所有的用户信息")
    @GetMapping("/getNodeAllUser/export")

    public void export(HttpServletResponse response) throws IOException {
        //查询所有用户
//        List<User> list = userService.list();
        String s = redisCache.getCacheObject("getNodeAllUser").toString();
        List<JSONObject> list = JSONUtil.toList(s, JSONObject.class);
        //在内存操作，写到浏览器
        ExcelWriter writer = ExcelUtil.getWriter(true);

        //自定义标题别名
        writer.addHeaderAlias("order", "序号");
        writer.addHeaderAlias("name", "姓名");
        writer.addHeaderAlias("path", "单位");
        writer.addHeaderAlias("path", "部门");
        writer.addHeaderAlias("path", "岗位");

        //默认配置
        writer.write(list, true);
        //设置content—type
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset:utf-8");

        //设置标题
        String fileName = URLEncoder.encode("用户信息", "UTF-8");
        //Content-disposition是MIME协议的扩展，MIME协议指示MIME用户代理如何显示附加的文件。
        response.setHeader("Content-Disposition", System.currentTimeMillis() + ".xlsx");
                ServletOutputStream outputStream = response.getOutputStream();

        //将Writer刷新到OutPut
        writer.flush(outputStream, true);
        outputStream.close();
        writer.close();
    }
}
