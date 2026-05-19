Page({
  data: {
    isLoggedIn: false, //登录状态
    userId: null,
    userInfo: {},
    garageCount: 0, // 车库车模数量
    wishlistCount: 0, //心愿单车模数量
    orderCount: 0, // 订单
    cartCount: 0, // 购物车数量
    followCount: 10, // 关注
    fanCount: 100, // 粉丝
    likeCount: 500, 
    showFeedbackPopup: false,
    feedbackContent: '',
    showCarSupplementPopup: false,
    carSupplementForm: {
      brand: '',
      name: '',
      number: '',
      year: ''
    }
  },

  onLoad() {
    // 检查登录状态
    const userId = wx.getStorageSync('userId');
    if (userId) {
      this.setData({
        isLoggedIn: true,
        userId,
      });
      this.fetchUserInfo();
      this.fetchGarageCount();
      this.fetchWishlistCount();
      this.fetchOrderCount(); // 获取订单数量
      this.fetchCartCount(); // 获取购物车数量
    }
  },

  onShow() {
    const userId = wx.getStorageSync('userId');
    if (userId) {
      this.fetchGarageCount();
      this.fetchWishlistCount();
      this.fetchOrderCount();
      this.fetchCartCount();
    }
  },

  // 登录函数，模拟登录注册
  login() {
    wx.showModal({
      title: '模拟登录注册',
      content: '点击确定完成登录注册',
      success: (res) => {
        if (res.confirm) {
          const mockUserId = '123456';
          wx.setStorageSync('userId', mockUserId);
          this.setData({
            isLoggedIn: true,
            userId: mockUserId,
          });
          this.fetchUserInfo();
          this.fetchGarageCount();
          this.fetchWishlistCount();
          this.fetchOrderCount(); // 获取订单数量
          this.fetchCartCount(); // 获取购物车数量
        }
      }
    });
  },

  // 退出登录函数
  logout() {
    wx.removeStorageSync('userId');
    this.setData({
      isLoggedIn: false,
      userId: null,
      userInfo: {}
    });
  },

  // 获取用户信息
  fetchUserInfo() {
    const mockUserInfo = {
      nickname: '测试用户',
      id: '2593251870',
      joinDate: '2025-06-21',
    };
    this.setData({
      userInfo: mockUserInfo,
    });
  },

  // 获取车库车模数量
  fetchGarageCount() {
    const carModels = wx.getStorageSync('garage') || [];
    this.setData({
      garageCount: carModels.length,
    });
  },

  // 获取心愿单车模数量
  fetchWishlistCount() {
    const wishlist = wx.getStorageSync('wishlist') || [];
    this.setData({
      wishlistCount: wishlist.length,
    });
  },

  // 获取订单数量
  fetchOrderCount() {
    const orders = wx.getStorageSync('orders') || [];
    this.setData({
      orderCount: orders.length,
    });
  },
  
  // 获取购物车数量
  fetchCartCount() {
    const cart = wx.getStorageSync('cart') || [];
    this.setData({
      cartCount: cart.reduce((total, item) => total + item.quantity, 0)
    });
  },

  // 点击我的车库
  navigateToGarage() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    wx.navigateTo({ url: '/pages/garage/garage?showGarage=true' });
  },

  // 点击心愿单
  navigateToWishlist() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    wx.navigateTo({ url: '/pages/wishlist/wishlist' });
  },

  // 点击订单
  navigateToOrders() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    wx.navigateTo({ url: '/pages/orders/orders' });
  },
  
  // 点击购物车
  navigateToCart() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    wx.navigateTo({ url: '/pages/cart/cart' });
  },

  // 点击关注、粉丝、获赞
  showPopup(e) {
    const type = e.currentTarget.dataset.type;
    let message = '';
    switch (type) {
      case 'follow':
        message = `你有 ${this.data.followCount} 个关注`;
        break;
      case 'fan':
        message = `你有 ${this.data.fanCount} 个粉丝`;
        break;
      case 'like':
        message = `你获得了 ${this.data.likeCount} 个赞`;
        break;
    }
    wx.showModal({
      title: '提示',
      content: message,
      showCancel: false,
    });
  },

  // 点击爱车入库
  navigateToAddCar() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    wx.navigateTo({ url: '/pages/garage/garage' });
  },

  // 点击关于我们
  showAboutUs() {
    wx.showModal({
      title: '关于我们',
      content: '软工2408（专升本）罗海维24055060335',
      showCancel: false
    });
  },

  // 显示意见反馈弹窗
  showFeedback() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    this.setData({ showFeedbackPopup: true });
  },

  // 意见反馈输入
  onFeedbackInput(e) {
    this.setData({ feedbackContent: e.detail.value });
  },

  // 提交意见反馈
  submitFeedback() {
    const feedback = this.data.feedbackContent.trim();
    if (!feedback) {
      wx.showToast({ title: '请输入反馈内容', icon: 'none' });
      return;
    }
    console.log('【意见反馈提交】', { feedback, time: new Date().toLocaleString() });
    wx.showToast({ title: '提交成功', icon: 'success' });
    this.setData({ showFeedbackPopup: false, feedbackContent: '' });
  },

  // 隐藏意见反馈弹窗
  hideFeedback() {
    this.setData({ showFeedbackPopup: false });
  },

  // 显示车模补充弹窗
  showCarSupplement() {
    if (!this.data.isLoggedIn) {
      this.login();
      return;
    }
    this.setData({ 
      showCarSupplementPopup: true,
      carSupplementForm: { brand: '', name: '', number: '', year: '' }
    });
  },

  // 车模补充表单输入
  onCarSupplementInput(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ 
      [`carSupplementForm.${field}`]: e.detail.value 
    });
  },

  // 车模补充年份选择
  onCarSupplementYearChange(e) {
    this.setData({ 
      'carSupplementForm.year': e.detail.value 
    });
  },

  // 提交车模补充表单
  submitCarSupplement() {
    const { brand, name, number, year } = this.data.carSupplementForm;
    if (!brand || !name || !number || !year) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' });
      return;
    }
    console.log('【车模补充提交】', {
      brand, name, number, year,
      time: new Date().toLocaleString()
    });
    wx.showToast({ title: '提交成功', icon: 'success' });
    this.setData({ showCarSupplementPopup: false });
  },

  // 隐藏车模补充弹窗
  hideCarSupplement() {
    this.setData({ showCarSupplementPopup: false });
  }
});