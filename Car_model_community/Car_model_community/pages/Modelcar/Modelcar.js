const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    swiperItems: [],
    featuredProduct: null,
    products: [],
    activeCategory: 'all',
    filteredSwiperItems: [],
    filteredFeaturedProduct: null,
    filteredProducts: [],
    isFiltering: false,
    searchKeyword: '',
    // 分页
    page: 1,
    pageSize: 6,
    hasMore: true,
    isLoading: false
  },

  onLoad(options) {
    // 初始化加载数据
    this.loadInitialData();
    //从URL参数中获取传递的关键词
    const keyword = options.keyword;
    if (keyword) {
      // 如果有关键词，将其设置到页面数据的 searchKeyword 中
      this.setData({
        searchKeyword: keyword
      });
      // 调用搜索函数进行搜索
      this.onSearch();
    }
  },

  // 加载初始数据
  loadInitialData() {
    wx.showLoading({ title: '加载中...' });
    // 模拟网络请求延迟
    setTimeout(() => {
      // 将从数据文件中获取的数据设置到页面数据中
      this.setData({
        swiperItems: vehicleData.swiperItems,
        featuredProduct: vehicleData.featuredProduct,
        // 确保所有商品都有必要字段
        products: vehicleData.products.map(item => ({
          ...item,
          modelNo: item.modelNo || "",
          year: item.year || ""
        })),
        activeCategory: 'all',
        filteredSwiperItems: vehicleData.swiperItems,
        filteredFeaturedProduct: vehicleData.featuredProduct,
        // 初始加载第一页数据
        filteredProducts: vehicleData.products.slice(0, this.data.pageSize),
        // 重置分页状态
        page: 1,
        hasMore: vehicleData.products.length > this.data.pageSize
      });
      wx.hideLoading();
    }, 800);
  },
  // 切换分类
  switchCategory(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ 
      activeCategory: category,
      // 重置分页状态
      page: 1,
      hasMore: true
    });
    // 分类过滤逻辑
    let filteredSwiperItems = [];
    let filteredFeaturedProduct = null;
    let filteredProducts = [];

    if (category === 'all') {
      // 显示所有内容
      filteredSwiperItems = this.data.swiperItems;
      filteredFeaturedProduct = this.data.featuredProduct;
      // 加载第一页数据
      filteredProducts = this.data.products.slice(0, this.data.pageSize);
    } else {
      // 按品牌过滤
      // 达人数据过滤（使用品牌或名称匹配）
      filteredSwiperItems = this.data.swiperItems.filter(item => 
        (item.brand && item.brand.includes(category)) || 
        (item.name && item.name.includes(category))
      );
      // 推荐商品过滤
      filteredFeaturedProduct = 
        (this.data.featuredProduct.brand && this.data.featuredProduct.brand.includes(category)) ||
        (this.data.featuredProduct.title && this.data.featuredProduct.title.includes(category)) ? 
        this.data.featuredProduct : null;
      // 商品列表过滤
      const allFilteredProducts = this.data.products.filter(product => 
        (product.brand && product.brand.includes(category)) ||
        (product.name && product.name.includes(category))
      );
      // 加载第一页数据
      filteredProducts = allFilteredProducts.slice(0, this.data.pageSize);
      // 更新是否有更多数据
      this.setData({
        hasMore: allFilteredProducts.length > this.data.pageSize
      });
    }
    
    this.setData({
      filteredSwiperItems,
      filteredFeaturedProduct,
      filteredProducts
    });
  },

  // 查看车模详情
  onProductTap(e) {
    const id = e.currentTarget.dataset.id;
    const product = this.data.products.find(item => item.id == id);
    
    if (product) {
      wx.navigateTo({
        url: `/pages/car-detail/index?id=${id}` +
            `&name=${encodeURIComponent(product.name)}` +
            `&price=${product.price}` +
            `&image=${encodeURIComponent(product.imageUrl)}` +
            `&brand=${encodeURIComponent(product.brand)}` +
            `&stock=${product.stock}` +
            `&description=${encodeURIComponent(product.description || '暂无描述')}`
      });
    }
  },

  // 推荐商品点击方法
  onFeaturedProductTap(e) {
    const product = this.data.filteredFeaturedProduct;
    if (product) {
      wx.navigateTo({
        url: `/pages/car-detail/index?id=${product.id}&name=${encodeURIComponent(product.title)}&price=${product.price}&image=${encodeURIComponent(product.imageUrl)}`
      });
    }
  },
  //通过onSearchInput函数监听搜索框的输入事件，将输入的关键词存储在searchKeyword中。
  onSearchInput(e) {
    this.setData({
      searchKeyword: e.detail.value
    });
  },
  // 当点击搜索按钮时，调用onSearch函数进行搜索。该函数会根据当前的分类和搜索关键词对数据进行过滤，并更新页面的data。
  onSearch() {
    const keyword = this.data.searchKeyword.trim();
    const category = this.data.activeCategory; 
    if (category === 'all') {
      var swiperItems = this.data.swiperItems;
      var featuredProduct = this.data.featuredProduct;
      var products = this.data.products;
    } else {
      var swiperItems = this.data.swiperItems.filter(item => 
        (item.brand && item.brand.includes(category)) || 
        (item.name && item.name.includes(category))
      );
      var featuredProduct = 
        (this.data.featuredProduct.brand && this.data.featuredProduct.brand.includes(category)) ||
        (this.data.featuredProduct.title && this.data.featuredProduct.title.includes(category)) ? 
        this.data.featuredProduct : null;
      var products = this.data.products.filter(product => 
        (product.brand && product.brand.includes(category)) ||
        (product.name && product.name.includes(category))
      );
    }

    if (keyword) {
      const filteredSwiperItems = swiperItems.filter(item => {
        return (item.title && item.title.includes(keyword)) || 
               (item.brand && item.brand.includes(keyword)) ||
               (item.name && item.name.includes(keyword));
      });
      
      const filteredFeaturedProduct = featuredProduct && 
        ((featuredProduct.title && featuredProduct.title.includes(keyword)) || 
         (featuredProduct.brand && featuredProduct.brand.includes(keyword))) ? 
        featuredProduct : null;
      
      const filteredProducts = products.filter(product => {
        return (product.name && product.name.includes(keyword)) || 
               (product.brand && product.brand.includes(keyword));
      });

      this.setData({
        filteredSwiperItems,
        filteredFeaturedProduct,
        filteredProducts: filteredProducts.slice(0, this.data.pageSize),
        page: 1,
        hasMore: filteredProducts.length > this.data.pageSize
      });
    } else {
      // 搜索框清空时数据还原
      this.switchCategory({currentTarget: {dataset: {category: this.data.activeCategory}}});
    }
  },
  
  // 处理"我有"按钮点击
  handleHave(e) {
    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const id = e.currentTarget.dataset.id;
    // 从商品列表中查找商品
    const product = this.data.products.find(item => item.id == id);
    
    if (!product) return;
    
    // 构建车模信息对象
    const carInfo = {
      brand: product.brand,
      name: product.name,
      modelNo: product.modelNo || "",
      year: product.year || "",
      image: product.imageUrl
    };
    
    // 跳转到爱车入库页面
    wx.navigateTo({
      url: `/pages/garage/garage?carInfo=${encodeURIComponent(JSON.stringify(carInfo))}`
    });
  },
  
  // 处理"购买"按钮点击
  handleBuy(e) {
    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    const id = e.currentTarget.dataset.id;
    const product = this.data.products.find(item => item.id == id);
    
    if (product) {
      wx.navigateTo({  //传递商品数据到详情页id价格名称图片
        url: `/pages/goods-detail/index?id=${id}&name=${encodeURIComponent(product.name)}&price=${product.price}&image=${encodeURIComponent(product.imageUrl)}`
      });
    }
  },
  
  // 跳转到用户空间
  goToUserSpace(e) {
    const index = e.currentTarget.dataset.index;
    const expert = this.data.filteredSwiperItems[index];
    if (expert && expert.userId) {
      wx.navigateTo({
        url: `/pages/user-space/index?userId=${expert.userId}`
      });
    } else {
      wx.showToast({
        title: '用户信息错误',
        icon: 'none'
      });
    }
  },
  
  // 下拉刷新
  onPullDownRefresh() {
    wx.showNavigationBarLoading(); // 显示顶部加载动画
    // 模拟网络请求
    setTimeout(() => {
      // 重新加载初始数据
      this.loadInitialData();
      
      wx.stopPullDownRefresh(); // 停止下拉刷新动画
      wx.hideNavigationBarLoading(); // 隐藏顶部加载动画
      wx.showToast({
        title: '刷新成功',
        icon: 'success'
      });
    }, 1000);
  },
  
  // 上拉触底加载更多
  onReachBottom() {
    // 如果没有更多数据或正在加载，则返回
    if (!this.data.hasMore || this.data.isLoading) return;
    
    this.setData({ isLoading: true });
    
    wx.showLoading({ title: '加载中...' });
    
    // 模拟网络请求延迟
    setTimeout(() => {
      // 计算当前分类下的所有商品
      let allProducts = [];
      
      if (this.data.activeCategory === 'all') {
        allProducts = this.data.products;
      } else {
        allProducts = this.data.products.filter(product => 
          (product.brand && product.brand.includes(this.data.activeCategory)) ||
          (product.name && product.name.includes(this.data.activeCategory))
        );
      }
      
      // 如果有搜索关键词，进一步过滤
      if (this.data.searchKeyword) {
        allProducts = allProducts.filter(product => 
          (product.name && product.name.includes(this.data.searchKeyword)) || 
          (product.brand && product.brand.includes(this.data.searchKeyword))
        );
      }
      
      // 计算新一页的数据
      const start = this.data.page * this.data.pageSize;
      const newProducts = allProducts.slice(start, start + this.data.pageSize);
      
      if (newProducts.length === 0) {
        this.setData({
          hasMore: false,
          isLoading: false
        });
      } else {
        this.setData({
          filteredProducts: [...this.data.filteredProducts, ...newProducts],
          page: this.data.page + 1,
          isLoading: false
        });
      }
      
      wx.hideLoading();
    }, 1500);
  }
});