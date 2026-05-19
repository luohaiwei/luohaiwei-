Page({
  data: {
    topic: null,
    posts: []
  },
  
  onLoad(options) {
    if (options.topic) {
      // 解析传递过来的话题数据
      const topicData = JSON.parse(decodeURIComponent(options.topic));
      this.setData({
        topic: {
          id: topicData.id,
          title: topicData.title,
          dateRange: topicData.dateRange,
          participants: topicData.participants,
          description: topicData.description,
          author: topicData.author,
          commentCount: topicData.commentCount,
          image: topicData.image
        },
        posts: topicData.posts
      });
    } else {
      wx.showToast({
        title: '话题数据加载失败',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },
  
  onShareAppMessage() {
    if (!this.data.topic) return {};
    
    return {
      title: this.data.topic.title,
      path: `/pages/topic/detail?topic=${encodeURIComponent(JSON.stringify(this.data.topic))}`
    }
  },
  
  // 处理帖子点击
  onPostTap(e) {
    const id = e.currentTarget.dataset.id;
    const post = this.data.posts.find(item => item.id === id);
    if (post) {
      wx.navigateTo({
        url: `/pages/post/detail?id=${id}&title=${encodeURIComponent(post.title)}&author=${encodeURIComponent(post.author)}&image=${encodeURIComponent(post.image)}`,
      });
    }
  }
});