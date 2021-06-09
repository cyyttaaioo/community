package com.cyyttaaioo.community.service;

import com.cyyttaaioo.community.dao.UserMapper;
import com.cyyttaaioo.community.entity.User;
import com.cyyttaaioo.community.util.CommunityUtil;
import com.cyyttaaioo.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;//域名

    @Value("${server.servlet.context-path}")
    private String contextPath;//项目名

    public User findUserId(int id){
        return userMapper.selectById(id);
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();

        //空值处理
        if(user==null){
            throw  new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        //验证信息
        User u = userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg","该账号已存在");
            return map;
        }

        //验证邮箱
         u = userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("emailMsg","该邮箱已被注册");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));//密码后的随机数
        user.setPassword(CommunityUtil.md5(user.getPassword()) + user.getSalt());//生成加密密码
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());//激活码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);


        return map;
    }
}
