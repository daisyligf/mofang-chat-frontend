package com.mofang.chat.frontend.controller.websocket;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.frontend.controller.AbstractActionExecutor;
import com.mofang.chat.frontend.logic.MessageLogic;
import com.mofang.chat.frontend.logic.impl.MessageLogicImpl;
import com.mofang.framework.web.server.annotation.Action;
import com.mofang.framework.web.server.reactor.context.RequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
@Action(url="send_msg")
public class SendMessageAction extends AbstractActionExecutor
{
	private MessageLogic logic = MessageLogicImpl.getInstance();
	
	@Override
	public ResultValue exec(RequestContext context) throws Exception
	{
		WebSocketRequestContext ctx = (WebSocketRequestContext)context;
		return logic.sendMessage(ctx);
	}
}