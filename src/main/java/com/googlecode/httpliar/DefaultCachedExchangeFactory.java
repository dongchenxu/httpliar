package com.googlecode.httpliar;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.CachedExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.HttpURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.httpliar.handler.HttpRequestHandler;
import com.googlecode.httpliar.handler.HttpResponseHandler;

/**
 * HttpExchangeFactory默认实现
 * @author luanjia@taobao.com
 *
 */
public class DefaultCachedExchangeFactory implements CachedExchangeFactory {

	private static final Logger logger = LoggerFactory.getLogger("httpliar");
	
	private final List<HttpRequestHandler> _httpRequestHandlers;
	private final List<HttpResponseHandler> _httpResponseHandlers;
	
	/*
	 * 代理服务器到服务器所禁止的请求头
	 */
	private final HashSet<String> _DontProxyToServerHeaders = new HashSet<String>();
	{
//		_DontProxyToServerHeaders.add("If-Modified-Since");
//		_DontProxyToServerHeaders.add("If-None-Match");
//		_DontProxyToServerHeaders.add("Cache-Control");
	}
	
	/*
	 * 代理服务器到浏览器所禁止的应答头
	 */
	private final HashSet<String> _DontProxyToBrowserHeaders = new HashSet<String>();
	{
		_DontProxyToBrowserHeaders.add("Proxy-Connection");
		_DontProxyToBrowserHeaders.add("Connection");
		_DontProxyToBrowserHeaders.add("Keep-Alive");
		_DontProxyToBrowserHeaders.add("Transfer-Encoding");
		_DontProxyToBrowserHeaders.add("TE");
		_DontProxyToBrowserHeaders.add("Trailer");
		_DontProxyToBrowserHeaders.add("Proxy-Authorization");
		_DontProxyToBrowserHeaders.add("Proxy-Authenticate");
		_DontProxyToBrowserHeaders.add("Upgrade");
//		_DontProxyToBrowserHeaders.add("Expires");
//		_DontProxyToBrowserHeaders.add("Cache-Control");
//		_DontProxyToBrowserHeaders.add("ETag");
//		_DontProxyToBrowserHeaders.add("Content-Length");
	}
	
	public DefaultCachedExchangeFactory(
			final List<HttpRequestHandler> _httpRequestHandlers,
			final List<HttpResponseHandler> _httpResponseHandlers) {
		this._httpRequestHandlers = _httpRequestHandlers;
		this._httpResponseHandlers = _httpResponseHandlers;
	}
	
	@Override
	public CachedExchange newInstance(
			final HttpURI httpUri,
			final HttpServletRequest bpRequest,
			final HttpServletResponse bpResponse) throws IOException {
		
		final InputStream bpIn = bpRequest.getInputStream();
		final HttpLiarExchange exchange = new HttpLiarExchange(
				_httpRequestHandlers,
				_httpResponseHandlers,
				_DontProxyToBrowserHeaders,
				bpRequest,
				bpResponse);
		
		final String connectionHeader = getConnectionHeader(bpRequest);
		setupHttpExchange(exchange, httpUri, bpRequest);
		setupProxyToServerRequestHeaders(bpIn, exchange, bpRequest, connectionHeader);
		
		return exchange;
	}

	/**
	 * 获取链接头
	 * @param bpRequest
	 * @return
	 */
	private String getConnectionHeader(final HttpServletRequest bpRequest) {
		// check connection header
		String connectionHdr = bpRequest.getHeader("Connection");
		if (connectionHdr != null) {
			connectionHdr = connectionHdr.toLowerCase(Locale.ENGLISH);
			if (connectionHdr.indexOf("keep-alive") < 0
					&& connectionHdr.indexOf("close") < 0)
				connectionHdr = null;
		}
		return connectionHdr;
	}
	
	/**
	 * 设置HttpExchange
	 * @param in
	 * @param exchange
	 * @param httpUri
	 * @param request
	 * @throws MalformedURLException 
	 */
	private void setupHttpExchange(
			final HttpLiarExchange exchange,
			final HttpURI httpUri,
			final HttpServletRequest request) throws MalformedURLException {
		
		
		exchange.setURL(new URL(httpUri.toString()));
//		exchange.setURL(httpUri.toString());
		
		logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n  MSG : {}\n", new Object[]{
			"handle http request.",
			exchange.getRequestURL(),
			"protpcol="+request.getProtocol()
		});
		
		exchange.setScheme(HttpSchemes.HTTPS.equals(request.getScheme()) 
				? HttpSchemes.HTTPS_BUFFER
				: HttpSchemes.HTTP_BUFFER);
		exchange.setMethod(request.getMethod());
		exchange.setVersion(request.getProtocol());

	}

	/**
	 * 设置Proxy To Server的请求头
	 * @param in
	 * @param exchange
	 * @param request
	 * @param connectionHdr
	 */
	private void setupProxyToServerRequestHeaders(final InputStream in,
			final HttpExchange exchange, final HttpServletRequest request,
			String connectionHdr) {
		// copy headers
		boolean xForwardedFor = false;
		boolean hasContent = false;
		long contentLength = -1;
		Enumeration<?> enm = request.getHeaderNames();
		while (enm.hasMoreElements()) {
			// TODO could be better than this!
			String hdr = (String) enm.nextElement();
			String lhdr = hdr.toLowerCase(Locale.ENGLISH);

			if (_DontProxyToServerHeaders.contains(lhdr)) {
				continue;
			}
				
			if (connectionHdr != null
					&& connectionHdr.indexOf(lhdr) >= 0) {
				continue;
			}
				
			if ("content-type".equals(lhdr)) {
				hasContent = true;
			} else if ("content-length".equals(lhdr)) {
				contentLength = request.getContentLength();
				exchange.setRequestHeader(HttpHeaders.CONTENT_LENGTH,
						Long.toString(contentLength));
				if (contentLength > 0) {
					hasContent = true;
				}
			} else if ("x-forwarded-for".equals(lhdr)) {
				xForwardedFor = true;
			}
				

			Enumeration<?> vals = request.getHeaders(hdr);
			while (vals.hasMoreElements()) {
				String val = (String) vals.nextElement();
				if (val != null) {
					exchange.setRequestHeader(hdr, val);
				}
			}
		}

		// Proxy headers
		exchange.setRequestHeader("Via", "1.1 (jetty)");
		if (!xForwardedFor) {
			exchange.addRequestHeader("X-Forwarded-For",
					request.getRemoteAddr());
			exchange.addRequestHeader("X-Forwarded-Proto",
					request.getScheme());
			exchange.addRequestHeader("X-Forwarded-Host",
					request.getHeader("Host"));
			exchange.addRequestHeader("X-Forwarded-Server",
					request.getLocalName());
		}
		
		if (hasContent) {
			exchange.setRequestContentSource(in);
		}
		
		// remove request header from _Dont
		for( String nameInRequest : exchange.getRequestFields().getFieldNamesCollection() ) {
			for( String nameInDont : _DontProxyToServerHeaders ) {
				if( StringUtils.equalsIgnoreCase(nameInRequest, nameInDont) ) {
					exchange.getRequestFields().remove(nameInDont);
				}
			}
		}
		
		
	}
	
}
