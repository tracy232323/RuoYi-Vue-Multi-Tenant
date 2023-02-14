package com.ruoyi.demo.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.demo.domain.MapUserNode;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.ruoyi.demo.domain.request.ReqAuth;
import com.ruoyi.demo.domain.request.ReqRootTree;
import com.ruoyi.demo.domain.request.ReqUserAuth;
import com.ruoyi.demo.service.DemoService;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.TokenService;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.system.domain.SysUser;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import java.util.Set;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private DemoService demoService;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TokenService tokenService;

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
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        //获取sso登录的用户信息
        Object cacheObject = redisTemplate.opsForValue().get(user.getUserName());
        if (StringUtils.isEmpty(cacheObject) ) {
            return AjaxResult.success("无登陆用户，请先登陆");
        }
        String[] arr = cacheObject.toString().split("\\|");
        demoService.addAuthUser(reqUserAuth,arr[0], Integer.valueOf(arr[1]));
        return AjaxResult.success("success");
    }

    @ApiOperation("用户设置管理和浏览权限")
    @PostMapping("/addAuth")
    public AjaxResult addAuth(@RequestBody ReqAuth reqAuth) {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        //获取sso登录的用户信息
        Object cacheObject = redisTemplate.opsForValue().get(user.getUserName());
        if (StringUtils.isEmpty(cacheObject) ) {
            return AjaxResult.success("无登陆用户，请先登陆");
        }
        String[] arr = cacheObject.toString().split("\\|");
        demoService.addAuth(reqAuth,arr[0], Integer.valueOf(arr[1]));
        return AjaxResult.success("success");
    }

    @ApiOperation("删除用户管理和浏览权限")
    @PostMapping("/delAuth")
    public AjaxResult delAuth(@RequestBody ReqAuth reqAuth) {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        //获取sso登录的用户信息
        Object cacheObject = redisTemplate.opsForValue().get(user.getUserName());
        if (StringUtils.isEmpty(cacheObject) ) {
            return AjaxResult.success("无登陆用户，请先登陆");
        }
        String[] arr = cacheObject.toString().split("\\|");
        demoService.delAuth(reqAuth,arr[0], Integer.valueOf(arr[1]));
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
        String s = redisCache.getCacheObject("getNodeAllUser").toString();
        List<JSONObject> list = JSONUtil.toList(s, JSONObject.class);
        for (JSONObject jsonObject : list) {
            String path = jsonObject.getStr("path");
            String[] arr = path.split("/");
            List<String> paths = Arrays.asList(arr);
            int size = paths.size();
            jsonObject.putOpt("dept", paths.get(size - 3));
            jsonObject.putOpt("org", paths.get(size - 2));
            jsonObject.putOpt("position", paths.get(size - 1));
            //需要从实体中去掉，否则会写到excel里
            jsonObject.remove("gender");
            jsonObject.remove("catagory");
            jsonObject.remove("path");
            jsonObject.remove("positionId");
            jsonObject.remove("positionStatus");
            jsonObject.remove("mainPosition");
            jsonObject.remove("id");
        }
        //在内存操作，写到浏览器
        ExcelWriter writer = ExcelUtil.getWriter(true);

        //自定义标题别名
        writer.addHeaderAlias("order", "序号");
        writer.addHeaderAlias("name", "姓名");
        writer.addHeaderAlias("dept", "单位");
        writer.addHeaderAlias("org", "部门");
        writer.addHeaderAlias("position", "岗位");

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
//    @ApiOperation("根据providerId和nodeId获取当前节点中被授权对象信息列表(excel)")
//    @GetMapping("/get/{providerId}/{nodeId}/node/map/excel")
//    public void exportAuthorizeInfo(@PathVariable String providerId, @PathVariable Integer nodeId, HttpServletResponse response){
//        List<MapUserNode> nodeMap = demoService.getNodeMap(providerId, nodeId);
//        //查询所有用户
//        //在内存操作，写到浏览器
//        ExcelWriter writer= ExcelUtil.getWriter(true);
//        //自定义标题别名
//        writer.addHeaderAlias("username","用户名");
//        writer.addHeaderAlias("password","密码");
//        writer.addHeaderAlias("nickname","昵称");
//        writer.addHeaderAlias("email","邮箱");
//        writer.addHeaderAlias("phone","电话");
//        writer.addHeaderAlias("address","地址");
//        writer.addHeaderAlias("createTime","创建时间");
//        //默认配置
//        writer.write(nodeMap,true);
//        //设置content—type
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset:utf-8");
//        ServletOutputStream outputStream= null;
//        try{
//            String fileName= URLEncoder.encode("用户信息","UTF-8");
//            //Content-disposition是MIME协议的扩展，MIME协议指示MIME用户代理如何显示附加的文件。
//            response.setHeader("Content-Disposition","attachment;filename="+fileName+".xlsx");
//            outputStream= response.getOutputStream();
//            //将Writer刷新到OutPut
//            writer.flush(outputStream,true);
//        }catch (IOException e){
//
//        }finally {
//            if( StringU )
//            outputStream.close();
//            writer.close();
//        }
//        //设置标题
//
//
//    }

}
