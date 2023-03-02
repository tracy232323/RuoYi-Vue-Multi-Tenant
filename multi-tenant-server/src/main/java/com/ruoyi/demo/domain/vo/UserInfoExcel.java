package com.ruoyi.demo.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: UserInfoExcel
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/3/2 09:24
 * @Version: 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoExcel {

    private Integer index;

    private String name;

    private String unit;

    private String dept;

    private String position;

}
