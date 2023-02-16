package com.ruoyi.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    private NodeInfo nodeInfo;

    private List<TreeNode> children = new LinkedList<>();

}
