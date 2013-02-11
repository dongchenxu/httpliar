package com.googlecode.httpliar.handler;

import java.nio.charset.Charset;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.httpliar.HttpLiarExchange;
import com.googlecode.httpliar.handler.block.CssBlock;
import com.googlecode.httpliar.handler.block.DataBlock;
import com.googlecode.httpliar.handler.block.HtmlBlock;
import com.googlecode.httpliar.handler.block.JsonBlock;
import com.googlecode.httpliar.handler.block.TextBlock;
import com.googlecode.httpliar.handler.block.XmlBlock;
import com.googlecode.httpliar.mime.MIME;
import com.googlecode.httpliar.util.HttpUtils;

/**
 * 文本应答处理器
 * @author luanjia@taobao.com
 *
 */
public class TextHttpResponseHandler implements HttpResponseHandler {

	private static final Logger logger = LoggerFactory.getLogger("httpliar");
	
	@Override
	public boolean isHandleResponse(HttpLiarExchange exchange) {

//		// 需要对其内容进行解压
//		if(StringUtils.isNotEmpty(exchange.getResponseFields().getStringField("Content-Encoding"))){
//			return false;
//		};
		
		// 如果当前的mime不在约定mimes指定范围内，则不能当成Text处理
		final String mime = HttpUtils.getMIME(exchange.getResponseFields());
		if( !MIME.isText(mime) ) {
			return false;
		}
		
		return true;
	}

	@Override
	public ResponseHandlerResult handleResponse(HttpLiarExchange exchange,
			DataBlock block) throws Exception {
		
		final Charset charset = HttpUtils.getCharset(exchange.getResponseFields(), HttpUtils.DEFAULT_CHARSET);
		final TextBlock textBlock = new TextBlock(block.getDatas(), charset);
		final String mime = HttpUtils.getMIME(exchange.getResponseFields());
		
		final TextBlock returnBlock;
		if( MIME.isHtml(mime) ) {
			returnBlock = convertToHtmlBlock(textBlock, block);
		} else if( MIME.isCss(mime) ) {
			returnBlock = convertToCssBlock(textBlock);
		} else if( MIME.isJson(mime) ) {
			returnBlock = convertToJsonBlock(textBlock);
		} else if( MIME.isXml(mime) ) {
			returnBlock = convertToXmlBlock(textBlock);
		} else {
			returnBlock = textBlock;
		}
		logger.debug("\n[DEBUG] : debug for \"{}\"\n  URL : {}\n  MSG : block cover to={}\n", new Object[]{
			"text block convert.",
			exchange.getRequestURL(),
			returnBlock.getClass().getSimpleName()
		});
		final ResponseHandlerResult result = new ResponseHandlerResult(returnBlock);
		return result;
	}
	
	private HtmlBlock convertToHtmlBlock(final TextBlock textBlock, final DataBlock block) {
		final HtmlBlock html = new HtmlBlock(textBlock.getText(), textBlock.getCharset());
		final Document doc = html.getDocument();
		
		Charset htmlCharset = textBlock.getCharset();
		
		/*
		 * 解析 <meta http-equiv="content-type" content="text/html; charset=gbk"/>
		 */
		final Element metaCtEle = doc.select("meta[HTTP-EQUIV=content-type]").first();
		if( null != metaCtEle ) {
			htmlCharset = HttpUtils.getCharset(metaCtEle.attr("content"), htmlCharset);
		}
		
		/*
		 * 解析 <meta charset="gbk"/>
		 */
		final Element metaChEle = doc.select("meta[charset]").first();
		if( null != metaChEle ) {
			try {
				htmlCharset = Charset.forName(metaChEle.attr("charset"));
			} catch(Exception e) {
				// ingore
			}
		}
		
		/*
		 * 如果Response的Header中的字符编码和HTML-META所指定的不一样
		 * 则用HTML-META中的字符集重编码
		 */
		return textBlock.getCharset().equals(htmlCharset)
				// 返回原有的文本块
				? html
				// HTML重编码
				: new HtmlBlock(new String(block.getDatas(), htmlCharset), htmlCharset);
	}
	
	private CssBlock convertToCssBlock(final TextBlock textBlock) {
		return new CssBlock(textBlock.getText(), textBlock.getCharset());
	}
	
	private XmlBlock convertToXmlBlock(final TextBlock textBlock) {
		return new XmlBlock(textBlock.getText(), textBlock.getCharset());
	}
	
	private JsonBlock convertToJsonBlock(final TextBlock textBlock) {
		return new JsonBlock(textBlock.getText(), textBlock.getCharset());
	}
	
}
