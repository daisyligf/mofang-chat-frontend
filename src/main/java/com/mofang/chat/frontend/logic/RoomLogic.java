package com.mofang.chat.frontend.logic;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
public interface RoomLogic
{
	/**
	 * 进入聊天室
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public ResultValue enter(WebSocketRequestContext context);
	
	/**
	 * 退出聊天室
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public ResultValue quit(WebSocketRequestContext context);
	
	/**
	 * 获取订阅房间用户列表
	 * @param context
	 * @return
	 */
	public ResultValue getSubscribeUsers(WebSocketRequestContext context);
}