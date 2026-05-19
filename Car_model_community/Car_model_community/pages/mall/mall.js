const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    // 分类数据
    categories: [
      { id: 1, name: "MINI GT" },
      { id: 2, name: "风火轮" },
      { id: 3, name: "风火轮RLC" },
      { id: 4, name: "Tarmac Works" },
      { id: 5, name: "INNO64" },
      { id: 6, name: "KaidoHouse x MINI GT" },
      { id: 7, name: "BBR" },
    ],
    // 商品数据
    products: vehicleData.products, // 从ehicleData中获取商品数据
    currentCategory: 0, //"全部"分类
    filteredProducts: [], //筛选后的商品列表
    page: 1, 
    pageSize: 4, //商品数量
    hasMore: true,
    isLoading: false 
  },

  onLoad() {
    // 初始化显示所有商品
    this.loadProducts(0);
  },

  // 加载商品数据
  loadProducts(categoryId) {
    if (this.data.isLoading) return;
    this.setData({ isLoading: true });

    let filteredProducts = [];
    if (categoryId === 0) {
      filteredProducts = this.data.products;
    } else {
      // 根据分类ID筛选商品
      filteredProducts = this.data.products.filter(
        product => product.type === categoryId
      );
    }
    const start = (this.data.page - 1) * this.data.pageSize;
    const end = start + this.data.pageSize;
    const newProducts = filteredProducts.slice(start, end);
    if (newProducts.length < this.data.pageSize) {
      this.setData({ hasMore: false });
    }
    this.setData({
      currentCategory: categoryId,
      filteredProducts: this.data.page === 1 ? newProducts : [...this.data.filteredProducts, ...newProducts],
      isLoading: false
    });
  },

  // 切换分类
  switchCategory(e) {
    const categoryId = parseInt(e.currentTarget.dataset.id);
    this.setData({ page: 1, hasMore: true });
    this.loadProducts(categoryId);
  },

  // 查看商品详情
  viewGoodsDetail(e) {
    const id = e.currentTarget.dataset.id;
    const goods = this.data.products.find(item => item.id === id);

    if (goods) {
      wx.navigateTo({
        url: `/pages/goods-detail/index?id=${id}&name=${encodeURIComponent(goods.name)}&price=${goods.price}&image=${encodeURIComponent(goods.image)}`,
      });
    }
  },

  // 购买商品
  buyProduct(e) {
    if (!e || typeof e.stopPropagation!== 'function') {
      console.error('传递的参数不是有效的事件对象');
      return;
    }

    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const id = e.currentTarget.dataset.id;
    const goods = this.data.products.find(item => item.id === id);

    if (goods) {
      // 添加到订单
      let orders = wx.getStorageSync('orders') || [];
      orders.push({
        ...goods,
        orderId: Date.now(),
        status: '待付款',
        orderTime: new Date().toISOString()
      });
      wx.setStorageSync('orders', orders);

      wx.showToast({
        title: "已加入购物车",
        icon: "success",
        duration: 2000,
      });
    }

    // 阻止事件冒泡
    e.stopPropagation();
  },

  // 下拉刷新
  onPullDownRefresh() {
    wx.showToast({
      title: '正在刷新...',
      icon: 'loading'
    });
    this.setData({ page: 1, hasMore: true });
    this.loadProducts(this.data.currentCategory);
    setTimeout(() => {
      wx.stopPullDownRefresh();
      wx.showToast({
        title: '刷新成功',
        icon: 'success'
      });
    }, 1000);
  },

  // 上拉触底加载更多
  onReachBottom() {
    if (this.data.hasMore && !this.data.isLoading) {
      wx.showToast({
        title: '正在加载更多...',
        icon: 'loading'
      });
      this.setData({ page: this.data.page + 1 });
      this.loadProducts(this.data.currentCategory);
      setTimeout(() => {
        wx.hideToast();
      }, 1500);
    }
  }
});