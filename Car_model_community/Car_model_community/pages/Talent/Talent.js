const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    posts: vehicleData.swiperItems.map(item => ({ ...item, isFollowed: false })), // 初始化关注状态
    activeTab: '综合' // 默认选中“综合”
  },

  // 切换标签
  switchTab(e) {
    this.setData({
      activeTab: e.currentTarget.dataset.tab
    });
    // 可扩展：根据标签请求不同数据，这里演示简单切换
  },

  // 关注/取消关注
  toggleFollow(e) {
    const index = e.currentTarget.dataset.index;
    const posts = this.data.posts;
    posts[index].isFollowed = !posts[index].isFollowed;
    this.setData({ posts });
    
    // 模拟保存关注状态到本地存储
    const followedUsers = wx.getStorageSync('followedUsers') || [];
    const userId = posts[index].userId;
    if (posts[index].isFollowed) {
      if (!followedUsers.includes(userId)) {
        followedUsers.push(userId);
      }
    } else {
      const newFollowedUsers = followedUsers.filter(id => id !== userId);
      wx.setStorageSync('followedUsers', newFollowedUsers);
    }
    
    wx.showToast({
      title: posts[index].isFollowed ? '已关注' : '已取消关注',
      icon: 'none'
    });
  },

  // 跳转个人空间
  goToUserSpace(e) {
    const userId = e.currentTarget.dataset.userid;
    if (!userId) {
        wx.showToast({
            title: '用户ID不存在，无法跳转',
            icon: 'none'
        });
        return;
    }
    wx.navigateTo({
        url: `/pages/user-space/index?userId=${userId}`
    });
  },
  
  onLoad() {
    // 从本地存储中恢复关注状态
    const followedUsers = wx.getStorageSync('followedUsers') || [];
    const posts = this.data.posts.map(item => ({
      ...item,
      isFollowed: followedUsers.includes(item.userId)
    }));
    this.setData({ posts });
  }
});