package com.cyyttaaioo.community.controller;

import com.cyyttaaioo.community.entity.DiscussPost;
import com.cyyttaaioo.community.entity.Page;
import com.cyyttaaioo.community.service.ElasticSearchService;
import com.cyyttaaioo.community.service.LikeService;
import com.cyyttaaioo.community.service.UserService;
import com.cyyttaaioo.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticSearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // /search?keyword=
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page , Model model){
        // 查找帖子
        List<Object> list = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() -1, page.getLimit());

        // 聚合数据
        List<DiscussPost> tempPosts = (List<DiscussPost>) list.get(1);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(tempPosts.size() > 0){
            for(DiscussPost post : tempPosts){
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        // 设置分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(list.get(0) == null ? 0 : (int)list.get(0));
        return "/site/search";
    }
}