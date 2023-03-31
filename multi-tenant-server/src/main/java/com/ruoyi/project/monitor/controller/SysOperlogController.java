package com.ruoyi.project.monitor.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.TokenService;
import com.ruoyi.project.monitor.mapper.SysOperLogMapper;
import com.ruoyi.project.system.domain.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.service.ISysOperLogService;

/**
 * 操作日志记录
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/operlog")
public class SysOperlogController extends BaseController
{
    @Autowired
    private ISysOperLogService operLogService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TokenService tokenService;
    @Autowired
    private MapUserNodeMapper mapUserNodeMapper;

    @Autowired
    private SysOperLogMapper operLogMapper;

//    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysOperLog operLog)
    {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        //获取sso登录的用户信息
        Object cacheObject = stringRedisTemplate.opsForValue().get(user.getUserName());
        if (StringUtils.isEmpty(cacheObject) ) {
            throw new CustomException("无此用户，请重新登陆");
        }
        String[] arr = cacheObject.toString().split("\\|");
        String providerId = arr[0];
        Integer userId = Integer.parseInt(arr[1]);
        MapUserNode mapUserNode = mapUserNodeMapper.selectOne(providerId, userId, 1);
        if( !StringUtils.isEmpty(mapUserNode) ){
            startPage();
            List<SysOperLog> sysOperLogs = operLogMapper.selectOperLogList(operLog);
            return getDataTable(sysOperLogs);
        }
        // 进入这里，说明不是root用户，需要加入nodeIds的配置
        // 根据当前登陆的用户信息，查询其拥有哪些授权节点
        List<MapUserNode> mapUserNodes = mapUserNodeMapper.selectListByManager(providerId, userId,1);
        if( mapUserNodes.isEmpty() ){
            return getDataTable(new ArrayList<>());
        }
        Iterator<MapUserNode> iterator = mapUserNodes.iterator();
        ArrayList<Integer> nodeIds = new ArrayList<>();
        while( iterator.hasNext() ){
            MapUserNode next = iterator.next();
            nodeIds.add(next.getNodeId());
        }
        operLog.setNodeIds(nodeIds);
        startPage();
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:export')")
    @GetMapping("/export")
    public AjaxResult export(SysOperLog operLog)
    {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        //获取sso登录的用户信息
        Object cacheObject = stringRedisTemplate.opsForValue().get(user.getUserName());
        if (StringUtils.isEmpty(cacheObject) ) {
            throw new CustomException("无此用户，请重新登陆");
        }
        String[] arr = cacheObject.toString().split("\\|");
        List<SysOperLog> list = operLogService.selectOperLogList(operLog,arr[0], Integer.valueOf(arr[1]));
        ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
        return util.exportExcel(list, "操作日志");
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/{operIds}")
    public AjaxResult remove(@PathVariable Long[] operIds)
    {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        operLogService.cleanOperLog();
        return AjaxResult.success();
    }
}
