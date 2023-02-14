package com.ruoyi.demo.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
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

}
