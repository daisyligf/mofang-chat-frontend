package com.mofang.chat.frontend.logic;

import io.netty.channel.ChannelHandlerContext;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.framework.web.server.reactor.context.HttpRequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
public interface UserLogic
{
	public ResultValue atom(WebSocketRequestContext context);
	
	public void leave(ChannelHandlerContext context);
	
	public void prohibit(HttpRequestContext context);
	
	public void logout(HttpRequestContext context);
}