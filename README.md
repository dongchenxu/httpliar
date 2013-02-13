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

## 内部原理
HTTP的应答会在压缩之后拆分成多个Chunked的方式进行传递，作为一个Proxy如果不需要关注内部数据只需要将流经的数据包最快的传走即可。但对于HttpLiar而言我们需要了解和修改应答的数据，所以HttpLiar需要对流经的数据像浏览器进行整合处理。这也是HttpLiar为什么性能提升不上来的原因。

![Logo](http://pic.yupoo.com/oldmanpushcart/CDvCpwiP/medium.jpg)

对于应答所返回的数据，HttpLiar抽象成了DataBlock（二进制块）。
![Logo](http://pic.yupoo.com/oldmanpushcart/CDvFY9po/medish.jpg)

## 独立运行

作为独立的HttpProxy进行部署
```
./java -jar httpliar-jar-with-dependencies.jar
```

## 内嵌运行
作为内嵌应用，HttpLiar已经上传到Maven中心仓库
在pom.xml中添加
```
<dependency>
  <groupId>com.googlecode.httpliar</groupId>
  <artifactId>httpliar</artifactId>
  <version>1.0.0</version>
</dependency>
```
编写Java代码
```
public static void main(String[] args) throws Exception {
	
	final Configer configer = getConfiger(args);
	
	
	// 在1.0.0的版本中有一个bug，在Response的Header中忘记去掉了Content-Length
	// 可能会导致部分浏览器进行长度校验失败，从而导致网页无法正常显示
	// 所以在这里需要主动添加一个ResponseHandler，对Content-Length主动进行删除
	// 在1.0.0（不含1.0.0）之后的版本中会修复这个问题
	final List<HttpResponseHandler> handlers = new ArrayList<HttpResponseHandler>();
	handlers.add(new HttpResponseHandler(){

		@Override
		public boolean isHandleResponse(HttpLiarExchange exchange) {
			return true;
		}

		@Override
		public ResponseHandlerResult handleResponse(
				HttpLiarExchange exchange, DataBlock block)
				throws Exception {
			final ResponseHandlerResult result = new ResponseHandlerResult(block);
			result.removeHeader("Content-Length");
			return result;
		}
		
	});
	
	@SuppressWarnings("unchecked")
	final HttpLiarServer server = new HttpLiarServer(configer, ListUtils.EMPTY_LIST, handlers);
	
	JvmUtils.registShutdownHook("httpliar-shutdown", new ShutdownHook(){

		@Override
		public void shutdown() throws Throwable {
			server.stopProxy();
		}
		
	});
	
	server.startProxy();
	
}
```


