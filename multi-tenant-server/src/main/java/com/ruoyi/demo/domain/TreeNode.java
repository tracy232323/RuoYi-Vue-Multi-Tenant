package com.ruoyi.demo.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @ClassName: TreeNode
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/8 21:30
 * @Version: 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TreeNode {

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

    private List<TreeNode> children = new LinkedList<>();

    public void setNodeInfo(NodeInfo nodeInfo) {
        this.id = nodeInfo.getId();
        this.type = nodeInfo.getType();
        this.fatherId = nodeInfo.getFatherId();
        this.name = nodeInfo.getName();
        this.order = nodeInfo.getOrder();
        this.providerId = nodeInfo.getProviderId();
        this.nodeId = nodeInfo.getNodeId();
        this.updateTime = nodeInfo.getUpdateTime();
        this.createTime = nodeInfo.getCreateTime();
    }
}
