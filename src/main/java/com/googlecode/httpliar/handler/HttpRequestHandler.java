package com.googlecode.httpliar.handler;

import com.googlecode.httpliar.HttpLiarExchange;

/**
 * 请求处理器
 * @author luanjia@taobao.com
 *
 */
public interface HttpRequestHandler {

	/**
	 * 判断是否能处理HTTP应答
	 * @param exchange
	 * @return
	 */
	boolean isHandleRequest(final HttpLiarExchange exchange);
	
	/**
	 * 处理来自浏览器的请求
	 * @param exchange
	 * @return
	 * @throws Exception
	 */
	RequestHandlerResult handleRequest(final HttpLiarExchange exchange) throws Exception;
	
}
