package com.ruoyi.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @ClassName: UserInfo
 * @Description: TODO
 * @Author: CodeDan
 * @Date: 2023/3/1 11:03
 * @Version: 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {

    private Integer id;

    private String providerId;

    private Integer nodeId;

    private Integer userId;

    private String name;

    private String unit;

    private String dept;

    private String position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userInfo = (UserInfo) o;

        return new EqualsBuilder().append(providerId, userInfo.providerId).append(nodeId, userInfo.nodeId).append(userId, userInfo.userId).append(name, userInfo.name).append(unit, userInfo.unit).append(dept, userInfo.dept).append(position, userInfo.position).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(providerId).append(nodeId).append(userId).append(name).append(unit).append(dept).append(position).toHashCode();
    }
}
