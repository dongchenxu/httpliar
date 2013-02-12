Http Liar（说谎者）
========

![Logo](http://pic.yupoo.com/oldmanpushcart/CDuCa1LI/small.jpg) iar
========



## 简介
Http Liar 项目是基于HTTP Proxy工作模式的一款HTTP协议拦截、篡改工具，可以基于这个工具进行针对网页的实时修改。代理性能并不是这个工具的强项，相反Http Liar对HTML、CSS、JavaScript、JSON等文本块的识别和加载以及对HTTP的解压缩行为会让网页的性能有所降低。不建议使用在性能要求非常高的场景。



## 特性

- HTTP请求/应答头篡改
- HTTP应答数据解析和篡改
 - 解压数据（gzip/inflate）
 - 二进制数据支持
 - 文本数据支持
- 数据篡改
 - 字符编码识别
 - HTML DOM操作
 - CSS/JavaScript/JSON 文本替换


## 数据流转
HttpLiar是基于HttpProxy的工作模式，所以数据流转中也承担着承上启下的环节。
![Logo](http://pic.yupoo.com/oldmanpushcart/CDv86GpI/medish.jpg)
