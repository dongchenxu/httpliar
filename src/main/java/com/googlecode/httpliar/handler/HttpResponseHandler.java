package com.googlecode.httpliar.handler;

import com.googlecode.httpliar.HttpLiarExchange;
import com.googlecode.httpliar.handler.block.DataBlock;

/**
 * HTTP应答处理
 * @author luanjia@taobao.com
 *
 */
public interface HttpResponseHandler {

	/**
	 * 判断是否能处理HTTP应答
	 * @param exchange
	 * @return
	 */
	boolean isHandleResponse(final HttpLiarExchange exchange);
	
	/**
	 * 处理HTTP应答
	 * @param exchange
	 * @param block
	 * @return
	 * @throws Exception
	 */
	ResponseHandlerResult handleResponse(
			final HttpLiarExchange exchange,
			final DataBlock block) throws Exception;
	
}
