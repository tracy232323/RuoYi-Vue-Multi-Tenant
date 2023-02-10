package com.ruoyi.demo.domain.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Data
public class ReqUserAuth implements Serializable {
    @ApiModelProperty("授权人员集合")
    private List<UserAuth> list;

    @Setter
    @Getter
    public static class UserAuth {
        @ApiModelProperty("人员id")
        private Integer userId;
        @ApiModelProperty("人员所在的二级单位")
        private String providerId;
        @ApiModelProperty("人员所在的部门id")
        private Integer orgId;
        @ApiModelProperty("人员所在的岗位id")
        private Integer positionId;
    }
}
