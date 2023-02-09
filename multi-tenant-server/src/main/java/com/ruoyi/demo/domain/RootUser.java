package com.ruoyi.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RootUser {
    private Integer id;
    private String providerId;
    private Integer userId;
    private Date updateTime;
    private Date createTime;
    private Integer deleted;
}
