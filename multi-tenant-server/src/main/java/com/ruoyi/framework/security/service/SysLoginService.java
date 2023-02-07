package com.ruoyi.framework.security.service;

import javax.annotation.Resource;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.security.LoginUser;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
public class SysLoginService {
    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserService userService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param captcha  验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid) {
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }
        // 用户验证
        Authentication authentication = null;
        try {
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new CustomException(e.getMessage());
            }
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // 生成token
        return tokenService.createToken(loginUser);
    }

    public String ssoLogin(String code) {
        String tokenUrl = "https://g1openid.crcc.cn/oauth/token?grant_type=authorization_code&code=" + code + "&redirect_uri=http://hk.app.qiuqiuhetiantian.net";
        String jsonToken = HttpRequest.get(tokenUrl).header("Authorization", "Basic ZGNkZW1vOmVldFNzQ3lqS0NyaXBFN2doRzhBN3FKMzhIVm96Q3BvZ2xKN3VRQ0M=").execute().body();
        String tokenError = JSON.parseObject(jsonToken).getString("error");
        if (StringUtils.isNotBlank(tokenError)) {
            String msg = JSON.parseObject(jsonToken).getString("error_description");
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(code, Constants.LOGIN_FAIL, msg));
            throw new CustomException(msg);
        }
        String accessToken = JSON.parseObject(jsonToken).getString("access_token");

        String userInfoUrl = "https://g1openid.crcc.cn/oauth/userinfo";
        String jsonUserInfo = HttpRequest.get(userInfoUrl).header("Authorization", "Bearer " + accessToken).execute().body();
        String name = JSON.parseObject(jsonUserInfo).getString("name");
        String infoError = JSON.parseObject(jsonUserInfo).getString("error");
        if (StringUtils.isNotBlank(infoError)) {
            String msg = JSON.parseObject(jsonUserInfo).getString("msg");
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(code, Constants.LOGIN_FAIL, msg));
            throw new CustomException(msg);
        }
        String mappingId = name.substring(3,name.length());
        SysUser sysUser = userService.selectUserByMappingId(mappingId);
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(sysUser.getUserName(), sysUser.getMappingPwd()));
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        // 生成token
        return tokenService.createToken(loginUser);
    }

}
