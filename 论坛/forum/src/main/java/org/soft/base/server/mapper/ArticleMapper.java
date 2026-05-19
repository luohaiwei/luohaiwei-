package org.soft.base.server.mapper;

import org.apache.ibatis.annotations.*;
import org.soft.base.model.Article;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper {

    @Insert("insert into article (articleTitle, articleTime, articleContent, userId) value " +
            "(#{articleTitle},now(),#{articleContent},#{userId}) ")
    @Options(useGeneratedKeys = true , keyProperty = "articleId")
    public boolean articleIssueMapper(Article article);

    // 管理员用：查询所有文章
// 管理员文章列表
    @Select("SELECT a.*, u.userName FROM article a JOIN users u ON a.userId = u.userId ORDER BY articleTime DESC limit #{begin},#{size}")
    public List<Article> articleListMapper(Map<String,Integer> map);

    // 用户文章列表
    @Select("SELECT a.*, u.userName FROM article a JOIN users u ON a.userId = u.userId WHERE a.isActive=1 ORDER BY articleTime DESC limit #{begin},#{size}")
    public List<Article> activeArticleListMapper(Map<String,Integer> map);

    @Select("select count(*) from article")
    public int articleAllRowsMapper();

    // 查询上架文章数量
    @Select("select count(*) from article where isActive=1")
    public int articleActiveRowsMapper();

    @Select("SELECT a.*, u.userName FROM article a JOIN users u ON a.userId = u.userId WHERE a.articleId = #{articleId}")
    public Article articleByIdMapper(int articleId);

    @Delete("delete from article where articleId = #{articleId}")
    public boolean articleDeleteById(int articleId);

    @Update("update article set articleTitle = #{articleTitle} , " +
            "articleContent = #{articleContent} where articleId = #{articleId}")
    public boolean articleUpdateMapper(Article article);

    @Update("update article set isActive = 1 where articleId = #{articleId}")
    public boolean articlePutOnSale(int articleId);

    @Update("update article set isActive = 0 where articleId = #{articleId}")
    public boolean articleTakeOffSale(int articleId);
    @Select("SELECT COUNT(*) FROM article WHERE userId = #{userId}")
    int countArticlesByUser(int userId);

    @Select("SELECT * FROM article WHERE userId = #{userId} ORDER BY articleTime DESC LIMIT #{begin}, #{size}")
    List<Article> getArticlesByUser(
            @Param("userId") int userId,
            @Param("begin") int begin,
            @Param("size") int size);

    @Select("SELECT * FROM article WHERE articleTitle LIKE CONCAT('%', #{keyword}, '%') AND isActive=1 ORDER BY articleTime DESC")
    List<Article> searchArticlesByTitle(@Param("keyword") String keyword);
}