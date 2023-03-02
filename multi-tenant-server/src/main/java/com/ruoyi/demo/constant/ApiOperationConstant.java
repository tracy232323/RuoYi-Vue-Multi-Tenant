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
    public static final String GET_ORGANIZATION_USER_ALL_URL = "https://hrapitest.crcc.cn/api/hr/tree/{providerId}/{companyId}/users";
    public static final String PROVIDER_ID = "{providerId}";
    public static final String COMPANY_ID = "{companyId}";
    public static final String POSITION_ID = "{positionId}";
    public static final String ORG_ID = "{orgId}";
    public static final String USER_ID = "{userid}";
    public static final String DEPT_ID = "{deptId}";
    public static final Integer TYPE_POSITION = 3;
    public static final Integer TYPE_DEPT = 2;
    public static final String GET_POSITION_ALL_USER_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/position/{positionId}/users";
    public static final String GET_COMPANY_ALL_ORG_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/company/{orgId}/children";
    public static final String GET_USER_ALL_POSITION_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/user/{userId}/positions";
    public static final String GET_DEPT_ORG_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/department/{deptId}/children";
    public static final String GET_POSITION_ORG_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/position/{positionId}/parent";
    public static final String GET_ORG_PATH_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/path/{orgId}";
    public static final String AUTHORITY_MANAGER = "manager";
    public static final String AUTHORITY_SHOW = "show";
    public static final String AUTHORITY_ALL = "all";

    public static final String AUTHORITY_NOT_ALL = "all";
    public static final Integer AUTHORITY_MANAGER_VALUE = 1;
    public static final Integer AUTHORITY_SHOW_VALUE = 1;

    public static final Integer AUTHORITY_NOT_MANAGER_VALUE = 0;
    public static final Integer AUTHORITY_NOT_SHOW_VALUE = 0;
    public static final String GET_USER_INFO_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/user/{userid}";
    public static final String GET_MAIN_POSITION_URL = "https://hrapitest.crcc.cn/api/hr/org/{providerId}/user/{userid}/mainposition";
    public static final String OPERATION_TITLE_ADD_NUMBER = "新增授权人员";
    public static final String OPERATION_TITLE_MODIFY_AUTHORITY = "修改人物权限";
    public static final String OPERATION_TITLE_DELETE_AUTHORITY = "删除授权人员";

}
