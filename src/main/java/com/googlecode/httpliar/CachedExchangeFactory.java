package com.googlecode.httpliar;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.CachedExchange;
import org.eclipse.jetty.http.HttpURI;

/**
 * HttpExchange工厂
 * @author luanjia@taobao.com
 *
 */
public interface CachedExchangeFactory {

	/**
	 * 创建HttpExchange新实例
	 * @param httpUri
	 * @param request
	 * @param response
	 * @param waitForCompleted
	 * @return
	 * @throws IOException
	 */
	CachedExchange newInstance(
			final HttpURI httpUri,
			final HttpServletRequest request, 
			final HttpServletResponse response) throws IOException;
	
}
