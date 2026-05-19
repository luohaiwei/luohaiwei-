Page({
  data: {
    orders: [],
    loading: true
  },

  onLoad() {
    this.loadOrders();
  },

  loadOrders() {
    const orders = wx.getStorageSync('orders') || [];
    const formattedOrders = orders.map(order => {
        if (order.orderTime) {
            order.formattedOrderTime = order.orderTime.split('T')[0];
        }
        return order;
    });
    this.setData({
      orders: formattedOrders,
      loading: false
    });
  },

  onOrderTap(e) {
    const orderId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/order-detail/index?orderId=${orderId}`
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadOrders();
    wx.stopPullDownRefresh();
  },

  // 跳转到商城
  goToMall() {
    wx.switchTab({
      url: '/pages/mall/mall' 
    });
  }
});