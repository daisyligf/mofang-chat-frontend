package com.mofang.chat.frontend.global;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * 
 * @author zhaodx
 *
 */
public class GlobalObject
{
	/**
	 * Global Info Logger Instance 
	 */
	public final static Logger INFO_LOG = Logger.getLogger("frontend.info");
	
	/**
	 * Global Error Logger Instance
	 */
	public final static Logger ERROR_LOG = Logger.getLogger("frontend.error");
	
	/**
	 * websocket通道对应的UID 集合
	 * 用于根据channel 获取用户ID, 类似操作如发布消息时，客户端不会传uid参数，服务端是根据当前channel从内存中读取 
	 * 用户ID只有当第一次握手时, 通过原子封装的参数来获取,此后会一直保留在内存里
	 * 直到客户端断开连接，才会将对应关系移除
	 */
	public final static ConcurrentHashMap<Channel, Long> CHANNEL_UID_MAP = new ConcurrentHashMap<Channel, Long>();
	
	/**
	 * uid对应的websocket通道 集合 
	 * 根据用户ID获取channel, 类似操作如push公聊消息通知时, frontend 会根据roomid 获取到当前frontend存储的uid列表
	 * 再遍历uid列表，获取uid对应的channel, 通过channel 发布push通知
	 * 如果客户端断开连接，则会将对应关系移除
	 */
	public final static ConcurrentHashMap<Long, Channel> UID_CHANNEL_MAP = new ConcurrentHashMap<Long, Channel>();
	
	/**
	 * websocket通道对应的roomId
	 * 根据websocket通道获取roomId, 类似操作如 发布公聊消息时，客户端不会传roomId，服务端根据当前channel从内存中读取
	 * 当调用enter room接口时，会设置对应关系, 调用quit room 时，清除关系
	 * 如果客户端断开连接，则会将对应关系移除
	 */
	public final static ConcurrentHashMap<Channel, Integer> CHANNEL_ROOMID_MAP = new ConcurrentHashMap<Channel, Integer>();
	
}