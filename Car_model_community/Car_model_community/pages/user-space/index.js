Page({
  data: {
    userInfo: {}, 
    activeTab: '爱车' 
  },

  onLoad(options) {
    const userId = options.userId;
    if (!userId) {
      wx.showToast({ title: '用户ID错误', icon: 'none' });
      return;
    }
    const vehicleData = require('../../data/vehicleData.js'); 
    const user = vehicleData.swiperItems.find(item => item.userId === userId);
    if (user) {
      this.setData({ userInfo: user });
    } else {
      wx.showToast({ title: '未找到用户数据', icon: 'none' });
    }
  },

  switchTab(e) {
    this.setData({
      activeTab: e.currentTarget.dataset.tab
    });
  }
});