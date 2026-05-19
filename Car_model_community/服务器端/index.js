let fs = require('fs');
var express = require('express')


// 读取JSON数据
let jsonStr = fs.readFileSync('./data.json', {
  encoding: 'utf8'
});

let data = JSON.parse(jsonStr);

// 创建Web服务器对象
var app = express();

// 静态资源处理
app.use(express.static('public'));


// 配置跨域
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  next();
});

// 获取视频数据的接口
app.get('/videos', (req, res) => {
  const page = parseInt(req.query.page) || 1;
  const pageSize = parseInt(req.query.pageSize) || 10;
  const videos = data.videos || []; // 从data对象中获取videos数组
  
  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  const currentPageVideos = videos.slice(start, end);
  
  res.setHeader('X-Total-Count', videos.length);
  res.send(currentPageVideos);
});

app.listen(3000, () => {
  console.log('服务器启动成功，地址为：http://127.0.0.1:3000');
});