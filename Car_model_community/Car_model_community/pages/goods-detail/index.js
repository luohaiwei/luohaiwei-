const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    goodsInfo: null, //存储商品详细信息
    isLoading: true //加载状态标志
  },
  onLoad(options) {
    if (!options.id) {  //检查URL参数中是否有id参数
      wx.showToast({ title: '参数错误', icon: 'error' });
      setTimeout(() => wx.navigateBack(), 1500);
      return;
    }
    const goodsId = parseInt(options.id); //id转换为整数
    const goods = vehicleData.products.find(item => item.id === goodsId);
    if (goods) {  //如果找到商品，设置商品信息和加载状态
      this.setData({
        goodsInfo: {
          id: goodsId,
          name: goods.name,
          price: goods.price,
          brand: goods.brand,
          imageUrl: goods.imageUrl,
          description: goods.description,
          stock: goods.stock,
          modelNo: goods.modelNo || "",
          year: goods.year || "",
          isPresale: goods.isPresale,
          presaleStart: goods.presaleStart,
          presaleEnd: goods.presaleEnd,
          officialDelivery: goods.officialDelivery
        },
        isLoading: false
      });
    } else {
      wx.showToast({ title: '未找到商品数据', icon: 'none' });
    }
  },
  // 添加到购物车
  addToCart() {
    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    const { goodsInfo } = this.data; //获取商品信息
    if (goodsInfo) {
      // 从本地存储获取购物车数据
      let cart = wx.getStorageSync('cart') || [];
      // 检查是否已在购物车
      const existingItem = cart.find(item => item.id === goodsInfo.id);
      if (existingItem) {
        existingItem.quantity += 1; //如果存在则增加数量
      } else {
        cart.push({
          ...goodsInfo,
          quantity: 1
        });
      }
      // 更新本地存储中的购物车数据
      wx.setStorageSync('cart', cart);
      wx.showToast({
        title: "已添加到购物车",
        icon: "success",
        duration: 2000,
      });
    }
  },
  // 立即购买
  buyNow() {
    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    const { goodsInfo } = this.data;
    if (goodsInfo) {
      // 生成订单并保存到order缓存中
      const order = {
        orderId: Date.now(),
        goods: [goodsInfo],
        totalPrice: goodsInfo.price,//使用当前时间戳作为订单ID
        status: '待付款',
        // 获取当前时间并转换为 ISO 格式
        orderTime: new Date().toISOString() 
      };
      // 保存订单
      let orders = wx.getStorageSync('orders') || [];
      orders.push(order);
      wx.setStorageSync('orders', orders);
      // 如果商品有品牌和名称，则添加到用户车库,添加到车库数据存储在本地
      if (goodsInfo.brand && goodsInfo.name) {
        let garage = wx.getStorageSync('garage') || [];
        garage.push({
          id: Date.now(),
          brand: goodsInfo.brand,
          name: goodsInfo.name,
          modelNo: goodsInfo.modelNo || "",
          year: goodsInfo.year || "",
          image: goodsInfo.imageUrl,
          price: goodsInfo.price
        });
        wx.setStorageSync('garage', garage);
      }
      // 跳转到订单详情
      wx.navigateTo({
        url: `/pages/order-detail/index?orderId=${order.orderId}`
      });
    }
  }
});