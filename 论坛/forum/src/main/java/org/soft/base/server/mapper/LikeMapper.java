package org.soft.base.server.mapper;

import org.apache.ibatis.annotations.*;
import org.soft.base.model.Like;

import java.util.List;

@Mapper
public interface LikeMapper {
    @Insert("INSERT INTO article_likes (article_id, user_id) VALUES (#{articleId}, #{userId})")
    int addLike(Like like);

    @Delete("DELETE FROM article_likes WHERE article_id = #{articleId} AND user_id = #{userId}")
    int removeLike(Like like);

    @Select("SELECT COUNT(*) FROM article_likes WHERE article_id = #{articleId}")
    int countLikesByArticle(@Param("articleId") int articleId);

    @Select("SELECT * FROM article_likes WHERE article_id = #{articleId} AND user_id = #{userId}")
    List<Like> findLikesByUserAndArticle(Like like);

    // 确保这个方法正确实现
    @Select("SELECT COUNT(*) FROM article_likes WHERE article_id = #{articleId} AND user_id = #{userId}")
    int isLikedByUser(@Param("articleId") int articleId, @Param("userId") int userId);
}