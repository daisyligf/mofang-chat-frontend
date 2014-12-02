package com.mofang.chat.frontend.logic;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.framework.web.server.reactor.context.HttpRequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
public interface MessageLogic
{
	/**
	 * 客户端发送聊天消息
	 * @param json
	 * @return
	 */
	public ResultValue sendMessage(WebSocketRequestContext context);
	
	/**
	 * 推送新消息通知
	 * @return
	 */
	public void pushNotify(HttpRequestContext context);
	
	/**
	 * 推送新消息通知
	 * @param message
	 */
	public void pushNotify(String message);
	
	/**
	 * 拉取消息通知
	 * @param json
	 * @return
	 */
	public ResultValue pullNotify(WebSocketRequestContext context);
	
	/**
	 * 获取消息列表
	 * @param json
	 * @return
	 */
	public ResultValue pullMessage(WebSocketRequestContext context);
}