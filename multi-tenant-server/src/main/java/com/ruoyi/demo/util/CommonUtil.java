package com.ruoyi.demo.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ruoyi.demo.domain.NodeInfo;
import com.ruoyi.demo.domain.TreeNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: CommonUtil
 * @Description: 通用方法工具类
 * @Author: CodeDan
 * @Date: 2023/2/10 16:10
 * @Version: 1.0.0
 **/
@Component
public class CommonUtil {

    /**
     * 根据获取的组织列表，构建一个用户所在组织路径
     * @param data 组织列表
     * @return 用户所在组织路径
     */
    public String buildUserPathFromTree(String data){
        List<JSONObject> nodeList = new JSONArray(data).toList(JSONObject.class);
        StringBuilder str = new StringBuilder();
        for(JSONObject node : nodeList) {
            if( node.containsKey("virtual") && Boolean.TRUE.equals(node.get("virtual",Boolean.class))){
                continue;
            }
            str.insert(0,node.get("name"));
            str.insert(0,"/");
        }
        str = str.deleteCharAt(0);
        return str.toString();
    }

    public Integer getInsertIndex(List<TreeNode> list, Integer order){
        if( list.isEmpty() ){
            return 0;
        }
        int leftIndex = 0;
        int rightIndex = list.size() - 1;
        while( leftIndex <= rightIndex ){
            int mid = leftIndex + ((rightIndex - leftIndex) / 2);
            if (list.get(mid).getNodeInfo().getOrder() > order) {
                // target 在左区间，所以[low, mid - 1]
                rightIndex = mid - 1;
            } else{
                // target 在右区间，所以[mid + 1, high]
                leftIndex = mid + 1;
            }
        }
        return rightIndex + 1;
    }

}
