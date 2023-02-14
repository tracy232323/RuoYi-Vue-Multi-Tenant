package com.ruoyi.demo.domain;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.format.annotation.DateTimeFormat;

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

    private Integer nodeId;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 逻辑删除
     */
    private Integer deleted;

    public NodeInfo(Integer nodeId,Integer type, Integer fatherId, String name, Integer order, String providerId) {
        this.type = type;
        this.fatherId = fatherId;
        this.name = name;
        this.order = order;
        this.providerId = providerId;
        this.nodeId = nodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeInfo nodeInfo = (NodeInfo) o;

        return new EqualsBuilder().append(type, nodeInfo.type).append(fatherId, nodeInfo.fatherId).append(name, nodeInfo.name).append(order, nodeInfo.order).append(providerId, nodeInfo.providerId).append(nodeId, nodeInfo.nodeId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(type).append(fatherId).append(name).append(order).append(providerId).append(nodeId).toHashCode();
    }
}