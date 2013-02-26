package com.googlecode.httpliar.handler.block;

import java.nio.charset.Charset;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

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
		this.document.outputSettings().escapeMode(EscapeMode.extended);
	}

	/**
	 * 获取当前html文档
	 * @return
	 */
	public Document getDocument() {
		return document.clone();
	}

}
