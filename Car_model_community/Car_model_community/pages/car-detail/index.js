const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    carInfo: null, 
    brandName: '',
    categoryName: '',
    loading: true
  },
  onLoad(options) {
    // 检查必要的参数
    if (!options.id || !options.name || !options.price) {
      this.handleError('缺少车模信息参数');
      return;
    }
    try {
      //URL参数构建车模信息对象
      const carInfo = {
        id: options.id,
        name: decodeURIComponent(options.name),
        price: parseFloat(options.price),
        imageUrl: options.image ? decodeURIComponent(options.image) : '/images/default-car.jpg',
        stock: options.stock ? parseInt(options.stock) : 0,
        description: options.description ? decodeURIComponent(options.description) : '暂无描述',
        brand: options.brand ? decodeURIComponent(options.brand) : '未知品牌',
        year: options.year ? options.year : '',
        modelNo: options.modelNo ? decodeURIComponent(options.modelNo) : ''
      };
      // 设置品牌和分类名称
      const brandCategory = this.getBrandAndCategory(carInfo.brand);
      this.setData({
        carInfo,
        brandName: brandCategory.brandName || carInfo.brand || '未知品牌',
        categoryName: brandCategory.categoryName || '未知分类',
        loading: false
      });
    } catch (e) {
      console.error('解析车模信息失败', e);
      this.handleError('车模信息格式错误');
    }
  },

  // 获取品牌和分类名称
  getBrandAndCategory(brand) {
    const brandMap = {
      'MINI GT': { brandName: 'MINI GT', categoryName: '合金车模' },
      '风火轮': { brandName: '风火轮', categoryName: '合金车模' },
      '风火轮RLC': { brandName: '风火轮RLC', categoryName: '收藏级' },
      'Tarmac Works': { brandName: 'Tarmac Works', categoryName: '合金车模' },
      'INNO64': { brandName: 'INNO64', categoryName: '合金车模' },
      'KaidoHouse x MINI GT': { brandName: 'KaidoHouse x MINI GT', categoryName: '联名款' },
      'BBR': { brandName: 'BBR', categoryName: '树脂车模' },
      '多美卡': { brandName: '多美卡', categoryName: '合金车模' }
    };

    return brandMap[brand] || { 
      brandName: brand || '未知品牌', 
      categoryName: '未知分类' 
    };
  },

  // 错误处理
  handleError(message) {
    console.error(message);
    wx.showToast({
      title: message,
      icon: 'none',
      duration: 3000
    });
    this.setData({ loading: false });
    
    // 2秒后返回上一页
    setTimeout(() => {
      wx.navigateBack();
    }, 2000);
  },

  // 点击"我有"按钮-跳转到爱车入库页面
  handleHave() {
    const { carInfo } = this.data;
    // 传递车模信息到爱车入库页面
    const carData = {
      brand: carInfo.brand,
      name: carInfo.name,
      modelNo: carInfo.modelNo || '',
      year: carInfo.year || '',
      image: carInfo.imageUrl
    };
    wx.navigateTo({
      url: `/pages/garage/garage?carInfo=${encodeURIComponent(JSON.stringify(carData))}`
    });
  },

  // 点击"购买"按钮 - 跳转到购买页面
  handleBuy() {
    const { carInfo } = this.data;
    wx.navigateTo({
      url: `/pages/goods-detail/index?id=${carInfo.id}&name=${encodeURIComponent(carInfo.name)}&price=${carInfo.price}&image=${encodeURIComponent(carInfo.imageUrl)}`
    });
  }
});