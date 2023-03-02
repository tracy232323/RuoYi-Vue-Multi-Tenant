package com.ruoyi.demo.domain.vo;

import com.ruoyi.demo.domain.UserInfo;
import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: UserInfoVo
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/3/1 10:42
 * @Version: 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVo {

    private List<UserInfo> records;

    private Integer total;

}
