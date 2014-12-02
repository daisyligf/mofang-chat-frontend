package com.mofang.chat.frontend.logic.impl;

import org.json.JSONObject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import com.mofang.chat.business.entity.User;
import com.mofang.chat.business.redis.RoomRedis;
import com.mofang.chat.business.redis.UserRedis;
import com.mofang.chat.business.redis.impl.RoomRedisImpl;
import com.mofang.chat.business.redis.impl.UserRedisImpl;
import com.mofang.chat.business.service.UserService;
import com.mofang.chat.business.service.impl.UserServiceImpl;
import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.business.sysconf.ReturnCode;
import com.mofang.chat.business.sysconf.ReturnCodeHelper;
import com.mofang.chat.business.sysconf.common.UserStatus;
import com.mofang.chat.frontend.global.GlobalConfig;
import com.mofang.chat.frontend.global.GlobalObject;
import com.mofang.chat.frontend.logic.UserLogic;
import com.mofang.chat.frontend.util.ContextUtil;
import com.mofang.framework.util.StringUtil;
import com.mofang.framework.web.server.reactor.context.HttpRequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
public class UserLogicImpl implements UserLogic
{
	private final static UserLogicImpl LOGIC = new UserLogicImpl();
	private UserRedis userRedis = UserRedisImpl.getInstance();
	private RoomRedis roomRedis = RoomRedisImpl.getInstance();
	private UserService userService = UserServiceImpl.getInstance();
	
	private UserLogicImpl()
	{}
	
	public static UserLogicImpl getInstance()
	{
		return LOGIC;
	}

	@Override
	public ResultValue atom(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("atom_response");
		String postData = context.getPostData();
		if(StringUtil.isNullOrEmpty(postData))
		{
			result.setCode(ReturnCode.CLIENT_REQUEST_DATA_IS_INVALID);
			return result;
		}
		
		try
		{
			JSONObject json = new JSONObject(postData);
			Long sequenceId = json.optLong("seq_id");
			result.setSequenceId(sequenceId);
			Long userId = json.optLong("uid");
			String deviceToken = json.optString("token", "");
			String platform = json.optString("pf", "android");
			String sessionId = json.optString("sid");
			if(null == userId || 0 == userId || StringUtil.isNullOrEmpty(sessionId))
			{
				result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			User user = userService.getAuth(userId, sessionId);
			if(null == user || (user.getStatus() == UserStatus.PROHIBIT)) 
			{
				result.setCode(ReturnCode.ENTER_ROOM_AUTH_FAIL);
				return result;
			}
			
			Channel channel = context.getChannelContext().channel();
			///添加channel和uid的对应关系
			ContextUtil.setUserAndChannelRelation(userId, channel);
			
			//ContextUtil.setUserIdToChannel(channel, userId);
			///添加uid和channel的对应关系
			//ContextUtil.setChannelToUserId(userId, channel);
			///将frontend的IP加入当前用户对应的frontend关系中
			userRedis.setFrontend(userId, GlobalConfig.SERVER_IP);
			///保存用户token到redis中
			if("iphone".equals(platform))
				userRedis.setToken(userId, deviceToken);
			
			///记录日志
			GlobalObject.INFO_LOG.info("act=atom&uid=" + userId + "&post_data=" + postData);
			GlobalObject.INFO_LOG.info("channel_uid_map size:" + GlobalObject.CHANNEL_UID_MAP.size() + "    uid_channel_map size:" + GlobalObject.UID_CHANNEL_MAP.size());
			
			///返回信息
			result.setCode(ReturnCode.SUCCESS);
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at UserLogicImpl.atom throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}

	@Override
	public void leave(ChannelHandlerContext context)
	{
		Channel channel = context.channel();
		if(null == channel)
		{
			///记录用户进入房间的时长
			String logMsg = "act=close_websocket_by_none_channel";
			GlobalObject.INFO_LOG.info(logMsg);
		}
		///根据channel获取userId
		long userId = ContextUtil.getUserIdByChannel(channel);
		///根据channel获取roomId
		int roomId = ContextUtil.getRoomIdByChannel(channel);
		
		if(userId == 0)
		{
			///记录用户进入房间的时长
			String logMsg = "act=close_websocket_by_none_user&userId=" + userId;
			GlobalObject.INFO_LOG.info(logMsg);
			return;
		}
		try
		{
			///移除channel和uid的对应关系
			ContextUtil.removeUserAndChannelRelation(channel);
			
			///获取进入房间的时间戳
			long timestamp = roomRedis.getEnterTimestamp(roomId, userId);
			long curtimestamp = System.currentTimeMillis();
			
			///将UID从房间对应的用户集合中移除
			if(roomId > 0)
			{
				roomRedis.quitRoom(roomId, userId);
				///移除当前websocket通道对应的roomId
				ContextUtil.removeRoomIdFromChannel(channel);
			}
			///如果是ios用户，则不移除和frontend的对应关系
			String token = userRedis.getToken(userId);
			if(StringUtil.isNullOrEmpty(token))
			{
				///移除uid和frontend的对应关系
				userRedis.removeFrontend(userId);
			}
			
			///记录用户进入房间的时长
			String logMsg = "act=close_websocket&userId=" + userId  +"&roomId=" + roomId + "&enterRoomTimes=" + (curtimestamp - timestamp);
			GlobalObject.INFO_LOG.info(logMsg);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at UserLogicImpl.leave throw an error.", e);
		}
	}

	@Override
	public void prohibit(HttpRequestContext context)
	{
		String postData = context.getPostData();
		if(StringUtil.isNullOrEmpty(postData))
			return;
		
		try
		{
			JSONObject json = new JSONObject(postData);
			Long userId = json.optLong("uid");
			Channel channel = ContextUtil.getChannelByUserId(userId);
			///根据channel获取roomId
			int roomId = ContextUtil.getRoomIdByChannel(channel);
			if(userId == 0)
				return;
			
			///移除channel和uid的对应关系
			ContextUtil.removeUserAndChannelRelation(channel);
			
			///将UID从房间对应的用户集合中移除
			if(roomId > 0)
			{
				roomRedis.quitRoom(roomId, userId);
				///移除当前websocket通道对应的roomId
				ContextUtil.removeRoomIdFromChannel(channel);
			}
			///移除uid和frontend的对应关系
			userRedis.removeFrontend(userId);
			///移除用户的token信息
			userRedis.removeToken(userId);
			///移除用户信息
			userRedis.removeInfo(userId);
			///关闭通道
			if(null != channel && channel.isOpen())
				channel.close();
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at UserLogicImpl.prohibit throw an error.", e);
		}
	}

	
	@Override
	public void logout(HttpRequestContext context)
	{
		String postData = context.getPostData();
		if(StringUtil.isNullOrEmpty(postData))
			return;
		
		try
		{
			JSONObject json = new JSONObject(postData);
			Long userId = json.optLong("uid");
			Channel channel = ContextUtil.getChannelByUserId(userId);
			///根据channel获取roomId
			int roomId = ContextUtil.getRoomIdByChannel(channel);
			if(userId == 0)
				return;
			
			///移除channel和uid的对应关系
			ContextUtil.removeUserAndChannelRelation(channel);
			
			if(roomId > 0)
			{
				///将UID从房间对应的用户集合中移除
				roomRedis.quitRoom(roomId, userId);
				///移除当前websocket通道对应的roomId
				ContextUtil.removeRoomIdFromChannel(channel);
			}
			///移除uid和frontend的对应关系
			userRedis.removeFrontend(userId);
			///移除用户的token信息
			userRedis.removeToken(userId);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at UserLogicImpl.logout throw an error.", e);
		}
	}
}