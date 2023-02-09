package com.ruoyi.demo.domain.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ReqRootTree implements Serializable {
    @ApiModelProperty("类型 1：单位 2：部门 3：岗位")
    public Integer type;

    @ApiModelProperty("二级单位的编码 如hr11 hr22")
    public String providerId;

    @ApiModelProperty("部门id")
    public Integer orgId;

    @ApiModelProperty("岗位id")
    public Integer positionId;
}