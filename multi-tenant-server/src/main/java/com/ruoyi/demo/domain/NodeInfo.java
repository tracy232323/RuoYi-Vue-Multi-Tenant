package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 组织树节点表
 * @TableName node_info
 */
@TableName(value ="node_info")
@Data
public class NodeInfo implements Serializable {
    /**
     * 节点id
     */
    @TableId(value = "id")
    private Integer id;

    /**
     * 节点类型，1为组织、2为部门、3为岗位
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 当前节点父亲节点，根节点为0
     */
    @TableField(value = "father_id")
    private Integer fatherId;

    /**
     * 节点名称（即节点的简要称呼）
     */
    @TableField(value = "name")
    private String name;

    /**
     * 节点的排序号
     */
    @TableField(value = "order")
    private Integer order;

    /**
     * 根节点的独特id
     */
    @TableField(value = "main_id")
    private String mainId;

    /**
     * 创建时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 更新时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 逻辑删除
     */
    @TableField(value = "deleted")
    private Integer deleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}