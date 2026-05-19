package org.soft.base.model;

public class Like {
    private int likeId;
    private int articleId;
    private int userId;
    private String likeTime;

    public Like() {
    }

    public Like(int likeId, int articleId, int userId, String likeTime) {
        this.likeId = likeId;
        this.articleId = articleId;
        this.userId = userId;
        this.likeTime = likeTime;
    }

    public int getLikeId() {
        return likeId;
    }

    public void setLikeId(int likeId) {
        this.likeId = likeId;
    }

    public int getArticleId() {
        return articleId;
    }

    public void setArticleId(int articleId) {
        this.articleId = articleId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getLikeTime() {
        return likeTime;
    }

    public void setLikeTime(String likeTime) {
        this.likeTime = likeTime;
    }
}