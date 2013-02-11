package com.googlecode.httpliar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.httpliar.handler.HttpRequestHandler;
import com.googlecode.httpliar.handler.HttpResponseHandler;

/**
 * HttpFilter的代理服务Servlet
 * 
 * @author luanjia@taobao.com
 * 
 */
public class HttpLiarProxyServlet implements Servlet {
	
	private static final Logger logger = LoggerFactory.getLogger("httpliar");
	private HttpClient _client;
	private final List<HttpRequestHandler> _httpRequestHandlers = new ArrayList<HttpRequestHandler>();
	private final List<HttpResponseHandler> _httpResponseHandlers = new ArrayList<HttpResponseHandler>();
	private CachedExchangeFactory _httpExchangeFactory;

	private ServletConfig _config;
	private ServletContext _context;

	/* ------------------------------------------------------------ */
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		_config = config;
		_context = config.getServletContext();
		_httpExchangeFactory = new DefaultCachedExchangeFactory(
				_httpRequestHandlers, _httpResponseHandlers);

		try {

			_client = createHttpClient(config);

			if (_context != null) {
				_context.setAttribute(config.getServletName() + ".ThreadPool",
						_client.getThreadPool());
				_context.setAttribute(config.getServletName() + ".HttpClient",
						_client);
			}

		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public void destroy() {
		try {
			_client.stop();
		} catch (Exception x) {
			logger.debug("\n[DEBUG] : debug for \"{}\"\n  MSG : {}\n", new Object[]{
				"destroy httpliar proxy servlet occre exception.",
				x
			});
		}
	}

	/**
	 * Create and return an HttpClient based on ServletConfig
	 * 
	 * By default this implementation will create an instance of the HttpClient
	 * for use by this proxy servlet.
	 * 
	 * @param config
	 * @return HttpClient
	 * @throws Exception
	 */
	private HttpClient createHttpClient(ServletConfig config)
			throws Exception {
		final HttpClient client = new HttpClient();
		
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

		String t = "";

		if ((t=config.getInitParameter("maxThreads")) != null) {
			client.setThreadPool(new QueuedThreadPool(Integer.parseInt(t)));
		} else {
			client.setThreadPool(new QueuedThreadPool());
		}
		((QueuedThreadPool) client.getThreadPool()).setName(config.getServletName());

		if ((t=config.getInitParameter("maxConnections")) != null) {
			client.setMaxConnectionsPerAddress(Integer.parseInt(t));
		}

		if ((t = config.getInitParameter("timeout")) != null) {
			client.setTimeout(Long.parseLong(t));
		}

		if ((t = config.getInitParameter("idleTimeout")) != null) {
			client.setIdleTimeout(Long.parseLong(t));
		}

		if ((t = config.getInitParameter("requestHeaderSize")) != null) {
			client.setRequestHeaderSize(Integer.parseInt(t));
		}

		if ((t = config.getInitParameter("requestBufferSize")) != null) {
			client.setRequestBufferSize(Integer.parseInt(t));
		}

		if ((t = config.getInitParameter("responseHeaderSize")) != null) {
			client.setResponseHeaderSize(Integer.parseInt(t));
		}

		if ((t = config.getInitParameter("responseBufferSize")) != null) {
			client.setResponseBufferSize(Integer.parseInt(t));
		}

		client.start();

		return client;
	}


	/* ------------------------------------------------------------ */
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#getServletConfig()
	 */
	public ServletConfig getServletConfig() {
		return _config;
	}

	/* ------------------------------------------------------------ */
	/* browser -> (bpRequest)  -> proxy -> (psRequest)  -> server
	 * browser <- (bpResponse) <- proxy <- (psResponse) <- server
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse)
	 */
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {

		final HttpServletRequest bpRequest = (HttpServletRequest) req;
		final HttpServletResponse bpResponse = (HttpServletResponse) res;
		
		if ("CONNECT".equalsIgnoreCase(bpRequest.getMethod())) {
			handleConnect(bpRequest, bpResponse);
			return;
		}

		final Continuation continuation = ContinuationSupport.getContinuation(bpRequest);
		if (!continuation.isInitial()) {
			bpResponse.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT);
			return;
		}
		
		final HttpURI httpUri = proxyHttpURI(bpRequest);

		if (httpUri == null) {
			bpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
//		try {
//			URI.create(httpUri.toString());
//		}catch(IllegalArgumentException e) {
//			// fix by dukun@taobao.com
//			bpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
//			return;
//		}

		final HttpExchange exchange = _httpExchangeFactory.newInstance(httpUri, bpRequest, bpResponse);

		setupContinuationTimeout(continuation, exchange);
		
		continuation.suspend(bpResponse);
		_client.send(exchange);
		
	}
	
	/**
	 * 设置通讯超时时间
	 * @param continuation
	 * @param exchange
	 */
	private void setupContinuationTimeout(final Continuation continuation, final HttpExchange exchange) {
		/*
		 * we need to set the timeout on the continuation to take into
		 * account the timeout of the HttpClient and the HttpExchange
		 */
		long ctimeout = (_client.getTimeout() > exchange.getTimeout()) 
				? _client.getTimeout() 
				: exchange.getTimeout();

		// continuation fudge factor of 1000, underlying components
		// should fail/expire first from exchange
		if (ctimeout == 0) {
			continuation.setTimeout(0); // ideally never times out
		} else {
			continuation.setTimeout(ctimeout + 1000);
		}
	}

	/* ------------------------------------------------------------ */
	private void handleConnect(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String uri = request.getRequestURI();

		String port = "";
		String host = "";

		int c = uri.indexOf(':');
		if (c >= 0) {
			port = uri.substring(c + 1);
			host = uri.substring(0, c);
			if (host.indexOf('/') > 0)
				host = host.substring(host.indexOf('/') + 1);
		}

		// TODO - make this async!

		InetSocketAddress inetAddress = new InetSocketAddress(host,
				Integer.parseInt(port));

		// if
		// (isForbidden(HttpMessage.__SSL_SCHEME,addrPort.getHost(),addrPort.getPort(),false))
		// {
		// sendForbid(request,response,uri);
		// }
		// else
		{
			InputStream in = request.getInputStream();
			OutputStream out = response.getOutputStream();

			Socket socket = new Socket(inetAddress.getAddress(),
					inetAddress.getPort());

			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Connection", "close");
			response.flushBuffer();
			// TODO prevent real close!

			IO.copyThread(socket.getInputStream(), out);
			IO.copy(in, socket.getOutputStream());
		}
	}

	/* ------------------------------------------------------------ */
	private HttpURI proxyHttpURI(HttpServletRequest request) throws MalformedURLException {
		String uri = request.getRequestURI();
		if (request.getQueryString() != null) {
			uri += "?" + request.getQueryString();
		}
		return new HttpURI(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + uri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#getServletInfo()
	 */
	public String getServletInfo() {
		return "HttpFilter Proxy Servlet";
	}

	/**
	 * 获取所有的HttpRequestHandler集合
	 * @return
	 */
	public List<HttpRequestHandler> getHttpRequestHandlers() {
		return _httpRequestHandlers;
	}
	
	/**
	 * 获取所有的HttpResponseHandler集合
	 * @return
	 */
	public List<HttpResponseHandler> getHttpResponseHandlers() {
		return _httpResponseHandlers;
	}
	
}
