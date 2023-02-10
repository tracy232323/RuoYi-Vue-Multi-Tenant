package com.ruoyi.demo.domain.request;

import lombok.Data;

@Data
public class ReqAuth {
    private Integer[] ids;
    private Integer isManage;
    private Integer isShow;
}
