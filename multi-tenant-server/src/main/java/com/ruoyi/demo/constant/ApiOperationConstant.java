package com.ruoyi.demo.constant;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: ApiOperationConstant
 * @Description: API调用常量
 * @Author: CodeDan
 * @Date: 2023/2/7 11:40
 * @Version: 1.0.0
 **/
public class ApiOperationConstant {
    public static final String GRANT_TYPE = "grant_type";

    public static final Integer TIME_OUT = 20000;
    public static final String GET_ACCESS_TOKEN_URL = "https://regtest.crcc.cn/oauth/token";
    public static final String CLIENT_ID = "dcdemo";
    public static final String CLIENT_SECRET = "oxWuON9CvlokJObm5s9YdCFWP9qCzqXUYdmAVJML";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final Object ACCESS_TOKEN = "access_token";
    public static final String AUTHORIZATION = "authorization";
    public static final String GET_ALL_ORGANIZATION_URL = "https://hrapitest.crcc.cn/api/hr/orglist";
    public static final String GET_ORGANIZATION_CHILDREN_URL = "https://hrapitest.crcc.cn/api/hr/tree/{providerId}/{companyId}";

    public static final String PROVIDER_ID = "{providerId}";

    public static final String COMPANY_ID = "{companyId}";
}
