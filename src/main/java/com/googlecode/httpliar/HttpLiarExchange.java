package com.googlecode.httpliar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.CachedExchange;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.httpliar.handler.HttpRequestHandler;
import com.googlecode.httpliar.handler.HttpResponseHandler;
import com.googlecode.httpliar.handler.RequestHandlerResult;
import com.googlecode.httpliar.handler.ResponseHandlerResult;
import com.googlecode.httpliar.handler.block.DataBlock;
import com.googlecode.httpliar.util.HttpUtils;

/**
 * HttpFilter的Exchange
 * @author luanjia@taobao.com
 *
 */
public class HttpLiarExchange extends CachedExchange {

	
	private static final Logger logger = LoggerFactory.getLogger("httpliar");
	
	private final Configer _configer;
	private final List<HttpRequestHandler> _httpRequestHandlers;
	private final List<HttpResponseHandler> _httpResponseHandlers;
	private final HashSet<String> _dontProxyToBrowserHeaders;
	private final HttpServletRequest _bpRequest;
	private final HttpServletResponse _bpResponse;
	private final Continuation _bpRequestContinuation;
	private final OutputStream _bpOut;
	private final HttpURI _httpURI;
	
	private boolean _waitForCompleted = false;	//是否等待所有chunk完成
	private Buffer _waitForCompletedBuffer;		//数据缓存对象

	public HttpLiarExchange(
			final Configer configer,
			final List<HttpRequestHandler> _httpRequestHandlers,
			final List<HttpResponseHandler> _httpResponseHandlers,
			final HashSet<String> _DontProxyToBrowserHeaders,
			final HttpServletRequest bpRequest,
			final HttpServletResponse bpResponse,
			final HttpURI httpURI) throws IOException {
		super(true);
		this._configer = configer;
		this._httpRequestHandlers = _httpRequestHandlers;
		this._httpResponseHandlers = _httpResponseHandlers;
		this._dontProxyToBrowserHeaders = _DontProxyToBrowserHeaders;
		this._bpRequest = bpRequest;
		this._bpResponse = bpResponse;
		this._bpRequestContinuation = ContinuationSupport.getContinuation(bpRequest);
		this._bpOut = bpResponse.getOutputStream();
		this._httpURI = httpURI;
	}
	
	@Override
	protected void onRequestComplete() throws IOException {
		super.onRequestComplete();
		try {
			for( HttpRequestHandler reqHandler : _httpRequestHandlers ) {
				if( reqHandler.isHandleRequest(this) ) {
					final RequestHandlerResult result = reqHandler.handleRequest(this);
					processProxyToServerRequestHandlerResult(result);
				}
			}
		} catch (Exception e) {
			logger.warn("\n[WARN] : warn for \"{}\"\n  URL : {}\n", new Object[]{
				"handle response handler occre exception.",
				getRequestURL(),
				e
			});
		}
		
	}

	@Override
	protected void onResponseHeader(Buffer name, Buffer value)
			throws IOException {
			
		final String nameStr = name.toString();
		final String valueStr = value.toString();
		
		if( StringUtils.isBlank(nameStr)
				|| StringUtils.isBlank(valueStr)) {
			return;
		}
		
		for( String dontProxyHeader : _dontProxyToBrowserHeaders ) {
			if( StringUtils.equalsIgnoreCase(nameStr, dontProxyHeader) ) {
				return;
			}
		}
		
		_bpResponse.addHeader(nameStr, valueStr);
		super.onResponseHeader(name, value);
		
	}
	
	@Override
	protected void onResponseHeaderComplete() throws IOException {
		super.onResponseHeaderComplete();
		
		final String mime = HttpUtils.getMIME(getResponseFields());
		logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n  MSG : mime={}\n", new Object[]{
			"snoop the response mime type.",
			getRequestURL(),
			mime
		});
		_waitForCompleted = _configer.isMimeSupport(mime);
		_waitForCompletedBuffer = createBuffer(_waitForCompleted);
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResponseStatus(Buffer version, int status,
			Buffer reason) throws IOException {
		logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n  MSG : status={};reason={}\n", new Object[]{
			"response status changed.",
			getRequestURL(),
			status,
			reason
		});
	
		if (reason != null && reason.length() > 0) {
			_bpResponse.setStatus(status, reason.toString());
		} else {
			_bpResponse.setStatus(status);
		}
		
		super.onResponseStatus(version, status, reason);
	}

	@Override
	protected void onResponseContent(Buffer content)
			throws IOException {
		logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n  MSG : content.length={}\n", new Object[]{
			"snoop the response content length.",
			getRequestURL(),
			content.length()
		});
		if( _waitForCompleted ) {
			_waitForCompletedBuffer.put(content);
		} else {
			content.writeTo(_bpOut);
		}
	}

	@Override
	protected void onResponseComplete() throws IOException {
		logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n  MSG : {}\n", new Object[]{
			"snoop the response content complete length.",
			getRequestURL(),
			"all response content receive completed."
		});
		try {
			if( _waitForCompleted ) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				_waitForCompletedBuffer.writeTo(baos);
				DataBlock block = new DataBlock(baos.toByteArray());
				IO.close(baos);//这里不放入到finally，赌以上步骤在正常情况下不会抛出异常
				try {
					for( HttpResponseHandler respHandler : _httpResponseHandlers ) {
						if( respHandler.isHandleResponse(this) ) {
							final ResponseHandlerResult result = respHandler.handleResponse(this, block);
							processServerToProxyResponseHandlerResult(result);
							block = result.getBlock();
						}
					}
				} catch (Exception e) {
					logger.warn("\n[WARN] : warn for \"{}\"\n  URL : {}\n", new Object[]{
						"response handler occre exception.",
						getRequestURL(),
						e
					});
				}
				try {
					_bpOut.write(block.getDatas());
				}catch(EofException e) {
					// 出这种错，只可能是浏览器关闭了链接所导致
					if( !logger.isDebugEnabled() ) {
						logger.warn("\n[WARN] : warn for \"{}\"\n  URL : {}\n", new Object[]{
							"bTp write data occre Eof exception.",
							getRequestURL()
						});
					}
					logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n", new Object[]{
						"bTp write data occre Eof exception.",
						getRequestURL(),
						e
					});
				}
			}
		} finally {
			_bpRequestContinuation.complete();
			if( null != _waitForCompletedBuffer
					&& _waitForCompletedBuffer instanceof FixedRandomAccessFileBuffer ) {
				((FixedRandomAccessFileBuffer)_waitForCompletedBuffer).close();
			}
		}
	}

	@Override
	protected void onConnectionFailed(Throwable ex) {
		handleOnConnectionFailed(ex, _bpRequest, _bpResponse);

		// it is possible this might trigger before the
		// continuation.suspend()
		if (!_bpRequestContinuation.isInitial()) {
			_bpRequestContinuation.complete();
		}
	}

	@Override
	protected void onException(Throwable ex) {
		handleOnException(ex, _bpRequest, _bpResponse);

		// it is possible this might trigger before the
		// continuation.suspend()
		if (!_bpRequestContinuation.isInitial()) {
			_bpRequestContinuation.complete();
		}
	}

	@Override
	protected void onExpire() {
		handleOnExpire(_bpRequest, _bpResponse);
		_bpRequestContinuation.complete();
	}
	
	/**
	 * 处理server ======+ proxy的返回结果
	 * @param result
	 */
	private void processServerToProxyResponseHandlerResult(ResponseHandlerResult result) {
		for( Entry<String, String> e : result.getHeaderModifier().entrySet() ) {
			_bpResponse.setHeader(e.getKey(), e.getValue());
		}
		for( String key : result.getHeaderRemover() ) {
			_bpResponse.setHeader(key, null);
		}
	}

	/**
	 * 处理proxy ======+ server的返回结果
	 * @param result
	 */
	private void processProxyToServerRequestHandlerResult(RequestHandlerResult result) {
		for( Entry<String, String> e : result.getHeaderModifier().entrySet() ) {
			setRequestHeader(e.getKey(), e.getValue());
		}
		for( String key : result.getHeaderRemover() ) {
			setRequestHeader(key, null);
		}
	}

	/**
	 * 创建buffer
	 * @param waitForCompleted
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private FixedRandomAccessFileBuffer createBuffer(final boolean waitForCompleted) throws FileNotFoundException,
			IOException {
		if( !waitForCompleted ) {
			return null;
		}
		return new FixedRandomAccessFileBuffer(File.createTempFile("__HTTPFILTER_", "temp_"+System.currentTimeMillis()));
	}

	/**
	 * Extension point for custom handling of an HttpExchange's
	 * onConnectionFailed method. The default implementation delegates to
	 * {@link #handleOnException(Throwable, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
	 * 
	 * @param ex
	 * @param request
	 * @param response
	 */
	private void handleOnConnectionFailed(Throwable ex,
			HttpServletRequest request, HttpServletResponse response) {
		handleOnException(ex, request, response);
	}

	/**
	 * Extension point for custom handling of an HttpExchange's onException
	 * method. The default implementation sets the response status to
	 * HttpServletResponse.SC_INTERNAL_SERVER_ERROR (503)
	 * 
	 * @param ex
	 * @param request
	 * @param response
	 */
	private void handleOnException(Throwable ex, HttpServletRequest request,
			HttpServletResponse response) {
		if(ex instanceof EofException) {
			logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n", new Object[]{
				"http exchange occer Eof error.",
				getRequestURL(),
				ex
			});
		} else if (ex instanceof IOException) {
			if( !logger.isDebugEnabled() ) {
				logger.warn("\n[WARN] : warn for \"{}\"\n  URL : {}\n", new Object[]{
					"http exchange occre I/O exception.",
					getRequestURL()
				});
			}
			logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n", new Object[]{
				"http exchange occre I/O exception.",
				getRequestURL(),
				ex
			});
		} else {
			logger.warn("\n[WARN] : warn for \"{}\"\n  URL : {}\n", new Object[]{
				"http exchange occre exception.",
				getRequestURL(),
				ex
			});
		}
			
		if (!response.isCommitted()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Extension point for custom handling of an HttpExchange's onExpire method.
	 * The default implementation sets the response status to
	 * HttpServletResponse.SC_GATEWAY_TIMEOUT (504)
	 * 
	 * @param request
	 * @param response
	 */
	private void handleOnExpire(HttpServletRequest request,
			HttpServletResponse response) {
		if (!response.isCommitted()) {
			response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
		}
	}
	
	/* ------------------------------------------------------------ */
	/**
	 * @param uri
	 *            an absolute URI (for example 'http://localhost/foo/bar?a=1')
	 * @deprecated
	 * 	please use {@link #setURL(URL)}
	 */
	public void setURI(URI uri) {
		throw new UnsupportedOperationException("setURI can't use in HttpFilterExchange. please use setURL();");
	}
	
	/**
	 * @deprecated
	 * 	please use {@link #setURL(URL)}
	 */
	public void setURL(String urlString) {
		throw new UnsupportedOperationException("setURI can't use in HttpFilterExchange. please use setURL();");
	}
	
	public void setURL(URL url) {
		final String scheme = url.getProtocol();
		int port = url.getPort();
		if (port <= 0) {
			port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
		}
		setScheme(scheme);
		setAddress(new Address(url.getHost(), port));
		
		HttpURI httpUri = new HttpURI(url.toString());
		String completePath = httpUri.getCompletePath();
		setRequestURI(completePath == null ? "/" : completePath);
//		setRequestURI(StringUtils.isBlank(url.getPath()) ? "/" : url.getPath());
	}
	
	/**
	 * 获取请求URL字符信息
	 * @return
	 */
	public String getRequestURL() {
		return _httpURI.toString();
	}
	
	/**
	 * 获取请求URL字符信息
	 * @return
	 */
	public HttpURI getHttpURI() {
		return _httpURI;
	}
	
	/**
	 * 获取配置文件
	 * @return
	 */
	public Configer getConfiger() {
		return _configer;
	}

}
