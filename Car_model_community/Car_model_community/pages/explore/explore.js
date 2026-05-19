const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    hotGoods: [],
    featuredPosts: [],
    features: [],
    isLoading: false,
    hasMore: true,
    page: 1,
    pageSize: 8
  },

  onLoad() {
    // 初始化数据
    this.setData({
      hotGoods: vehicleData.hotGoods,
      features: vehicleData.features,
      featuredPosts: this.getInitialPosts()
    });
  },

  // 查看热门商品详情
  viewGoodsDetail(e) {
    const goodsId = e.currentTarget.dataset.id;
    const goods = this.data.hotGoods.find(item => item.id === goodsId);
    if (!goods) return;
    wx.navigateTo({
      url: `/pages/goods-detail/index?id=${goodsId}&name=${encodeURIComponent(goods.name)}&price=${goods.price}&image=${encodeURIComponent(goods.imageUrl)}`
    });
  },

  // 导航到功能页面
  navigateToFeature(e) {
    const feature = e.currentTarget.dataset.feature;
    switch(feature) {
      case 'garage':
        //调用 wx.navigateTo 方法进行页面跳转
        wx.navigateTo({ url: '/pages/garage/garage' });
        break;
      case 'Talent':  
        wx.navigateTo({ url: '/pages/Talent/Talent' });
        break;
      case 'more':
        wx.switchTab({ url: '/pages/Modelcar/Modelcar' });
        break;
      default:
        wx.navigateTo({ url: '/pages/index/index' });
    }
  },

  // 查看更多热门商品 - 跳转到商城
  viewMoreHotGoods() {
    wx.switchTab({
      url: '/pages/mall/mall'
    });
  },

  // 处理精彩内容点击事件
  onFeaturedTap(e) {
    const id = parseInt(e.currentTarget.dataset.id);
    const post = this.data.featuredPosts.find(item => item.id === id);

    if (!post) {
      wx.showToast({ title: '内容加载失败', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/post/detail?id=${id
            }&title=${encodeURIComponent(post.title)
            }&author=${encodeURIComponent(post.user.name)
            }&image=${encodeURIComponent(post.image)
            }&content=${encodeURIComponent(post.content)
            }&publishTime=${encodeURIComponent(post.publishTime)
            }&video=${encodeURIComponent(post.video || '')}`,
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    // 重置数据
    this.setData({
      featuredPosts: this.getInitialPosts(),
      page: 1,
      hasMore: true
    });

    setTimeout(() => {
      wx.stopPullDownRefresh();
      wx.showToast({
        title: '刷新成功',
        icon: 'success'
      });
    }, 1000);
  },

  // 上拉加载更多
  onReachBottom() {
    if (!this.data.hasMore || this.data.isLoading) return;

    this.setData({ isLoading: true });

    // 模拟加载更多数据
    setTimeout(() => {
      const newPosts = this.getMorePosts();
      if (newPosts.length === 0) {
        this.setData({
          hasMore: false,
          isLoading: false
        });
        return;
      }

      this.setData({
        featuredPosts: [...this.data.featuredPosts, ...newPosts],
        page: this.data.page + 1,
        isLoading: false
      });
    }, 1500);
  },

  // 获取初始数据
  getInitialPosts() {
    return vehicleData.featuredPosts.slice(0, this.data.pageSize);
  },

  // 获取更多数据
  getMorePosts() {
    const start = this.data.page * this.data.pageSize;
    return vehicleData.featuredPosts.slice(start, start + this.data.pageSize);
  }
});