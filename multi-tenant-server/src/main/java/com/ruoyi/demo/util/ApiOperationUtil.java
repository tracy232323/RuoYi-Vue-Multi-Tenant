package com.ruoyi.demo.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.demo.constant.ApiOperationConstant;
import com.ruoyi.demo.constant.HttpStatusConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

import static org.hibernate.validator.internal.metadata.raw.ConfigurationSource.API;

/**
 * @ClassName: ApiOperationUtil
 * @Description: 调用第三方API工具类
 * @Author: CodeDan
 * @Date: 2023/2/7 10:30
 * @Version: 1.0.0
 **/
@Component
@Slf4j
public class ApiOperationUtil {

    /**
     * 采用懒汉子模式，只有在第一次获取以及过期的时候才会去更新token
     */
    public static String accessToken;
    /**
     * 获取token接口
     * @param url 获取token url
     * @param grantType 获取token的类型
     * @param clientId client id
     * @param clientSecret client secret
     * @return
     */
    public String getAccessToken(String url, String grantType, String clientId, String clientSecret){
        // 拼接一下
        String requestUrl = url + "?"+ ApiOperationConstant.GRANT_TYPE + "=" + grantType;
        HttpResponse execute = HttpRequest.get(requestUrl)
                .basicAuth(clientId, clientSecret)
                .timeout(ApiOperationConstant.TIME_OUT)
                .execute();
        int status = execute.getStatus();
        if( status != HttpStatusConstant.OK ){
            throw new CustomException("获取token失败，状态码为:"+status);
        }
        String result = execute.body();
        log.info(result);
        Map body = new JSONObject(result).toBean(Map.class);
        String token = String.valueOf(body.get(ApiOperationConstant.ACCESS_TOKEN));
        accessToken = token;
        return token;
    }

    /**
     * 获取所有的二级组织信息
     * @param url 获取所有的二级组织信息url
     * @return
     */
    public String getAllOrganizationInfo(String url){
        if(StringUtils.isEmpty(accessToken)){
            getAccessToken(ApiOperationConstant.GET_ACCESS_TOKEN_URL, ApiOperationConstant.CLIENT_CREDENTIALS, ApiOperationConstant.CLIENT_ID, ApiOperationConstant.CLIENT_SECRET);
        }
        HttpResponse execute = HttpRequest.get(url)
                .header(ApiOperationConstant.AUTHORIZATION, "Bearer " + accessToken)
                .execute();
        int status = execute.getStatus();
        if(status == HttpStatusConstant.OK){
            String result = execute.body();
            log.info(result);
            return result;
        }else if( status == HttpStatusConstant.Unauthorized ){
            accessToken = null;
            return getAllOrganizationInfo(url);
        }else{
            throw new CustomException("调用"+url+"失败，状态码为:"+status);
        }
    }

    /**
     * 获取所有的二级组织下所有的机构
     * @param url 获取所有的二级组织下机构集合url
     * @return
     */
    public String getOrganizationChildren(String url,String providerId, Integer companyId){
        // 替换url中的指定参数
        url = url.replace(ApiOperationConstant.PROVIDER_ID, providerId);
        url = url.replace(ApiOperationConstant.COMPANY_ID,companyId.toString());
        HttpResponse execute = HttpRequest.get(url)
                .header(ApiOperationConstant.AUTHORIZATION, "Bearer " + accessToken)
                .execute();
        int status = execute.getStatus();
        if(status == HttpStatusConstant.OK){
            String result = execute.body();
            log.info(result);
            return result;
        }else if( status == HttpStatusConstant.Unauthorized ){
            accessToken = null;
            return getAllOrganizationInfo(url);
        }else{
            throw new CustomException("调用"+url+"失败，状态码为:"+status);
        }
    }
}
