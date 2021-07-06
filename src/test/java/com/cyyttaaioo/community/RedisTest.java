package com.cyyttaaioo.community;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings(){
         String redisKey = "test:count";

         redisTemplate.opsForValue().set(redisKey, 1);

        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes(){
        String redis = "test:user";

        redisTemplate.opsForHash().put(redis,"id",1);
        redisTemplate.opsForHash().put(redis,"username","zhangsan");

        System.out.println(redisTemplate.opsForHash().get(redis,"id"));
        System.out.println(redisTemplate.opsForHash().get(redis,"username"));
    }

    @Test
    public void testLists(){
        String redisKey = "test:ids";

        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey, 0));
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));

        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSets(){
        String redisKey = "test:people";

        redisTemplate.opsForSet().add(redisKey, "小明", "小刚", "小霞", "小光", "小茂", "叔叔");

        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey, 2));
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    @Test
    public void testSortSets(){
        String redisKey = "test:students";

        redisTemplate.opsForZSet().add(redisKey,"劳伦斯", 80);
        redisTemplate.opsForZSet().add(redisKey, "玛丽亚", 90);
        redisTemplate.opsForZSet().add(redisKey, "格曼", 50);
        redisTemplate.opsForZSet().add(redisKey, "路德维希", 70);
        redisTemplate.opsForZSet().add(redisKey, "洋葱哥", 60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "玛丽亚"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "玛丽亚"));//倒序
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));//倒序

    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");

        System.out.println(redisTemplate.hasKey("test:user"));

        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    //多次访问同一个key
    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());

    }

    //编程式事务
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";

                operations.multi();//开启事务

                operations.opsForSet().add(redisKey,"zhangsan");
                operations.opsForSet().add(redisKey,"lishi");
                operations.opsForSet().add(redisKey,"wangwu");

                System.out.println(operations.opsForSet().members(redisKey));

                return operations.exec();//提交事务
            }
        });
        System.out.println("-------------");
        System.out.println(obj);
    }

}
