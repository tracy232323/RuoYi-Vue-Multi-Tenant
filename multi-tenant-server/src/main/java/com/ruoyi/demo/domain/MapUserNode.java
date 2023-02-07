package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户与节点映射的节点权限表
 * @TableName map_user_node
 */
@TableName(value ="map_user_node")
@Data
public class MapUserNode implements Serializable {
    /**
     * 用户与节点映射id
     */
    @TableId(value = "id")
    private Integer id;

    /**
     * 用户id
     */
    @TableField(value = "user_id")
    private Integer userId;

    /**
     * 单位id（只有公司id和用户id一起才能定位到用户）
     */
    @TableField(value = "company_id")
    private String companyId;

    /**
     * 节点id
     */
    @TableField(value = "node_id")
    private Integer nodeId;

    /**
     * 是否拥有管理权限(1为无，1为有)
     */
    @TableField(value = "is_manage")
    private Integer isManage;

    /**
     * 是否拥有浏览权限(1为无，1为有)
     */
    @TableField(value = "is_show")
    private Integer isShow;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 创建时间
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