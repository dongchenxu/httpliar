package com.googlecode.httpliar.handler.block;

import java.nio.charset.Charset;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * HTML块
 * @author luanjia@taobao.com
 *
 */
public class HtmlBlock extends TextBlock {

	/*
	 * html文档
	 */
	private final Document document;
	
	public HtmlBlock(String text, Charset charset) {
		super(text, charset);
		this.document = Jsoup.parse(text);
	}

	/**
	 * 获取当前html文档
	 * @return
	 */
	public Document getDocument() {
		return document.clone();
	}

}
