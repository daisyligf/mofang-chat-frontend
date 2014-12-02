package com.mofang.chat.frontend.controller.websocket;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.frontend.controller.AbstractActionExecutor;
import com.mofang.chat.frontend.logic.UserLogic;
import com.mofang.chat.frontend.logic.impl.UserLogicImpl;
import com.mofang.framework.web.server.annotation.Action;
import com.mofang.framework.web.server.reactor.context.RequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
@Action(url="atom")
public class AtomicAction extends AbstractActionExecutor
{
	private UserLogic logic = UserLogicImpl.getInstance();
	
	@Override
	public ResultValue exec(RequestContext context) throws Exception
	{
		WebSocketRequestContext ctx = (WebSocketRequestContext)context;
		return logic.atom(ctx);
	}
}