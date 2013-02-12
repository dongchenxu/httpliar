package com.googlecode.httpliar.handler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.googlecode.httpliar.HttpLiarExchange;
import com.googlecode.httpliar.handler.block.DataBlock;
import com.googlecode.httpliar.handler.block.HtmlBlock;
import com.googlecode.httpliar.util.HttpUtils;

/**
 * 谎言标记<br/>
 * 如果处理的是HTML，则给html标签添加一个attribute的最后一行添加一个注释<br/>
 * httpliar='true'
 * @author luanjia@taobao.com
 *
 */
public class HtmlLiarMarkHttpResponseHandler implements HttpResponseHandler {

	@Override
	public boolean isHandleResponse(HttpLiarExchange exchange) {
		// 只标记html
		return exchange.getConfiger().isMimeHtml(HttpUtils.getMIME(exchange.getResponseFields()));
	}

	@Override
	public ResponseHandlerResult handleResponse(HttpLiarExchange exchange,
			DataBlock block) throws Exception {
		if( !(block instanceof HtmlBlock) ) {
			return new ResponseHandlerResult(block);
		}
		
		// 标记liar
		final HtmlBlock htmlBlock = (HtmlBlock)block;
		final Document doc = htmlBlock.getDocument();
		final Element htmlElement = doc.select("html").first();
		if( null != htmlElement ) {
			htmlElement.attr("httpliar", "true");
		}
		
		return new ResponseHandlerResult(new HtmlBlock(doc.html(), htmlBlock.getCharset()));
	}

}
