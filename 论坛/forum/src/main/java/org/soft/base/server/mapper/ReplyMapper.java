package org.soft.base.server.mapper;

import org.apache.ibatis.annotations.*;
import org.soft.base.model.Reply;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReplyMapper {
    @Insert("insert into reply (replyContent, replyTime) value (#{replyContent}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "replyId")
    public boolean replyIssueMapper(Reply reply);

    @Insert("insert into replyrelation (userId, articleId, replyId) VALUE (#{userId},#{articleId},#{replyId})")
    public boolean replyRelationMapper(Map<String, Integer> map);

    @Select("select r.*, u.userName from reply r inner join replyRelation re on " +
            "r.replyId = re.replyId and re.articleId = #{articleId} " +
            "inner join users u on re.userId = u.userId " +
            "limit #{begin}, #{size}")
    public List<Reply> repliesByArticleListMapper(Map<String,Integer> map);

    @Select("select count(r.replyId) from reply r inner join replyRelation re " +
            "on r.replyId = re.replyId and re.articleId = #{articleId}")
    public int repliesAllRows(int articleId);
}