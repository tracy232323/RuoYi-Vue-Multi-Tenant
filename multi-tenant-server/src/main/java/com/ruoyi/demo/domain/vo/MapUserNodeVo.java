package com.ruoyi.demo.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @ClassName: MapUserNodeVo
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/2/14 16:03
 * @Version: 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapUserNodeVo {

    private Integer index;

    /**
     * 用户所属组织路径
     */
    private String path;

    /**
     * 是否拥有管理权限(1为无，1为有)
     */
    private Integer isManage;

    /**
     * 是否拥有浏览权限(1为无，1为有)
     */
    private Integer isShow;

}
