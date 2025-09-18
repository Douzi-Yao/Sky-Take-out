package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatProperties weChatProperties;


    // 微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public UserLoginVO wxLogin(UserLoginDTO userLoginDTO) {
//        // 调用微信接口服务，获得当前微信用户的openId
//        // 通过HttpCleint向地址发送Get请求
//        Map<String, String> map = new HashMap<>();
//        map.put("appid", weChatProperties.getAppid());
//        map.put("secret", weChatProperties.getSecret());
//        map.put("js_code", userLoginDTO.getCode());
//        map.put("grant_type", "authorization_code");
//        String json = HttpClientUtil.doGet(WX_LOGIN, map);
//        // 从json字符串中解析出openid
//        JSONObject jsonObject = JSON.parseObject(json);
//        String openid = jsonObject.getString("openid");
        String openid = getOpenid(userLoginDTO.getCode());

        // 判断openId是否为空，若为空则表示登录失败，抛出业务异常。不为空则为合法微信用户
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断当前微信用户是否是新用户，即当前openId是否在user表中，不在则新增
        User user = userMapper.getByOpenid(openid);

        // 如果是新用户，则自动完成注册。封装User对象，保存到user表中
        if(user == null){
            // user为空则为新用户
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        // 为微信用户生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(), claims);

        // 返回UserLoginVO对象
        UserLoginVO userLoginVO = UserLoginVO.builder()
                                            .id(user.getId())
                                            .openid(user.getOpenid())
                                            .token(token)
                                            .build();
        return userLoginVO;
    }

    /**
     * 调用微信接口服务，获得当前微信用户的openId
     * @param code
     * @return
     */
    private String getOpenid(String code){
        // 调用微信接口服务，获得当前微信用户的openId
        // 通过HttpCleint向地址发送Get请求
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        // 从json字符串中解析出openid
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
