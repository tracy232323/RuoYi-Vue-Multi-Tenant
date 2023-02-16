package com.ruoyi.demo.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @ClassName: IdObject
 * @Description: 作为节点的唯一标识
 * @Author: CodeDan
 * @Date: 2023/2/15 09:50
 * @Version: 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdObject {

    private Integer nodeId;

    private String providerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdObject idObject = (IdObject) o;

        return new EqualsBuilder().append(nodeId, idObject.nodeId).append(providerId, idObject.providerId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(nodeId).append(providerId).toHashCode();
    }
}
