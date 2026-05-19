Page({
  data: {
    post: null,
    comments: [],
    showCommentInput: false,
    commentContent: ''
  },
  
  onLoad(options) {
    // 检查必要参数
    if (!options.id) {
      wx.showToast({ title: '参数错误', icon: 'error' });
      setTimeout(() => wx.navigateBack(), 1500);
      return;
    }
    
    // 从URL参数中获取帖子数据并解码
    const post = {
      id: options.id,
      title: options.title ? decodeURIComponent(options.title) : '',
      author: options.author ? decodeURIComponent(options.author) : '',
      image: options.image ? decodeURIComponent(options.image) : '',
      content: options.content ? decodeURIComponent(options.content) : '',
      publishTime: options.publishTime ? decodeURIComponent(options.publishTime) : '',
      likes: 0,
      video: options.video ? decodeURIComponent(options.video) : ''
    };
    this.setData({ post });
    this.loadComments(options.id);
  },
  
  loadComments(id) {
    wx.showLoading({ title: '加载中...' });
    
    setTimeout(() => {
      // 模拟评论数据
      const comments = [
        {
          id: 1,
          author: '蟹老板',
          avatar: '/images/avatar/avatar2.png',
          content: '帅炸了！',
          time: '2025-06-11 13:56:28'
        }
      ];
      
      this.setData({ 
        comments: comments
      });
      
      wx.hideLoading();
    }, 800);
  },
  
  onShareAppMessage() {
    if (!this.data.post) return {};
    
    return {
      title: this.data.post.title,
      path: '/pages/post/detail?id=' + this.data.post.id
    }
  },
  
  // 点赞功能
  likePost() {
    const post = this.data.post;
    if (!post) return;
    
    post.likes = (post.likes || 0) + 1;
    this.setData({ post });
    
    wx.showToast({
      title: '已点赞',
      icon: 'success'
    });
  },
  
  // 显示评论输入框
  focusComment() {
    this.setData({ showCommentInput: true });
  },
  
  // 评论输入内容变化
  onCommentInput(e) {
    this.setData({
      commentContent: e.detail.value
    });
  },
  
  // 提交评论
  submitComment() {
    const commentContent = this.data.commentContent;
    if (!commentContent) {
      wx.showToast({
        title: '请输入评论内容',
        icon: 'none'
      });
      return;
    }
    
    const newComment = {
      id: Date.now(),
      author: '匿名用户',
      content: commentContent,
      time: new Date().toLocaleString()
    };
    
    const comments = this.data.comments;
    comments.push(newComment);
    
    this.setData({
      comments,
      showCommentInput: false,
      commentContent: ''
    });
    
    wx.showToast({
      title: '评论提交成功',
      icon: 'success'
    });
  },
  
  // 取消评论输入
  cancelComment() {
    this.setData({
      showCommentInput: false,
      commentContent: ''
    });
  }
});