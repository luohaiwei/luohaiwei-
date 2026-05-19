//vehicleData.js
module.exports = {
  // 达人数据
  swiperItems: [
    {
      image: '/images/avatar/avatar2.png',
      avatar: '/images/avatar/avatar2.png',
      name: '蟹老板',
      title: '达人也要玩玩具', 
      userId: 'user001',
      id: 'user001', 
      joinTime: '2025-05-19',
      follow: 4,
      fans: 2,
      likes: 16,
      carModelCount: 20,
      posts: [
        {
          postId: 'post001',
          createTime: '11天前',
          images: ['/images/car1.jpg', '/images/car1_1.jpg'],
          title: 'The Brave and the Bold Batmobile',
          brand: '风火轮',
          year: 2025,
          modelNo: 'HYY81',
          likes: 0,
          comments: 0,
          share: 0
        },
        {
          postId: 'post002',
          createTime: '16天前',
          images: ['/images/car2.jpg', '/images/car3.jpg'],
          title: 'Batman Forever Batmobile',
          brand: '风火轮',
          year: 2024,
          modelNo: 'HVJ39',
          likes: 1,
          comments: 2,
          share: 0
        }
      ]
    },
    {
      image: '/images/avatar/avatar1.png',
      avatar: '/images/avatar/avatar1.png',
      name: '匿名用户',
      title: '达人玩家',
      userId: 'user002',
      id: 'user002',
      joinTime: '2025-04-20',
      follow: 3,
      fans: 5,
      likes: 22,
      carModelCount: 15,
      posts: [
        {
          postId: 'post003',
          createTime: '7天前',
          images: ['/images/car2.jpg'],
          title: '闪电麦昆（麦昆日2024特别版）',
          brand: '多美卡',
          year: 2024,
          modelNo: 'XXX01',
          likes: 0,
          comments: 0,
          share: 0
        }
      ]
    },
    {
      image: '/images/avatar/avatar3.png',
      avatar: '/images/avatar/avatar3.png',
      name: '易燃易爆的🥚',
      title: '车模收藏家', 
      userId: 'user003',
      id: 'user003',
      joinTime: '2025-03-10',
      follow: 6,
      fans: 8,
      likes: 30,
      carModelCount: 25,
      posts: [
        {
          postId: 'post004',
          createTime: '5天前',
          images: ['/images/car3.jpg'],
          title: '财团M 80周年款',
          brand: '风火轮',
          year: 2025,
          modelNo: 'JBL68',
          likes: 0,
          comments: 0,
          share: 0
        }
      ]
    }
  ],
  // 推荐商品
  featuredProduct: {
    id: 1001,
    title: '风火轮 RLC 斯堪尼亚 Elite 600',
    brand: '风火轮_RLC',
    image: '/images/community_photos/topic1.jpg',
    imageUrl: '/images/community_photos/topic1.jpg', //imageUrl 字段
    wants: 0,
    haves: 1
  },
  // 商品列表 - 确保每个商品都有必要字段
  products: [
    {
      id: 1,
      type: 1, // MINI GT
      isPresale: true,
      image: "/images/Product_photos/MINI_GT2.jpg",
      imageUrl: "/images/Product_photos/MINI_GT2.jpg",
      name: "Jaguar C-X75 Black",
      title: "Jaguar C-X75 Black", 
      price: 76.5,
      stock: 15,
      description: "捷豹经典概念车，黑色涂装",
      brand: "MINI GT", 
      presaleStart: "2025-07-01",
      presaleEnd: "2025-07-31",
      officialDelivery: "2025-08-15"
    },
    {
      id: 2,
      type: 1, // MINI GT
      isPresale: true,
      image: "/images/Product_photos/MINI_GT1.jpg",
      imageUrl: "/images/Product_photos/MINI_GT1.jpg",
      name: 'Porsche 911 Dakar "Uncle Rally"',
      title: 'Porsche 911 Dakar "Uncle Rally"',
      price: 88.2,
      stock: 15,
      description: "保时捷达喀尔拉力赛特别版",
      brand: "MINI GT",
      presaleStart: "2025-07-01",
      presaleEnd: "2025-07-31",
      officialDelivery: "2025-08-15"
    },
    {
      id: 3,
      type: 1, // MINI GT
      isPresale: true,
      image: "/images/Product_photos/MINI_GT3.jpg",
      imageUrl: "/images/Product_photos/MINI_GT3.jpg",
      name: "Toyota Supra (A80) Top Secret GT-300",
      title: "Toyota Supra (A80) Top Secret GT-300",
      price: 76.5,
      stock: 15,
      description: "丰田经典跑车，特别改装版",
      brand: "MINI GT",
      presaleStart: "2025-07-01",
      presaleEnd: "2025-07-31",
      officialDelivery: "2025-08-15"
    },
    {
      id: 4,
      type: 2, // 风火轮
      isPresale: true,
      image: "/images/Product_photos/Hot_Wheels2.jpg",
      imageUrl: "/images/Product_photos/Hot_Wheels2.jpg",
      name: "Lamborghini Gallardo",
      title: "Lamborghini Gallardo",
      price: 66.5,
      stock: 15,
      description: "兰博基尼经典跑车",
      brand: "风火轮",
      presaleStart: "2025-07-01",
      presaleEnd: "2025-07-31",
      officialDelivery: "2025-08-15"
    },
    {
      id: 5,
      type: 2, 
      isPresale: true,
      image: "/images/Product_photos/Hot_Wheels1.jpg",
      imageUrl: "/images/Product_photos/Hot_Wheels1.jpg",
      name: "Stockar",
      title: "Stockar",
      price: 79.5,
      stock: 15,
      description: "经典肌肉车设计",
      brand: "风火轮",
      presaleStart: "2025-07-01",
      presaleEnd: "2025-07-31",
      officialDelivery: "2025-08-15"
    },
    {
      id: 6,
      type: 3, //风火轮RLC
      isPresale: true,
      image: "/images/Product_photos/Hot_Wheels_RLC.jpg",
      imageUrl: "/images/Product_photos/Hot_Wheels_RLC.jpg",
      name: "porsche 959(RLC)",
      title: "Porsche 959 (RLC)",
      price: 115.2,
      stock: 15,
      description: "保时捷经典跑车，收藏级",
      brand: "风火轮RLC",
      presaleStart: "2025-07-01",
      presaleEnd: "2025-07-31",
      officialDelivery: "2025-08-15"
    },
    {
      id: 7,
      type: 3, //风火轮RLC
      isPresale: false,
      image: "/images/explore_photos/hot-car1.jpg",
      imageUrl: "/images/explore_photos/hot-car1.jpg",
      name: "风火轮 RLC 斯堪尼亚",
      title: "风火轮 RLC 斯堪尼亚",
      price: 850.00,
      stock: 15,
      description: "带可拆卸拖车的精装模型",
      brand: "风火轮RLC",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 8,
      type: 4, //tarmac works
      isPresale: false,
      image: "/images/explore_photos/hot-car4.jpg",
      imageUrl: "/images/explore_photos/hot-car4.jpg",
      name: "Mercedes-AMG F1 W12",
      title: "Mercedes-AMG F1 W12",
      price: 107.00,
      stock: 15,
      description: "汉密尔顿冠军座驾模型",
      brand: "Tarmac Works",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 9,
      type: 4, //tarmac works
      isPresale: false,
      image: "/images/explore_photos/hot-car5.jpg",
      imageUrl: "/images/explore_photos/hot-car5.jpg",
      name: "Dodge Van OHIGE no PON",
      title: "Dodge Van OHIGE no PON",
      price: 105.00,
      stock: 15,
      description: "日本动漫联名款厢式车",
      brand: "Tarmac Works",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 10,
      type: 5, //INNO64
      isPresale: false,
      image: "/images/explore_photos/hot-car2.jpg",
      imageUrl: "/images/explore_photos/hot-car2.jpg",
      name: "MAZDA RX7(FD3S)",
      title: "MAZDA RX7(FD3S)",
      price: 142.00,
      stock: 15,
      description: "马自达转子发动机跑车",
      brand: "INNO64",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 11,
      type: 5, //INNO64
      isPresale: false,
      image: "/images/explore_photos/hot-car3.jpg",
      imageUrl: "/images/explore_photos/hot-car3.jpg",
      name: "R89C LE MANS 1989 No23",
      title: "R89C LE MANS 1989 No23",
      price: 176.00,
      stock: 15,
      description: "勒芒经典赛车复刻版",
      brand: "INNO64",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 12,
      type: 6, //kaidoHouse x mini gt
      isPresale: false,
      image: "/images/Product_photos/kaidoHouse1.jpg",
      imageUrl: "/images/Product_photos/kaidoHouse1.jpg",
      name: "Honde NSX Kaido Racing v2",
      title: "Honda NSX Kaido Racing v2",
      price: 106.00,
      stock: 15,
      description: "本田NSX赛车改装版",
      brand: "KaidoHouse x MINI GT",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 13,
      type: 6, //kaidoHouse x mini gt
      isPresale: false,
      image: "/images/Product_photos/kaidoHouse2.jpg",
      imageUrl: "/images/Product_photos/kaidoHouse2.jpg",
      name: "Nissan Skyline GT-R",
      title: "Nissan Skyline GT-R",
      price: 136.00,
      stock: 15,
      description: "日产经典跑车",
      brand: "KaidoHouse x MINI GT",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    },
    {
      id: 14,
      type: 7, //BBR
      isPresale: false,
      image: "/images/Product_photos/BBR1.jpg",
      imageUrl: "/images/Product_photos/BBR1.jpg",
      name: "Ferrari 499P car 50",
      title: "Ferrari 499P car 50",
      price: 1010.00,
      stock: 15,
      description: "法拉利赛车模型，树脂材质",
      brand: "BBR",
      presaleStart: null,
      presaleEnd: null,
      officialDelivery: null
    }
  ],
  // 轮播图数据
  topSwiperItems: [
    {
      id: 1,
      title: '风火轮 RLC 斯堪尼亚 Elite 600',
      image: '/images/community_photos/topic1.jpg',
      commentCount: 12,
      author: 'Alva',
      content: `斯堪尼亚，作为创新运输解决方案领域的领航者，重磅推出770s驾驶室，搭载迄今为止最为强劲的发动机。在缩小比例的车型设计中，精心打造了驾驶室铰链，旨在向您展示为这家商业卡车巨头提供强大动力的令人赞叹的 V8 发动机复制品。

Elite 64复制品，为长途旅行量身定制，配备定制的 Real Riders 车轮，并拥有可拆卸的双层拖车，最多能够容纳六辆压铸车。

Hot Wheels®Elite 64™斯堪尼亚 770s
亮点：配备可拆卸两层拖车，能够携带六辆 1:64 比例的车辆。
更多特色：前开式驾驶室铰链；拖车带有可折叠坡道。
车身色彩：珍珠白驾驶室，搭配亮红色拖车，以及银色金属坡道。
车身材质：ZAMAC 材质。
车轮：定制真实骑手车轮。
底盘：全金属底盘。
驾驶室车窗颜色：浅烟色。
驾驶室内饰颜色：黑色。
比例：1:64。
包装：采用带保护插件的全彩色封闭箱进行包装。
*需注意，拖车上展示的汽车不包含在内，且部分 Hot Wheels Elite 64 车型可能存在兼容性问题。`,
      publishTime: '2025-06-11 13:54:15'
    },
    {
      id: 2,
      title: 'TW 6月新车预售',
      image: '/images/community_photos/topic3.jpg',
      commentCount: 8,
      author: '蟹老板',
      content: 'TW 6月新车预售详细内容...',
      publishTime: '2025-06-12 10:30:22'
    },
    {
      id: 3,
      title: 'MINI GT 6月新车预售',
      image: '/images/community_photos/topic2.jpg',
      commentCount: 15,
      author: 'xwk',
      content: 'MINI GT 6月新车预预售细内容...',
      publishTime: '2025-06-10 15:45:33'
    }
  ],
  // 热门话题数据
  hotTopics: [
    {
      id: 1,
      title: '寻找车模摄影大师',
      image: '/images/community_photos/topics1.jpg',
      commentCount: 24,
      author: '社区官方',
      content: '寻找车模摄像大师活动详细内容...',
      publishTime: '2025-06-09 09:15:44',
      dateRange: '2024年12月31日 - 2025年12月31日',
      participants: 21,
      description: '用您的镜头讲述车模的故事。无论是静态展示，还是动态捕捉，期待看到您通过摄影赋予车模生命。',
      posts: [
        {
          id: 101,
          image: '/images/community_photos/topic4.jpg',
          title: '拍拍',
          author: 'xwk',
          likeCount: 10,
          content: '这是我最近拍的车模照片，大家觉得怎么样？',
          publishTime: '2025-06-15 09:30:22'
        },
        {
          id: 102,
          image: '/images/community_photos/topic8.jpg',
          title: '太帅了！',
          author: '匿名用户',
          likeCount: 5,
          content: '这辆车的设计真的太棒了，细节处理非常到位！',
          publishTime: '2025-06-14 14:20:15'
        },
        {
          id: 103,
          image: '/images/community_photos/topic5.jpg',
          title: '911',
          author: 'Maravic',
          likeCount: 4,
          content: '保时捷911经典车型，永远的经典！',
          publishTime: '2025-06-13 11:45:33'
        }
      ]
    },
    {
      id: 2,
      title: '晒出你今年最满意的小车',
      image: '/images/community_photos/topics2.jpg',
      commentCount: 36,
      author: '社区官方',
      content: '晒出你今年最满意的小车活动详细内容...',
      publishTime: '2025-06-08 14:20:55',
      dateRange: '2025年1月1日 - 2025年12月31日',
      participants: 36,
      description: '分享你今年收藏的最满意的小车模型，让我们一起欣赏！',
      posts: [
        {
          id: 201,
          image: '/images/community_photos/topic9.jpg',
          title: '相信每个男孩子都想拥有一台自己车子的模型吧',
          author: 'x',
          likeCount: 8,
          content: '从小就对汽车模型情有独钟，这是我最新的收藏！',
          publishTime: '2025-06-16 10:15:30'
        },
        {
          id: 202,
          image: '/images/community_photos/topic11.jpg',
          title: 'MAZDA RX7(FDS3)',
          author: '匿名用户',
          likeCount: 8,
          content: '马自达RX7经典跑车，转子发动机的传奇！',
          publishTime: '2025-06-15 16:40:18'
        },
        {
          id: 203,
          image: '/images/community_photos/topic12.jpg',
          title: '兰博基尼Revuelto',
          author: 'big trcuck',
          likeCount: 12,
          content: '兰博基尼最新款超级跑车，模型做工非常精细！',
          publishTime: '2025-06-14 13:25:45'
        }
      ]
    },
    {
      id: 3,
      title: '小车那些事',
      image: '/images/community_photos/topics3.jpg',
      commentCount: 42,
      author: '社区官方',
      content: '小车那些事话题详细内容...',
      publishTime: '2025-06-07 11:05:12',
      dateRange: '2025年1月1日 - 2025年12月31日',
      participants: 42,
      description: '聊聊关于小车模型的故事和趣事。',
      posts: [
        {
          id: 301,
          image: '/images/car1_1.jpg',
          title: '第一次收藏',
          author: '新手玩家',
          likeCount: 15,
          content: '这是我人生中收藏的第一辆小车模型，非常有纪念意义！',
          publishTime: '2025-06-17 09:10:22'
        },
        {
          id: 302,
          image: '/images/community_photos/topic13.jpg',
          title: '拓意青岛啤酒套装',
          author: '蟹老板',
          likeCount: 15,
          content: '限量版青岛啤酒主题小车模型，非常有特色！',
          publishTime: '2025-06-16 14:30:10'
        },
        {
          id: 303,
          image: '/images/community_photos/topic14.jpg',
          title: '第二贴',
          author: '不爱吃姜先生',
          likeCount: 15,
          content: '分享我最近收藏的另一辆经典小车模型。',
          publishTime: '2025-06-15 11:20:45'
        }
      ]
    }
  ],
  // 热门商品数据
  hotGoods: [
    {
      id: 1,
      name: 'Hot Wheels Elite 64 Scanin 770s',
      price: '850.00',
      imageUrl: '/images/explore_photos/hot-car1.jpg',
      description: '1:64比例精装模型，带可拆卸拖车，限量生产',
      stock: 15
    },
    {
      id: 2,
      name: 'MAZDA RX7(FD3S)',
      price: '142.00',
      imageUrl: '/images/explore_photos/hot-car2.jpg',
      description: '经典转子发动机跑车，精细内饰还原',
      stock: 32
    },
    {
      id: 3,
      name: 'R89C LE MANS 1989 NO23',
      price: '176.00',
      imageUrl: '/images/explore_photos/hot-car3.jpg',
      description: '勒芒经典赛车，复刻1989年冠军车型',
      stock: 8
    },
    {
      id: 4,
      name: 'Mercedes-AMG F1 W12',
      price: '1079.90',
      imageUrl: '/images/explore_photos/hot-car4.jpg',
      description: '1:18比例汉密尔顿冠军座驾，全碳纤维纹理',
      stock: 5
    },
    {
      id: 5,
      name: 'Dodge Van OHIGE no PON',
      price: '105.40',
      imageUrl: '/images/explore_photos/hot-car5.jpg',
      description: '日本动漫联名款，限量发售',
      stock: 21
    }
  ],
  
  // 功能导航数据
  features: [
    {
      id: 'garage',
      name: '爱车入库',
      icon: '/images/explore_photos/garage-icon.png',
      bgColor: '#FFE1E1'
    },
    {
      id: 'Talent',
      name: '达人圈',
      icon: '/images/explore_photos/Talent-icon.png',
      bgColor: '#E1F0FF'
    },
    {
      id: 'more',
      name: '更多车模',
      icon: '/images/explore_photos/more-icon.png',
      bgColor: '#E1FFE6'
    }
  ],
  // 精彩内容数据
  featuredPosts: [
    {
      id: 1,
      title: '风火轮 RLC 斯堪尼亚 Elite 600',
      image: '/images/community_photos/topic1.jpg',
      user: {
        name: 'Alva',
      },
      content: '风火轮 RLC 系列一直以精细做工著称，这款斯堪尼亚 Elite 600 模型更是其中的精品。模型采用全金属底盘，车身采用ZAMAC材质，细节处理非常到位。最吸引人的是它配备了可拆卸的两层拖车，能够携带六辆 1:64 比例的车辆。前开式驾驶室铰链设计让玩家可以轻松查看内部细节，包括精致的发动机复制品。整体配色采用珍珠白驾驶室搭配亮红色拖车，视觉效果非常出色。',
      publishTime: '2025-06-11 13:54:15'
    },
    {
      id: 2,
      title: 'MINI GT 6月新车预算',
      image: '/images/community_photos/topic2.jpg',
      user: {
        name: '蟹老板',
      },
      content: 'MINI GT 6月将推出多款新车模型，包括经典跑车和现代超跑。其中最令人期待的是保时捷911 GT3 RS 和 兰博基尼 Huracan STO。这些模型采用1:64比例，细节处理非常精细，包括可转向的前轮、精细的内饰和逼真的涂装。价格方面，预计每款模型在120-150元之间。作为MINI GT的忠实粉丝，我已经准备预购全套了！',
      publishTime: '2025-06-12 10:30:22'
    },
    {
      id: 3,
      title: 'TW 6月新车预算',
      image: '/images/community_photos/topic3.jpg',
      user: {
        name: '蟹老板',
      },
      content: 'TW模型品牌6月将推出多款经典车型，包括80年代的日本经典跑车和现代美式肌肉车。特别值得一提的是他们复刻的1985年丰田AE86，这款模型还原了头文字D中的经典造型。另一款值得关注的是道奇挑战者Hellcat，配备超大马力发动机的经典美式肌肉车。这些模型采用1:64比例，预计售价在100-130元之间。',
      publishTime: '2025-06-10 15:45:33'
    },
    {
      id: 4,
      title: '黄恐龙',
      image: '/images/community_photos/topic10.jpg',
      user: {
        name: 'xwk',
      },
      content: '这款被称为"黄恐龙"的模型是风火轮2025年的特别版，基于兰博基尼Countach设计。模型采用鲜艳的黄色涂装，搭配黑色条纹和夸张的尾翼，确实如恐龙般凶猛。最特别的是它的车门可以打开，这在1:64比例的模型中非常罕见。车轮采用定制真实骑手车轮，底盘为全金属材质。整体做工非常精细，绝对是收藏家们不可错过的精品。',
      publishTime: '2025-06-13 16:20:18'
    },
    {
      id: 5,
      title: '相信每个男孩子都想拥有一台自己车子的模型吧',
      image: '/images/community_photos/topic9.jpg',
      user: {
        name: '蟹老板',
      },
      content: '淘了好久 终于看到同款配置的了',
      publishTime: '2025-06-23 16:20:18'
    },
    {
        id: 6, 
        title: '带有视频的帖子',
        user: {
            name: '视频作者',
            avatar: '/images/avatar/avatar2.png'
        },
        image: '/images/car1.jpg', 
        content: '这是一个带有视频的帖子',
        publishTime: '2024-10-01',
        video: 'http://127.0.0.1:3000/001.mp4' 
    }
  ]
};