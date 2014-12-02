package com.mofang.chat.frontend.util;

import io.netty.channel.Channel;

import com.mofang.chat.frontend.global.GlobalObject;

/**
 * 
 * @author zhaodx
 *
 */
public class ContextUtil
{
	public synchronized static boolean setUserAndChannelRelation(long userId, Channel channel)
	{
		if(null == channel)
			return false;
		
		Channel oldChannel = GlobalObject.UID_CHANNEL_MAP.get(userId);
		if(null != oldChannel && !oldChannel.equals(channel))
		{	
			GlobalObject.CHANNEL_UID_MAP.remove(oldChannel);
			if(oldChannel.isOpen())
				oldChannel.close();
		}
		
		GlobalObject.UID_CHANNEL_MAP.put(userId, channel);
		GlobalObject.CHANNEL_UID_MAP.put(channel, userId);
		return true;
	}
	
	public synchronized static boolean removeUserAndChannelRelation(Channel channel)
	{
		if(null == channel)
			return false;
		
		Long userId = GlobalObject.CHANNEL_UID_MAP.get(channel);
		if(null == userId)
			return false;
		
		GlobalObject.UID_CHANNEL_MAP.remove(userId);
		GlobalObject.CHANNEL_UID_MAP.remove(channel);
		return true;
	}
	
	public static long getUserIdByChannel(Channel channel)
	{
		if(null == channel)
			return 0L;
		Long userId = GlobalObject.CHANNEL_UID_MAP.get(channel);
		if(null == userId)
			return 0L;
		return userId;
	}
	
	/*
	public static boolean setUserIdToChannel(Channel channel, long userId)
	{
		if(null == channel)
			return false;
		GlobalObject.CHANNEL_UID_MAP.put(channel, userId);
		return true;
	}
	
	public static boolean removeUserIdFromChannel(Channel channel)
	{
		if(null == channel)
			return false;
		GlobalObject.CHANNEL_UID_MAP.remove(channel);
		return true;
	}
	*/
	public static Channel getChannelByUserId(long userId)
	{
		return GlobalObject.UID_CHANNEL_MAP.get(userId);
	}
	/*
	public static boolean setChannelToUserId(long userId, Channel channel)
	{
		if(null == channel)
			return false;
		GlobalObject.UID_CHANNEL_MAP.put(userId, channel);
		return true;
	}
	
	public static boolean removeChannelFromUserId(long userId)
	{
		GlobalObject.UID_CHANNEL_MAP.remove(userId);
		return true;
	}
	*/
	public static int getRoomIdByChannel(Channel channel)
	{
		if(null == channel)
			return 0;
		Integer roomId = GlobalObject.CHANNEL_ROOMID_MAP.get(channel);
		if(null == roomId)
			return 0;
		return roomId;
	}
	
	public static boolean setRoomIdToChannel(Channel channel, Integer roomId)
	{
		if(null == channel)
			return false;
		GlobalObject.CHANNEL_ROOMID_MAP.put(channel, roomId);
		return true;
	}
	
	public static boolean removeRoomIdFromChannel(Channel channel)
	{
		if(null == channel)
			return false;
		GlobalObject.CHANNEL_ROOMID_MAP.remove(channel);
		return true;
	}
}