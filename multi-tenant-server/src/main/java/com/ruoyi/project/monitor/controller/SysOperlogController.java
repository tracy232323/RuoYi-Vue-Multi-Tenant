package com.ruoyi.project.monitor.controller;

import java.util.List;

import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.TokenService;
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

//    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysOperLog operLog)
    {
        startPage();
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        //获取sso登录的用户信息
        Object cacheObject = stringRedisTemplate.opsForValue().get(user.getUserName());
        if (StringUtils.isEmpty(cacheObject) ) {
            throw new CustomException("无此用户，请重新登陆");
        }
        String[] arr = cacheObject.toString().split("\\|");
        List<SysOperLog> list = operLogService.selectOperLogList(operLog,arr[0], Integer.valueOf(arr[1]));
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
