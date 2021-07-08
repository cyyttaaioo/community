package com.cyyttaaioo.community.controller;

import com.cyyttaaioo.community.annotation.LoginRequired;
import com.cyyttaaioo.community.entity.Event;
import com.cyyttaaioo.community.entity.User;
import com.cyyttaaioo.community.event.EventProducer;
import com.cyyttaaioo.community.service.LikeService;
import com.cyyttaaioo.community.util.CommunityConstant;
import com.cyyttaaioo.community.util.CommunityUtil;
import com.cyyttaaioo.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

   @LoginRequired
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId){
        User user = hostHolder.getUser();

        //点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        //数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        //返回结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞事件
       if(likeStatus == 1){
           Event event = new Event()
                   .setTopic(TOPIC_LIKE)
                   .setUserId(hostHolder.getUser().getId())
                   .setEntityType(entityType)
                   .setEntityId(entityId)
                   .setEntityUserId(entityUserId)
                   .setData("postId", postId);
           eventProducer.fileEvent(event);
       }


        return CommunityUtil.getJSONString(0, null, map);
    }

}
