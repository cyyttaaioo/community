package com.cyyttaaioo.community.service;

import com.cyyttaaioo.community.dao.UserMapper;
import com.cyyttaaioo.community.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User findUserId(int id){
        return userMapper.selectById(id);
    }
}
