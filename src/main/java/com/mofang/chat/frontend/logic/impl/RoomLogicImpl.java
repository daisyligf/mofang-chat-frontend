package com.mofang.chat.frontend.logic.impl;

import io.netty.channel.Channel;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mofang.chat.business.service.RoomService;
import com.mofang.chat.business.service.impl.RoomServiceImpl;
import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.business.entity.User;
import com.mofang.chat.business.redis.RoomRedis;
import com.mofang.chat.business.redis.impl.RoomRedisImpl;
import com.mofang.chat.frontend.logic.RoomLogic;
import com.mofang.chat.business.sysconf.ReturnCodeHelper;
import com.mofang.chat.business.sysconf.ReturnCode;
import com.mofang.chat.frontend.global.GlobalObject;
import com.mofang.chat.frontend.util.ContextUtil;
import com.mofang.framework.util.StringUtil;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
public class RoomLogicImpl implements RoomLogic
{
	private final static RoomLogicImpl LOGIC = new RoomLogicImpl();
	private RoomRedis roomRedis = RoomRedisImpl.getInstance();
	private RoomService roomService = RoomServiceImpl.getInstance();
	
	private RoomLogicImpl()
	{}
	
	public static RoomLogicImpl getInstance()
	{
		return LOGIC;
	}
	
	@Override
	public ResultValue enter(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("enter_room_response");
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
			int roomId = json.optInt("rid");
			
			///根据channel 获取uid
			long userId = ContextUtil.getUserIdByChannel(context.getChannelContext().channel());
			if(0 == userId)
			{
				result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			///设置当前websocket通道对应的roomId
			ContextUtil.setRoomIdToChannel(context.getChannelContext().channel(), roomId);
			///将UID添加到房间对应的的用户集合
			roomRedis.enterRoom(roomId, userId);
			///记录日志
			GlobalObject.INFO_LOG.info("act=enter_room&uid=" + userId + "&rid=" + roomId + "&post_data=" + postData);
			///返回信息
			result.setCode(ReturnCode.SUCCESS);
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at RoomLogicImpl.enter throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}

	@Override
	public ResultValue quit(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("quit_room_response");
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
			Channel channel = context.getChannelContext().channel();
			int roomId = ContextUtil.getRoomIdByChannel(channel);
			long userId = ContextUtil.getUserIdByChannel(channel);
			if(0 == roomId || 0 == userId)
			{
				result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			///移除当前websocket通道对应的roomId
			ContextUtil.removeRoomIdFromChannel(channel);
			///将UID从房间对应的用户集合中移除
			roomRedis.quitRoom(roomId, userId);
			///记录日志
			GlobalObject.INFO_LOG.info("act=quit_room&uid=" + userId + "&rid=" + roomId + "&post_data=" + postData);
			///返回信息
			result.setCode(ReturnCode.SUCCESS);
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at RoomLogicImpl.quit throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}

	@Override
	public ResultValue getSubscribeUsers(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("get_subscribe_user_response");
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
			
			int roomId = json.optInt("rid", 0);
			int start = json.optInt("start", 0);
			int size = json.optInt("size", 50);
			int end = start + size - 1;
			if(0 == roomId)
			{
				result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			JSONObject data = new JSONObject();
			List<User> userList = roomService.getSubscribeUsers(roomId, start, end);
			long count = roomRedis.getSubscribeUserCount(roomId);
			data.put("count", count);
			if(null != userList && userList.size() > 0)
			{
				JSONArray array = new JSONArray();
				JSONObject item = null;
				for(User user : userList)
				{
					item = new JSONObject();
					item.put("id", user.getUserId());
					item.put("nick_name", user.getNickName());
					item.put("avatar", user.getAvatar());
					item.put("type", user.getType());
					item.put("gender", user.getGender());
					array.put(item);
				}
				data.put("users", array);
			}
			///记录日志
			GlobalObject.INFO_LOG.info("act=get_subscribe_users&post_data=" + postData);
			///返回信息
			result.setData(data);
			result.setCode(ReturnCode.SUCCESS);
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at RoomLogicImpl.getSubscribeUsers throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}
}