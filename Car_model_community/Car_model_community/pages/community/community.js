const vehicleData = require('../../data/vehicleData.js');

Page({
  data: {
    searchKeyword: '',
    topSwiperItems: [],
    hotTopics: [],
    featuredPosts: [],
    filteredFeaturedPosts: [],
    isLoading: false,
    hasMore: true,
    page: 1,
    pageSize: 4,
    isSearching: false,
    showClear: false
  },
  
  onLoad() {
    this.setData({
      topSwiperItems: vehicleData.topSwiperItems,
      hotTopics: vehicleData.hotTopics,
      featuredPosts: this.getInitialPosts()
    });
    this.setData({ isLoading: false });
    // 检查poster属性是否为网络地址
    this.data.featuredPosts.forEach(post => {
      if (post.video && post.image) {
          if (!post.image.startsWith('http')) {
              console.warn(`poster 属性不是网络地址: ${post.image}`);
          }
      }
  });
},
  
  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({
      searchKeyword: keyword,
      showClear: keyword.length > 0
    });
    
    if (keyword.trim()) {
      this.performSearch(keyword);
    } else {
      this.setData({
        isSearching: false,
        filteredFeaturedPosts: []
      });
    }
  },
  
  clearSearch() {
    this.setData({
      searchKeyword: '',
      isSearching: false,
      filteredFeaturedPosts: [],
      showClear: false
    });
  },
  
  performSearch(keyword) {
    const normalizedKeyword = keyword.toLowerCase().trim();
    
    if (!normalizedKeyword) {
      this.setData({
        isSearching: false,
        filteredFeaturedPosts: []
      });
      return;
    }
    
    const searchResults = vehicleData.featuredPosts.filter(post => {
      const titleMatch = post.title.toLowerCase().includes(normalizedKeyword);
      const contentMatch = post.content && post.content.toLowerCase().includes(normalizedKeyword);
      const authorMatch = post.user.name && post.user.name.toLowerCase().includes(normalizedKeyword);
      
      return titleMatch || contentMatch || authorMatch;
    });
    
    this.setData({
      isSearching: true,
      filteredFeaturedPosts: searchResults
    });
  },
    onVideoTap(e) {
      const id = parseInt(e.currentTarget.dataset.id);
    const post = this.data.isSearching 
      ? this.data.filteredFeaturedPosts.find(item => item.id === id)
      : this.data.featuredPosts.find(item => item.id === id);
  },
  
  onSearch() {
    const keyword = this.data.searchKeyword.trim();
    
    if (!keyword) {
      wx.showToast({
        title: '请输入搜索关键词',
        icon: 'none'
      });
      return;
    }
    
    this.performSearch(keyword);
  },
  
  onSwiperTap(e) {
    const index = e.currentTarget.dataset.index;
    const topic = this.data.topSwiperItems[index];
    
    if (!topic) return;
    
    wx.navigateTo({
      url: `/pages/post/detail?id=${topic.id
            }&title=${encodeURIComponent(topic.title)
            }&author=${encodeURIComponent(topic.author)
            }&image=${encodeURIComponent(topic.image)
            }&content=${encodeURIComponent(topic.content)
            }&publishTime=${encodeURIComponent(topic.publishTime)}`,
    });
  },
  
  onHotTopicTap(e) {
    const id = parseInt(e.currentTarget.dataset.id);
    const topic = this.data.hotTopics.find(item => item.id === id);
    
    if (!topic) {
      wx.showToast({ title: '话题不存在', icon: 'none' });
      return;
    }
    
    wx.navigateTo({
      url: `/pages/topic/detail?topic=${encodeURIComponent(JSON.stringify(topic))}`
    });
  },
  
  onFeaturedTap(e) {
    const id = parseInt(e.currentTarget.dataset.id);
    const post = this.data.isSearching 
      ? this.data.filteredFeaturedPosts.find(item => item.id === id)
      : this.data.featuredPosts.find(item => item.id === id);
    
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
  
  onPullDownRefresh() {
    this.clearSearch();
    
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
  
  onReachBottom() {
    if (!this.data.hasMore || this.data.isLoading || this.data.isSearching) return;
    
    this.setData({ isLoading: true });
    
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
  
  getInitialPosts() {
    return vehicleData.featuredPosts.slice(0, this.data.pageSize);
  },
  //从整个数据集获取
  getMorePosts() {
    const start = this.data.page * this.data.pageSize;
    return vehicleData.featuredPosts.slice(start, start + this.data.pageSize);
  }
});