// pages/order-detail/index.js
Page({
  data: {
      order: {}
  },
  onLoad(options) {
      try {
          // 从本地存储中获取订单数据
          const orders = wx.getStorageSync('orders') || [];
          const orderId = parseInt(options.orderId);
          const order = orders.find(item => item.orderId === orderId);

          if (order) {
              // 格式化订单时间
              if (order.orderTime) {
                  order.formattedOrderTime = order.orderTime.split('T')[0];
              }
              this.setData({
                  order: order
              });
              console.log('订单详情数据加载成功:', order);
          } else {
              wx.showToast({ title: '未找到订单数据', icon: 'none' });
              console.error('未找到订单数据，订单ID:', orderId);
          }
      } catch (error) {
          wx.showToast({ title: '加载订单详情出错', icon: 'none' });
          console.error('加载订单详情出错:', error);
      }
  },
  // 支付订单
  payOrder() {
      const order = this.data.order;
      if (order) {
          // 更新订单状态
          order.status = '已付款';
          // 更新本地存储
          let orders = wx.getStorageSync('orders') || [];
          const index = orders.findIndex(o => o.orderId === order.orderId);
          if (index !== -1) {
              orders[index] = order;
              wx.setStorageSync('orders', orders);
          }
          wx.showToast({
              title: '支付成功',
              icon: 'success',
              duration: 2000,
              success: () => {
                  setTimeout(() => {
                      wx.navigateBack();
                  }, 1500);
              }
          });
      }
  },

  // 删除订单
  deleteOrder() {
    const order = this.data.order;
    if (order) {
      wx.showModal({
        title: '确认删除',
        content: '确定要删除该订单吗？',
        success: (res) => {
          if (res.confirm) {
            let orders = wx.getStorageSync('orders') || [];
            const index = orders.findIndex(o => o.orderId === order.orderId);
            if (index !== -1) {
              orders.splice(index, 1);
              wx.setStorageSync('orders', orders);
              wx.showToast({
                title: '订单删除成功',
                icon: 'success',
                duration: 2000,
                success: () => {
                  setTimeout(() => {
                    wx.navigateBack();
                  }, 1500);
                }
              });
            }
          }
        }
      });
    }
  }
});