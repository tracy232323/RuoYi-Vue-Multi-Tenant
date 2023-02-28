package com.ruoyi.project.monitor.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ruoyi.common.exception.CustomException;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.domain.MapUserNode;
import com.ruoyi.demo.mapper.MapUserNodeMapper;
import com.ruoyi.framework.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.project.monitor.domain.SysOperLog;
import com.ruoyi.project.monitor.mapper.SysOperLogMapper;
import com.ruoyi.project.monitor.service.ISysOperLogService;
import org.springframework.util.StringUtils;

/**
 * 操作日志 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysOperLogServiceImpl implements ISysOperLogService
{
    @Autowired
    private SysOperLogMapper operLogMapper;

    @Autowired
    private MapUserNodeMapper mapUserNodeMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增操作日志
     * 
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(SysOperLog operLog)
    {
        operLogMapper.insertOperlog(operLog);
    }

    /**
     * 查询系统操作日志集合
     * 
     * @param operLog 操作日志对象
     * @return 操作日志集合
     */
    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog operLog,String providerId, Integer userId)
    {
        MapUserNode mapUserNode = mapUserNodeMapper.selectOne(providerId, userId, 1);
        if( !StringUtils.isEmpty(mapUserNode) ){
            return operLogMapper.selectOperLogList(operLog);
        }
        // 进入这里，说明不是root用户，需要加入nodeIds的配置
        // 根据当前登陆的用户信息，查询其拥有哪些授权节点
        List<MapUserNode> mapUserNodes = mapUserNodeMapper.selectListByManager(providerId, userId,1);
        if( mapUserNodes.isEmpty() ){
            return new ArrayList<SysOperLog>();
        }
        Iterator<MapUserNode> iterator = mapUserNodes.iterator();
        ArrayList<Integer> nodeIds = new ArrayList<>();
        while( iterator.hasNext() ){
            MapUserNode next = iterator.next();
            nodeIds.add(next.getNodeId());
        }
        operLog.setNodeIds(nodeIds);
        return operLogMapper.selectOperLogList(operLog);
    }

    /**
     * 批量删除系统操作日志
     * 
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(Long[] operIds)
    {
        return operLogMapper.deleteOperLogByIds(operIds);
    }

    /**
     * 查询操作日志详细
     * 
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public SysOperLog selectOperLogById(Long operId)
    {
        return operLogMapper.selectOperLogById(operId);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog()
    {
        operLogMapper.cleanOperLog();
    }
}
