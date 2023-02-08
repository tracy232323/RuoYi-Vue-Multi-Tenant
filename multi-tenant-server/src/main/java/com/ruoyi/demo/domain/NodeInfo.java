package com.ruoyi.demo.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组织树节点表
 * @author codedan
 * @TableName node_info
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NodeInfo implements Serializable {
    /**
     * 节点id
     */
    private Integer id;

    /**
     * 节点类型，1为组织、2为部门、3为岗位
     */
    private Integer type;

    /**
     * 当前节点父亲节点，根节点为0
     */
    private Integer fatherId;

    /**
     * 节点名称（即节点的简要称呼）
     */
    private String name;

    /**
     * 节点的排序号
     */
    private Integer order;

    /**
     * 根节点的独特id
     */
    private String providerId;

    private String nodeId;

    /**
     * 创建时间
     */
    private Date updateTime;

    /**
     * 更新时间
     */
    private Date createTime;

    /**
     * 逻辑删除
     */
    private Integer deleted;
}