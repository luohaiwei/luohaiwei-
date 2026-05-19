Page({
  data: {
    carModels: [], // 存储车库中的所有车模
    formData: {    // 数据对象
      brand: '',
      name: '',
      number: '',
      year: ''
    },
    tempImagePath: '',  // 临时存储上传图片路径
    showForm: true
  },

  onLoad(options) {
    // 从缓存中获取车库数据
    const carModels = wx.getStorageSync('garage') || [];
    this.setData({ 
      carModels,
      showForm: carModels.length === 0
    });
    // 如果从商品页跳转过来，预填表单
    if (options.carInfo) {
      try {
        const carInfo = JSON.parse(decodeURIComponent(options.carInfo));
        this.setData({
          formData: {
            brand: carInfo.brand || '',
            name: carInfo.name || '',
            number: carInfo.modelNo || '',
            year: carInfo.year || ''
          },
          tempImagePath: carInfo.image || ''
        });
      } catch (e) {
        console.error('解析车模信息失败', e);
      }
    }
  },

  // 选择图片使用微信APIwx.chooseMedia调起图片选择
  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.setData({
          tempImagePath: tempFilePath
        });
      },
      fail: (err) => {
        console.error('选择图片失败', err);
        wx.showToast({
          title: '选择图片失败',
          icon: 'none'
        });
      }
    });
  },

  // 表单输入处理
  onFormInput(e) {
    const { field } = e.currentTarget.dataset;
    const value = e.detail.value;

    this.setData({
      [`formData.${field}`]: value
    });
  },

  // 选择年份
  onYearChange(e) {
    this.setData({
      'formData.year': e.detail.value
    });
  },

  // 提交表单
  submitForm() {
    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    const { brand, name, number, year } = this.data.formData;
    const { tempImagePath } = this.data;
    const yearRegex = /^\d{4}$/;
    if (!brand || !name || !number || !year) {
      wx.showToast({
        title: '请填写完整信息',
        icon: 'none'
      });
      return;
    }
    if (!yearRegex.test(year)) {
      wx.showToast({
        title: '请输入有效的四位年份',
        icon: 'none'
      });
      return;
    }

    // 创建新车模对象
    const newCarModel = {
      id: Date.now(),
      brand,
      name,
      number,
      year,
      // 优先使用用户上传的图片，否则使用默认图片
      image: tempImagePath || 
             (brand.includes('风火轮') ? '/images/hotwheel-car.jpg' : 
             brand.includes('多美卡') ? '/images/tomica-car.jpg' : 
             '/images/default-car.jpg')
    };
    // 更新车库数据
    const updatedCarModels = [...this.data.carModels, newCarModel];
    // 保存到缓存
    wx.setStorageSync('garage', updatedCarModels);
    // 更新数据
    this.setData({
      carModels: updatedCarModels,
      showForm: false,
      formData: {
        brand: '',
        name: '',
        number: '',
        year: ''
      },
      tempImagePath: '' // 重置图片路径
    });

    wx.showToast({
      title: '入库成功',
      icon: 'success'
    });
  },

  // 添加入库新车模
  addNewCar() {
    this.setData({
      showForm: true,
      tempImagePath: ''
    });
  },

  // 查看我的车模
  viewGarage() {
    this.setData({
      showForm: false
    });
  },

  // 查看车模详情
  onCarModelTap(e) {
    const carModelId = e.currentTarget.dataset.id;
    const carModel = this.data.carModels.find(item => item.id == carModelId);

    if (carModel) {
      wx.navigateTo({
        url: `/pages/car-detail/index?id=${carModelId}&name=${encodeURIComponent(carModel.name)}&price=${carModel.price}&brand=${encodeURIComponent(carModel.brand)}&number=${carModel.number}&year=${carModel.year}&image=${encodeURIComponent(carModel.image)}`
      });
    }
  }
});