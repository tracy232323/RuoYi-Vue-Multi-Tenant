package com.ruoyi.demo.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户与节点映射的节点权限表
 * @TableName map_user_node
 */
@Data
public class MapUserNode implements Serializable {
    /**
     * 用户与节点映射id
     */
    private Integer id;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 单位id（只有公司id和用户id一起才能定位到用户）
     */
    private String companyId;

    /**
     * 节点id
     */
    private Integer nodeId;

    /**
     * 是否拥有管理权限(1为无，1为有)
     */
    private Integer isManage;

    /**
     * 是否拥有浏览权限(1为无，1为有)
     */
    private Integer isShow;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 逻辑删除
     */
    private Integer deleted;
}