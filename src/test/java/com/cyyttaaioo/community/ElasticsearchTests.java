package com.cyyttaaioo.community;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyyttaaioo.community.dao.DiscussPostMapper;
import com.cyyttaaioo.community.dao.elasticsearch.DiscussPostRepository;
import com.cyyttaaioo.community.entity.DiscussPost;
import com.tdunning.math.stats.Sort;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticTemplate;

    @Qualifier("client")
    @Autowired
    private RestHighLevelClient restHighLevelClient;

//    @Value("${elasticsearch.indices}")
//    String esIndices;

    //判断某id的文档（数据库中的行）是否存在
//    @Test
//    public void testExist() {
//        boolean exists = discussRepository.existsById(109);
//        System.out.println(exists);
//    }

    //一次保存一条数据
    @Test
    public void testInsert() {
        //把id为241的DiscussPost的对象保存到discusspost索引（es的索引相当于数据库的表）
        discussRepository.save(discussMapper.selectDiscussPostById(241));
    }

    @Test
    public void testInsertList(){
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100,0));
    }

    @Test
    public void testUpdate(){
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水!");
        discussRepository.save(post);
    }

    @Test
    public void testDelete(){
       // discussRepository.deleteById(231);
        discussRepository.deleteAll();
    }

    @Test
    public void testSearchByRepository() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime.keyword").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

//        elasticTemplate.queryForPage(searchQuery, class,SearchResultMapper)
        //底层获取到了高亮显示的值，但没有返回

        Page<DiscussPost> page = discussRepository.search(searchQuery);

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

    @Test
    public void testSearchByTemplate() throws IOException {
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名

        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.field("content");
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");

        //构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime.keyword").order(SortOrder.DESC))
                .from(0)// 指定从哪条开始查询
                .size(10)// 需要查出的总记录条数
                .highlighter(highlightBuilder);//高亮

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<DiscussPost> list = new LinkedList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);

            // 处理高亮显示的结果
            HighlightField titleField = hit.getHighlightFields().get("title");
            if (titleField != null) {
                discussPost.setTitle(titleField.getFragments()[0].toString());
            }
            HighlightField contentField = hit.getHighlightFields().get("content");
            if (contentField != null) {
                discussPost.setContent(contentField.getFragments()[0].toString());
            }
            System.out.println(discussPost);
            list.add(discussPost);
        }
    }




//    //一次保存多条数据
//    @Test
//    public void testInsertList() {
//        //把id为101的用户发的前100条帖子（List<DiscussPost>）存入es的discusspost索引（es的索引相当于数据库的表）
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100, 0));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100, 0));
//    }
//
//    //通过覆盖原内容，来修改一条数据
//    @Test
//    public void testUpdate() {
//        DiscussPost post = discussMapper.selectDiscussPostById(230);
//        post.setContent("我是新人,使劲灌水。");
//        post.setTitle(null);//es中的title会设为null
//        discussRepository.save(post);
//    }
//
//    //修改一条数据
//    //覆盖es里的原内容 与 修改es中的内容 的区别：String类型的title被设为null，覆盖的话，会把es里的该对象的title也设为null；UpdateRequest，修改后该对象的title不变
//    @Test
//    void testUpdateDocument() throws IOException {
//        UpdateRequest request = new UpdateRequest("discusspost", "109");
//        request.timeout("1s");
//        DiscussPost post = discussMapper.selectDiscussPostById(230);
//        post.setContent("我是新人,使劲灌水.");
//        post.setTitle(null);//es中的title会保存原内容不变
//        request.doc(JSON.toJSONString(post), XContentType.JSON);
//        UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
//        System.out.println(updateResponse.status());
//    }
//
//    //删除一条数据和删除所有数据
//    @Test
//    public void testDelete() {
//        //discussRepository.deleteById(109);//删除一条数据
//        discussRepository.deleteAll();//删除所有数据
//    }
//
//    //不带高亮的查询
//    @Test
//    public void noHighlightQuery() throws IOException {
//        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名
//
//        //构建搜索条件
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
//                //在discusspost索引的title和content字段中都查询“互联网寒冬”
//                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                // matchQuery是模糊查询，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
//                // termQuery是精准查询：searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
//                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
//                //一个可选项，用于控制允许搜索的时间：searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//                .from(0)// 指定从哪条开始查询
//                .size(10);// 需要查出的总记录条数
//
//        searchRequest.source(searchSourceBuilder);
//        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//
//        System.out.println(JSONObject.toJSON(searchResponse));
//
//        List<DiscussPost> list = new LinkedList<>();
//        for (SearchHit hit : searchResponse.getHits().getHits()) {
//            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
//            System.out.println(discussPost);
//            list.add(discussPost);
//        }
//    }
//
//    //带高亮的查询
//    @Test
//    public void highlightQuery() throws Exception {
//        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名
//
//        //高亮
//        HighlightBuilder highlightBuilder = new HighlightBuilder();
//        highlightBuilder.field("title");
//        highlightBuilder.field("content");
//        highlightBuilder.requireFieldMatch(false);
//        highlightBuilder.preTags("<span style='color:red'>");
//        highlightBuilder.postTags("</span>");
//
//        //构建搜索条件
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
//                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
//                .from(0)// 指定从哪条开始查询
//                .size(10)// 需要查出的总记录条数
//                .highlighter(highlightBuilder);//高亮
//
//        searchRequest.source(searchSourceBuilder);
//        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//
//        List<DiscussPost> list = new LinkedList<>();
//        for (SearchHit hit : searchResponse.getHits().getHits()) {
//            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
//
//            // 处理高亮显示的结果
//            HighlightField titleField = hit.getHighlightFields().get("title");
//            if (titleField != null) {
//                discussPost.setTitle(titleField.getFragments()[0].toString());
//            }
//            HighlightField contentField = hit.getHighlightFields().get("content");
//            if (contentField != null) {
//                discussPost.setContent(contentField.getFragments()[0].toString());
//            }
//            System.out.println(discussPost);
//            list.add(discussPost);
//        }
//    }
//
//    ///////////////////////////////////////////
//
//    @Test
//    public void testExistsIndex() throws Exception {
//        System.out.println(existsIndex(esIndices));
//    }
//
//    @Test
//    public void testCreateIndex() throws Exception {
//        //System.out.println(createIndex(esIndices));
//        //把所有帖子（List<DiscussPost>）存入es的discusspost索引（es的索引相当于数据库的表）
//        //按分数降序插入es，就是按热门程度来排。搜索出来的顺序也是按热门程度来排。
//        discussRepository.saveAll(discussMapper.selectAllDiscussPosts());
//    }
//
//    @Test
//    public void testDeleteIndex() throws Exception {
//        discussRepository.deleteAll();//删除所有数据
//        System.out.println(deleteIndex(esIndices));
//    }
//
//    //判断索引是否存在
//    public boolean existsIndex(String index) throws IOException {
//        GetIndexRequest request = new GetIndexRequest(index);
//        boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
//        return exists;
//    }
//
//    //创建索引
//    public boolean createIndex(String index) throws IOException {
//        CreateIndexRequest request = new CreateIndexRequest(index);
//        CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
//        return createIndexResponse.isAcknowledged();
//    }
//
//    //删除索引
//    public boolean deleteIndex(String index) throws IOException {
//        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
//        AcknowledgedResponse response = restHighLevelClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
//        return response.isAcknowledged();
//    }

}
