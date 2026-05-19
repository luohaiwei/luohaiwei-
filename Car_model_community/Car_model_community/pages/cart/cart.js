// pages/cart/cart.js
Page({
  data: {
    cartItems: [], // 购物车商品数据，假设结构含id、name、price、quantity、imageUrl
    totalPrice: 0
  },

  onLoad() {
    this.loadCart();
  },

  // 加载购物车数据
  loadCart() {
    const cartItems = wx.getStorageSync('cart') || [];
    const totalPrice = cartItems.reduce(
      (total, item) => total + item.price * item.quantity, 
      0
    );
    this.setData({
      cartItems,
      totalPrice
    });
  },

  // 增加商品数量
  increaseQuantity(e) {
    const index = e.currentTarget.dataset.index;
    const cartItems = [...this.data.cartItems];
    cartItems[index].quantity += 1;
    this.updateCart(cartItems);
  },
  // 减少商品数量（最少为1）
  decreaseQuantity(e) {
    const index = e.currentTarget.dataset.index;
    const cartItems = [...this.data.cartItems];
    if (cartItems[index].quantity > 1) {
      cartItems[index].quantity -= 1;
      this.updateCart(cartItems);
    }
  },

  // 删除商品
  deleteItem(e) {
    const index = e.currentTarget.dataset.index;
    const cartItems = [...this.data.cartItems];
    cartItems.splice(index, 1);
    this.updateCart(cartItems);
  },

  // 更新购物车（同步缓存 + 重新计算总价）
  updateCart(cartItems) {
    const totalPrice = cartItems.reduce(
      (total, item) => total + item.price * item.quantity, 
      0
    );
    wx.setStorageSync('cart', cartItems);
    this.setData({
      cartItems,
      totalPrice
    });
  },

  // 结算
    checkout() {
      const userId = wx.getStorageSync('userId');
      if (!userId) {
        wx.showToast({ title: '请先登录', icon: 'none' });
        return;
      }

      if (this.data.cartItems.length === 0) {
        wx.showToast({ title: '购物车为空', icon: 'none' });
        return;
      }

      // 构建确认提示信息
      let confirmMessage = '确认提交以下商品订单：\n';
      this.data.cartItems.forEach(item => {
        confirmMessage += `${item.name} x ${item.quantity}，单价：¥${item.price}\n`;
      });
      confirmMessage += `总价：¥${this.data.totalPrice}`;

      wx.showModal({
        title: '确认订单',
        content: confirmMessage,
        success: (res) => {
          if (res.confirm) {
            // 用户点击确认，生成订单
            const order = {
              orderId: Date.now(),
              goods: this.data.cartItems,
              totalPrice: this.data.totalPrice,
              status: '待付款',
              orderTime: new Date().toISOString()
            };

            // 保存订单
            let orders = wx.getStorageSync('orders') || [];
            orders.push(order);
            wx.setStorageSync('orders', orders);

            // 将购买的车模添加到车库
            let garage = wx.getStorageSync('garage') || [];
            this.data.cartItems.forEach(item => {
              for (let i = 0; i < item.quantity; i++) {
                garage.push({
                  id: Date.now(),
                  brand: item.brand,
                  name: item.name,
                  modelNo: item.modelNo || "",
                  year: item.year || "",
                  image: item.imageUrl,
                  price: item.price
                });
              }
            });
            wx.setStorageSync('garage', garage);

            // 清空购物车
            wx.setStorageSync('cart', []);
            this.setData({
              cartItems: [],
              totalPrice: 0
            });

            wx.showToast({
              title: '订单提交成功',
              icon: 'success',
              duration: 2000
            });
          } else if (res.cancel) {
            // 用户点击取消
            wx.showToast({
              title: '订单未提交',
              icon: 'none',
              duration: 2000
            });
          }
        }
      });
    }
});