package com.mofang.chat.frontend.controller.websocket;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.frontend.controller.AbstractActionExecutor;
import com.mofang.chat.frontend.logic.RoomLogic;
import com.mofang.chat.frontend.logic.impl.RoomLogicImpl;
import com.mofang.framework.web.server.annotation.Action;
import com.mofang.framework.web.server.reactor.context.RequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
@Action(url="quit_room")
public class QuitRoomAction extends AbstractActionExecutor
{
	private RoomLogic logic = RoomLogicImpl.getInstance();
	
	@Override
	public ResultValue exec(RequestContext context) throws Exception
	{
		WebSocketRequestContext ctx = (WebSocketRequestContext)context;
		return logic.quit(ctx);
	}
}