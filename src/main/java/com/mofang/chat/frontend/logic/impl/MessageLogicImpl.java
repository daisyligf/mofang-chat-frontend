package com.mofang.chat.frontend.logic.impl;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mofang.chat.business.model.PostReplyNotify;
import com.mofang.chat.business.model.SysMessageNotify;
import com.mofang.chat.business.redis.GroupRedis;
import com.mofang.chat.business.redis.GuildRedis;
import com.mofang.chat.business.redis.UserRedis;
import com.mofang.chat.business.redis.WriteQueueRedis;
import com.mofang.chat.business.redis.impl.GroupRedisImpl;
import com.mofang.chat.business.redis.impl.GuildRedisImpl;
import com.mofang.chat.business.redis.impl.UserRedisImpl;
import com.mofang.chat.business.redis.impl.WriteQueueRedisImpl;
import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.business.entity.FriendMessage;
import com.mofang.chat.business.entity.GroupMessage;
import com.mofang.chat.business.entity.GroupMessageCollection;
import com.mofang.chat.business.entity.PrivateMessage;
import com.mofang.chat.business.entity.PrivateMessageCollection;
import com.mofang.chat.business.entity.RoomMessage;
import com.mofang.chat.business.entity.RoomMessageCollection;
import com.mofang.chat.business.entity.User;
import com.mofang.chat.business.service.FriendMessageService;
import com.mofang.chat.business.service.GroupMessageService;
import com.mofang.chat.business.service.PostReplyNotifyService;
import com.mofang.chat.business.service.PrivateMessageService;
import com.mofang.chat.business.service.RoomMessageService;
import com.mofang.chat.business.service.SysMessageNotifyService;
import com.mofang.chat.business.service.UserService;
import com.mofang.chat.business.service.impl.FriendMessageServiceImpl;
import com.mofang.chat.business.service.impl.GroupMessageServiceImpl;
import com.mofang.chat.business.service.impl.PostReplyNotifyServiceImpl;
import com.mofang.chat.business.service.impl.PrivateMessageServiceImpl;
import com.mofang.chat.business.service.impl.RoomMessageServiceImpl;
import com.mofang.chat.business.service.impl.SysMessageNotifyServiceImpl;
import com.mofang.chat.business.service.impl.UserServiceImpl;
import com.mofang.chat.business.sysconf.common.ChatType;
import com.mofang.chat.business.sysconf.common.PushDataType;
import com.mofang.chat.business.sysconf.ReturnCodeHelper;
import com.mofang.chat.business.sysconf.ReturnCode;
import com.mofang.chat.frontend.global.GlobalObject;
import com.mofang.chat.frontend.logic.MessageLogic;
import com.mofang.chat.frontend.util.ContextUtil;
import com.mofang.framework.util.StringUtil;
import com.mofang.framework.web.server.reactor.context.HttpRequestContext;
import com.mofang.framework.web.server.reactor.context.WebSocketRequestContext;

/**
 * 
 * @author zhaodx
 *
 */
public class MessageLogicImpl implements MessageLogic
{
	private final static MessageLogicImpl LOGIC = new MessageLogicImpl();
	private final static long MESSAGE_EXPIRE_TIME =  1577808000000L;    ///2020-01-01
	private RoomMessageService roomMessageService = RoomMessageServiceImpl.getInstance();
	private PrivateMessageService privateMessageService = PrivateMessageServiceImpl.getInstance();
	private GroupMessageService groupMessageService = GroupMessageServiceImpl.getInstance();
	private FriendMessageService friendNotifyService = FriendMessageServiceImpl.getInstance();
	private PostReplyNotifyService postReplyNotifyService = PostReplyNotifyServiceImpl.getInstance();
	private SysMessageNotifyService sysMessageNotifyService = SysMessageNotifyServiceImpl.getInstance();
	private UserService userService = UserServiceImpl.getInstance();
	private UserRedis userRedis = UserRedisImpl.getInstance();
	private GroupRedis groupRedis = GroupRedisImpl.getInstance();
	private GuildRedis guildRedis = GuildRedisImpl.getInstance();
	private WriteQueueRedis writeQueue = WriteQueueRedisImpl.getInstance();

	private MessageLogicImpl()
	{}
	
	public static MessageLogicImpl getInstance()
	{
		return LOGIC;
	}
	
	/**
	 * 发送聊天消息
	 */
	@Override
	public ResultValue sendMessage(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("send_msg_response");
		///根据channel 获取uid
		Channel channel = context.getChannelContext().channel();
		long userId = ContextUtil.getUserIdByChannel(channel);
		if(0 == userId)
		{
			result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
			return result;
		}
		String postData = context.getPostData();
		if(StringUtil.isNullOrEmpty(postData))
		{
			result.setCode(ReturnCode.CLIENT_REQUEST_DATA_IS_INVALID);
			return result;
		}
		
		try
		{
			JSONObject json = new JSONObject(postData);
			int chatType = json.optInt("chat_type");
			Long sequenceId = json.optLong("seq_id");
			result.setSequenceId(sequenceId);
			
			Long messageId = 0L;
			StringBuilder logMsg = new StringBuilder();
			if(chatType == ChatType.ROOM)  ///公聊
			{
				///获取roomId
				int roomId = ContextUtil.getRoomIdByChannel(channel);
				if(0 == roomId)
				{
					result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
					return result;
				}
				
				///构建消息体
				RoomMessage roomMessage = new RoomMessage();
				roomMessage.setFromUserId(userId);
				roomMessage.setContent(json.optString("content", ""));
				roomMessage.setMessageType(json.optInt("msg_type", 1));
				roomMessage.setDuration(json.optInt("duration", 0));
				roomMessage.setFontColor(json.optString("font_color", ""));
				roomMessage.setChatType(chatType);
				roomMessage.setTimeStamp(System.currentTimeMillis());
				roomMessage.setRoomId(roomId);
				roomMessage.setShowNotify(false);
				roomMessage.setClickAction("");
				messageId = roomMessageService.sendMessage(roomMessage);
				
				logMsg.append("act=send_room_msg&uid=" + userId + "&rId=" + roomId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.PRIVATE) ///私聊
			{
				///获取消息接收方uid
				Long toUserId = json.optLong("to_uid");
				if(null == toUserId || 0 == toUserId)
				{
					result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
					return result;
				}
				
				///鉴权
				boolean isAllow = userService.allowSendToUser(userId, toUserId);
				if(!isAllow) ///从服务端获取权限
				{
					result.setCode(ReturnCode.USER_NOT_HAVE_PRIVILEGE_TO_SEND_MESSAGE);
					return result;
				}
				
				PrivateMessage privateMessage = new PrivateMessage();
				privateMessage.setFromUserId(userId);
				privateMessage.setContent(json.optString("content", ""));
				privateMessage.setMessageType(json.optInt("msg_type", 1));
				privateMessage.setDuration(json.optInt("duration", 0));
				privateMessage.setChatType(chatType);
				privateMessage.setTimeStamp(System.currentTimeMillis());
				privateMessage.setToUserId(toUserId);
				privateMessage.setShowNotify(true);
				privateMessage.setClickAction("");
				privateMessage.setExpireTime(MESSAGE_EXPIRE_TIME);  ///永不过期
				messageId = privateMessageService.sendMessage(privateMessage);
				
				logMsg.append("act=send_private_msg&uid=" + userId + "&to_uid=" + toUserId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.GROUP) ///群聊
			{
				///获取群组ID
				Long groupId = json.optLong("group_id");
				if(null == groupId || 0 == groupId)
				{
					result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
					return result;
				}
				
				GroupMessage groupMessage = new GroupMessage();
				groupMessage.setGroupId(groupId);
				groupMessage.setFromUserId(userId);
				groupMessage.setContent(json.optString("content", ""));
				groupMessage.setMessageType(json.optInt("msg_type", 1));
				groupMessage.setDuration(json.optInt("duration", 0));
				groupMessage.setChatType(chatType);
				groupMessage.setTimeStamp(System.currentTimeMillis());
				groupMessage.setShowNotify(true);
				groupMessage.setClickAction("");
				messageId = groupMessageService.sendMessage(groupMessage);
				
				logMsg.append("act=send_group_msg&uid=" + userId + "&group_id=" + groupId + "&post_data=" + postData);
			}
			
			JSONObject data = new JSONObject();
			data.put("msg_id", messageId);
			result.setData(data);
			result.setCode(ReturnCode.SUCCESS);
			///记录日志
			GlobalObject.INFO_LOG.info(logMsg.toString());
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.sendMessage throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}
	
	/**
	 * 发送新消息push通知
	 */
	@Override
	public void pushNotify(HttpRequestContext context)
	{
		String postData = context.getPostData();
		pushNotify(postData);
	}
	
	/**
	 * 发送新消息push通知
	 */
	@Override
	public void pushNotify(String notify)
	{
		if(StringUtil.isNullOrEmpty(notify))
			return;
		
		JSONObject pushMsg = new JSONObject();
		try
		{
			JSONObject json = new JSONObject(notify);
			Channel channel = null;
			int dataType = json.optInt("push_data_type");
			long pushUserId = 0L; 
			StringBuilder logMsg = new StringBuilder();
			if(dataType == PushDataType.ROOM_NOTIFY)
			{
				int roomId = json.optInt("rid", 0);
				if(0 == roomId)
					return;
				
				pushMsg.put("act", "push_room_notify");
				pushMsg.put("target_id", roomId);
				pushUserId = json.optLong("uid");
				///记录日志
				logMsg.append("act=push_room_notify&rid=" + roomId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.PRIVATE_NOTIFY)
			{
				long fromUserId = json.optLong("from_uid");
				pushUserId = json.optLong("to_uid", 0);
				if(0 == fromUserId || 0 == pushUserId)
					return;
				
				pushMsg.put("act", "push_private_notify");
				PrivateMessage message = privateMessageService.getPushNotify(fromUserId, pushUserId);
				if(null == message)
					return;
				
				pushMsg.put("unread_count", message.getUnreadCount());
				pushMsg.put("is_show_notify", message.isShowNotify());
				pushMsg.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("content", message.getContent());
				msgJson.put("msg_type", message.getMessageType());
				JSONObject userJson = new JSONObject();
				userJson.put("id", fromUserId);
				User user = userService.getInfo(fromUserId);
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
				}
				msgJson.put("user", userJson);
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_private_notify&uid=" + fromUserId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.FRIEND_NOTIFY)
			{
				pushMsg.put("act", "push_friend_notify");
				long fromUserId = json.optLong("from_uid", 0);
				pushUserId = json.optLong("to_uid", 0);
				if(0 == fromUserId || 0 == pushUserId)
					return;
				
				FriendMessage message = friendNotifyService.getPushNotify(fromUserId, pushUserId);
				if(null == message)
					return;
				
				pushMsg.put("is_show_notify", message.isShowNotify());
				pushMsg.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("content", message.getContent());
				msgJson.put("msg_type", message.getMessageType());
				JSONObject userJson = new JSONObject();
				userJson.put("id", fromUserId);
				User user = userService.getInfo(fromUserId);
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
				}
				msgJson.put("user", userJson);
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_friend_notify&uid=" + fromUserId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.ROOM_ACTIVITY_NOTIFY)
			{
				int roomId = json.optInt("rid", 0);
				pushUserId = json.optLong("uid", 0);
				if(0 == roomId || 0 == pushUserId)
					return;
				
				pushMsg.put("act", "push_room_activity_notify");
				pushMsg.put("target_id", roomId);
				logMsg.append("act=push_room_activity_notify&rid=" + roomId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.GROUP_NOTIFY)
			{	
				long groupId = json.optLong("group_id");
				pushUserId = json.optLong("uid", 0);
				String groupName = json.optString("name", "");
				String guildAvatar = json.optString("avatar", "");
				if(0 == groupId || 0 == pushUserId)
					return;
				
				GroupMessage message = groupMessageService.getNotifyInfo(pushUserId, groupId);
				if(null == message)
					return;

				pushMsg.put("act", "push_group_notify");
				pushMsg.put("group_id", message.getGroupId());
				pushMsg.put("group_name", groupName);
				pushMsg.put("guild_avatar", guildAvatar);
				pushMsg.put("unread_count", message.getUnreadCount());
				pushMsg.put("is_show_notify", message.isShowNotify());
				pushMsg.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("content", message.getContent());
				msgJson.put("msg_type", message.getMessageType());
				JSONObject userJson = new JSONObject();
				userJson.put("id", message.getFromUserId());
				User user = userService.getInfo(message.getFromUserId());
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
				}
				msgJson.put("user", userJson);
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_group_notify&group_id=" + groupId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.POST_REPLY_NOTIFY)
			{
				long notifyId = json.optLong("notify_id");
				pushUserId = json.optLong("to_uid", 0);
				if(0 == notifyId || 0 == pushUserId)
					return;
				
				PostReplyNotify message = postReplyNotifyService.getInfo(notifyId);
				if(null == message)
					return;
				
				pushMsg.put("act", "push_post_reply_notify");
				pushMsg.put("is_show_notify", message.getIsShowNotify());
				pushMsg.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("notify_id", message.getNotifyId());
				msgJson.put("msg_type", message.getMessageType());
				
				JSONObject contentJson = new JSONObject();
				contentJson.put("post_id", message.getPostId());
				contentJson.put("post_title", message.getPostTitle());
				contentJson.put("reply_id", message.getReplyId());
				contentJson.put("reply_time", message.getReplyTime().getTime());
				contentJson.put("reply_type", message.getReplyType());
				
				JSONObject replyContentJson = new JSONObject();
				replyContentJson.put("text", message.getReplyText());
				String replyPictures = message.getReplyPictures();
				if(!StringUtil.isNullOrEmpty(replyPictures))
				{
					String[] pictures = replyPictures.split(",");
					JSONArray array = new JSONArray(Arrays.asList(pictures));
					replyContentJson.put("pictures", array);
				}
				contentJson.put("reply_content", replyContentJson);
				
				long fromUserId = message.getReplyUserId();
				JSONObject userJson = new JSONObject();
				userJson.put("id", fromUserId);
				User user = userService.getInfo(fromUserId);
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
					userJson.put("sex", user.getGender());
				}
				
				contentJson.put("reply_user", userJson);
				msgJson.put("content", contentJson);
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_post_reply_notify&notifyid=" + notifyId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.SYS_MESSAGE_NOTIFY)
			{
				long notifyId = json.optLong("notify_id");
				pushUserId = json.optLong("to_uid", 0);
				if(0 == notifyId || 0 == pushUserId)
					return;
				
				SysMessageNotify message = sysMessageNotifyService.getInfo(notifyId);
				if(null == message)
					return;
				
				pushMsg.put("act", "push_sys_msg_notify");
				pushMsg.put("is_show_notify", message.getIsShowNotify());
				pushMsg.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("notify_id", message.getNotifyId());
				msgJson.put("msg_type", message.getMessageType());
				msgJson.put("msg_category", message.getMessageCategory());
				
				JSONObject contentJson = new JSONObject();
				contentJson.put("title", message.getTitle());
				contentJson.put("detail", message.getDetail());
				contentJson.put("timestamp", message.getCreateTime().getTime());
				contentJson.put("icon", message.getIcon());
				String source = message.getSource();
				if(!StringUtil.isNullOrEmpty(source) && !source.equals("{}"))
				{
					JSONObject sourceJson = new JSONObject(source);
					contentJson.put("source", sourceJson);
				}
				msgJson.put("content", contentJson);
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_sys_msg_notify&notifyid=" + notifyId + "&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.USER_TASK_NOTIFY)
			{
				pushUserId = json.optLong("to_uid", 0);
				if(0 == pushUserId)
					return;
				
				JSONObject msgJson = json.optJSONObject("msg");
				if(null == msgJson)
					return;
				
				pushMsg.put("act", "push_task_notify");
				pushMsg.put("is_show_notify", json.optBoolean("is_show_notify", false));
				pushMsg.put("click_act", json.optString("click_act", ""));
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_task_notify&to_uid=" + pushUserId + "&notify=" + notify);
			}
			else if(dataType == PushDataType.USER_MEDAL_NOTIFY)
			{
				pushUserId = json.optLong("to_uid", 0);
				if(0 == pushUserId)
					return;
				
				JSONObject msgJson = json.optJSONObject("msg");
				if(null == msgJson)
					return;
				
				pushMsg.put("act", "push_medal_notify");
				pushMsg.put("is_show_notify", json.optBoolean("is_show_notify", false));
				pushMsg.put("click_act", json.optString("click_act", ""));
				pushMsg.put("msg", msgJson);
				logMsg.append("act=push_medal_notify&to_uid=" + pushUserId + "&notify=" + notify);
			}
			
			///处理推送消息
			///如果channel可用，则使用channel推送
			///如果channel不可用，则判断用户的平台
			///如果是ios设备，则获取用户的deviceToken, 使用apns进行推送
			channel = ContextUtil.getChannelByUserId(pushUserId);
			if(null != channel && channel.isOpen())
			{
				channel.writeAndFlush(new TextWebSocketFrame(pushMsg.toString()));
				///记录日志
				GlobalObject.INFO_LOG.info(logMsg.toString());
			}
			else
			{
				String token = userRedis.getToken(pushUserId);
				if(null != token)
				{
					 pushMsg.put("token", token);
					 ///将pushMsg添加到发送队列中
					 writeQueue.putAppleNotify(pushMsg.toString());
					 GlobalObject.INFO_LOG.info("act=push_notify&to_uid=" + pushUserId + "&msg=put notify to apple queue");
				}
				else
				{
					GlobalObject.INFO_LOG.info("act=push_notify&to_uid=" + pushUserId + "&msg=user offline");
				}
			}
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.pushNotify throw an error. parameter:" + notify, e);
		}
	}
	
	/**
	 * 拉取消息通知列表
	 * 公聊的消息通知不需要排序，私聊的消息通知需要按时间倒序
	 */
	@Override
	public ResultValue pullNotify(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("pull_notify_response");
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
			Integer chatType = json.optInt("chat_type");
			StringBuilder logMsg = new StringBuilder();
			if(null == chatType || chatType == 0)
			{
				result.setCode( ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			JSONArray data = null;
			if(chatType == ChatType.PRIVATE)
			{
				Channel channel = context.getChannelContext().channel();
				long userId = ContextUtil.getUserIdByChannel(channel);
				data = getPrivateNotify(userId);
				logMsg.append("act=pull_private_notify&uid=" + userId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.GROUP)
			{
				Channel channel = context.getChannelContext().channel();
				long userId = ContextUtil.getUserIdByChannel(channel);
				data = getGroupNotify(userId);
				logMsg.append("act=pull_group_notify&uid=" + userId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.POST_REPLY)
			{
				Channel channel = context.getChannelContext().channel();
				long userId = ContextUtil.getUserIdByChannel(channel);
				data = getPostReplyNotify(userId);
				logMsg.append("act=pull_post_reply_notify&uid=" + userId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.SYS_MESSAGE)
			{
				Channel channel = context.getChannelContext().channel();
				long userId = ContextUtil.getUserIdByChannel(channel);
				data = getSysMessageNotify(userId);
				logMsg.append("act=pull_sys_msg_notify&uid=" + userId + "&post_data=" + postData);
			}
			else
			{ 
				result.setCode( ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			///记录日志
			GlobalObject.INFO_LOG.info(logMsg.toString());
			
			if(null == data)
				result.setData(new JSONArray());
			else
				result.setData(data);
			
			result.setCode(ReturnCode.SUCCESS);
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.pullNotify throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}
	
	/**
	 * 拉取私聊消息通知
	 * @param userId
	 * @return
	 */
	private JSONArray getPrivateNotify(long userId)
	{
		try
		{
			JSONArray data = new JSONArray();
			JSONObject item = null;
			List<PrivateMessage> messages = privateMessageService.getPullNotify(userId);
			if(null == messages || messages.size() == 0)
				return null;
			
			long fromUserId = 0;
			for(PrivateMessage message : messages)
			{
				fromUserId = message.getFromUserId();
				item = new JSONObject();
				item.put("target_id", userId);
				item.put("unread_count", message.getUnreadCount());
				item.put("is_show_notify", message.isShowNotify());
				item.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("content", message.getContent());
				msgJson.put("msg_type", message.getMessageType());
				msgJson.put("time_stamp", message.getTimeStamp());
				JSONObject userJson = new JSONObject();
				userJson.put("id", fromUserId);
				User user = userService.getInfo(fromUserId);
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
				}
				msgJson.put("user", userJson);
				item.put("msg", msgJson);
				data.put(item);
			}
			return data;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getPrivateNotify throw an error. parameter:" + userId, e);
			return null;
		}
	}

	/**
	 * 拉取群聊消息通知
	 * @param userId
	 * @return
	 */
	private JSONArray getGroupNotify(long userId)
	{
		try
		{
			JSONArray data = new JSONArray();
			JSONObject item = null;
			List<GroupMessage> messages = groupMessageService.getNotifyList(userId);
			if(null == messages || messages.size() == 0)
				return null;
			
			long groupId = 0;
			long fromUserId = 0;
			JSONObject groupInfo = null;
			for(GroupMessage message : messages)
			{
				fromUserId = message.getFromUserId();
				groupId = message.getGroupId();
				item = new JSONObject();
				item.put("group_id", groupId);
				
				///获取群组名称
				groupInfo = groupRedis.getInfo(groupId);
				if(null != groupInfo)
				{
					item.put("group_name", groupInfo.optString("name", ""));
					long guildId = groupInfo.optLong("guildId", 0L);
					if(guildId > 0)
					{
						JSONObject guildInfo = guildRedis.getInfo(guildId);
						if(null != guildInfo)
							item.put("guild_avatar", guildInfo.optString("avatar", ""));
					}
				}
				item.put("unread_count", message.getUnreadCount());
				item.put("is_show_notify", message.isShowNotify());
				item.put("click_act", message.getClickAction());
				JSONObject msgJson = new JSONObject();
				msgJson.put("content", message.getContent());
				msgJson.put("msg_type", message.getMessageType());
				msgJson.put("time_stamp", message.getTimeStamp());
				JSONObject userJson = new JSONObject();
				userJson.put("id", fromUserId);
				User user = userService.getInfo(fromUserId);
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
				}
				msgJson.put("user", userJson);
				item.put("msg", msgJson);
				data.put(item);
			}
			return data;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getGroupNotify throw an error. parameter:" + userId, e);
			return null;
		}
	}
	
	/**
	 * 拉取帖子回复通知
	 * @param userId
	 * @return
	 */
	private JSONArray getPostReplyNotify(long userId)
	{
		try
		{
			JSONArray data = new JSONArray();
			JSONObject item = null;
			List<PostReplyNotify> notifies = postReplyNotifyService.getList(userId);
			if(null == notifies || notifies.size() == 0)
				return null;
			
			for(PostReplyNotify notify : notifies)
			{
				item = new JSONObject();
				item.put("notify_id", notify.getNotifyId());
				item.put("msg_type", notify.getMessageType());
				JSONObject contentJson = new JSONObject();
				contentJson.put("post_id", notify.getPostId());
				contentJson.put("post_title", notify.getPostTitle());
				contentJson.put("reply_id", notify.getReplyId());
				contentJson.put("reply_time", notify.getReplyTime().getTime());
				contentJson.put("reply_type", notify.getReplyType());
				
				JSONObject replyContentJson = new JSONObject();
				replyContentJson.put("text", notify.getReplyText());
				String replyPictures = notify.getReplyPictures();
				if(!StringUtil.isNullOrEmpty(replyPictures))
				{
					String[] pictures = replyPictures.split(",");
					JSONArray array = new JSONArray(Arrays.asList(pictures));
					replyContentJson.put("pictures", array);
				}
				contentJson.put("reply_content", replyContentJson);
				
				long fromUserId = notify.getReplyUserId();
				JSONObject userJson = new JSONObject();
				userJson.put("id", fromUserId);
				User user = userService.getInfo(fromUserId);
				if(null != user)
				{
					userJson.put("nick_name", user.getNickName());
					userJson.put("avatar", user.getAvatar());
					userJson.put("type", user.getType());
					userJson.put("sex", user.getGender());
				}
				
				contentJson.put("reply_user", userJson);
				item.put("content", contentJson);
				data.put(item);
			}
			return data;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getPostReplyNotify throw an error. parameter:" + userId, e);
			return null;
		}
	}
	
	/**
	 * 拉取系统消息通知
	 * @param userId
	 * @return
	 */
	private JSONArray getSysMessageNotify(long userId)
	{
		try
		{
			JSONArray data = new JSONArray();
			JSONObject item = null;
			List<SysMessageNotify> notifies = sysMessageNotifyService.getList(userId);
			if(null == notifies || notifies.size() == 0)
				return null;
			
			for(SysMessageNotify notify : notifies)
			{
				item = new JSONObject();
				item.put("notify_id", notify.getNotifyId());
				item.put("msg_type", notify.getMessageType());
				item.put("msg_category", notify.getMessageCategory());
				
				JSONObject contentJson = new JSONObject();
				contentJson.put("title", notify.getTitle());
				contentJson.put("detail", notify.getDetail());
				contentJson.put("timestamp", notify.getCreateTime().getTime());
				contentJson.put("icon", notify.getIcon());
				String source = notify.getSource();
				if(!StringUtil.isNullOrEmpty(source) && !source.equals("{}"))
				{
					JSONObject sourceJson = new JSONObject(source);
					contentJson.put("source", sourceJson);
				}
				item.put("content", contentJson);
				item.put("click_act", notify.getClickAction());
				data.put(item);
			}
			return data;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getSysMessageNotify throw an error. parameter:" + userId, e);
			return null;
		}
	}
	
	/**
	 * 拉取聊天消息列表
	 */
	@Override
	public ResultValue pullMessage(WebSocketRequestContext context)
	{
		ResultValue result = new ResultValue();
		result.setAction("pull_msg_response");
		String postData = context.getPostData();
		StringBuilder logMsg = new StringBuilder();
		if(StringUtil.isNullOrEmpty(postData))
		{
			result.setCode(ReturnCode.CLIENT_REQUEST_DATA_IS_INVALID);
			return result;
		}
		
		try
		{
			JSONObject json = new JSONObject(postData);
			Long sequenceId = json.optLong("seq_id");
			Integer targetId = json.optInt("target_id");
			Integer chatType = json.optInt("chat_type");
			Long minMsgId = json.optLong("msg_id_min", 0L);
			Long maxMsgId = json.optLong("msg_id_max", Long.MAX_VALUE);
			Integer pageSize = json.optInt("page_size", 50);
			
			result.setSequenceId(sequenceId);
			///如果为公聊，targetId为房间ID, 如果为私聊 targetId为fromUserId
			if(null == targetId || targetId == 0)
			{
				result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			if(null == chatType || chatType == 0)
			{
				result.setCode(ReturnCode.CLIENT_REQUEST_LOST_NECESSARY_PARAMETER);
				return result;
			}
			
			Channel channel = context.getChannelContext().channel();
			long userId = ContextUtil.getUserIdByChannel(channel);
			JSONObject data = null;
			if(chatType == ChatType.ROOM)
			{
				///返回聊天消息列表
				data = getRoomMessageList(targetId, minMsgId, maxMsgId, pageSize);
				logMsg.append("act=pull_room_msg&uid=" + userId + "&rid=" + targetId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.PRIVATE)
			{	
				///返回聊天消息列表
				data = getPrivateMessageList(targetId, userId, minMsgId, maxMsgId, pageSize);
				logMsg.append("act=pull_private_msg&to_uid=" + userId + "&from_uid=" + targetId + "&post_data=" + postData);
			}
			else if(chatType == ChatType.GROUP)
			{	
				///返回聊天消息列表
				data = getGroupMessageList(userId, targetId, minMsgId, maxMsgId, pageSize);
				logMsg.append("act=pull_group_msg&uid=" + userId + "&groupid=" + targetId + "&post_data=" + postData);
			}
			
			///记录日志
			GlobalObject.INFO_LOG.info(logMsg.toString());
			
			if(null == data)
				result.setData(new JSONObject());
			else
				result.setData(data);
			result.setCode(ReturnCode.SUCCESS);
			return result;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.pullMessage throw an error. parameter:" + postData, e);
			return ReturnCodeHelper.serverError(result);
		}
	}
	
	/**
	 * 获取指定房间公聊消息列表
	 * @param roomId
	 * @param minMessageId
	 * @param maxMessageId
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	private JSONObject getRoomMessageList(int roomId, long minMessageId, long maxMessageId, int pageSize)
	{
		try
		{
			RoomMessageCollection msgCollection = roomMessageService.getPullMessages(roomId, minMessageId, maxMessageId, pageSize);
			if(null == msgCollection)
				return null;
			
			JSONObject json = new JSONObject();
			json.put("msg_count", msgCollection.getCount());
			json.put("enter_user_count", msgCollection.getEnterRoomUserCount());
			JSONArray array = new JSONArray();
			JSONObject item = null;
			List<RoomMessage> messages = msgCollection.getMessage();
			if(null != messages && messages.size() > 0)
			{
				for(RoomMessage message : messages)
				{
					item = new JSONObject();
					item.put("msg_id", message.getMessageId());
					item.put("content", message.getContent());
					item.put("msg_type", message.getMessageType());
					item.put("time_stamp", message.getTimeStamp());
					item.put("duration", message.getDuration());
					item.put("font_color", message.getFontColor());
					item.put("status", message.getStatus());
					///构建消息发送者用户信息
					JSONObject userJson = new JSONObject();
					userJson.put("id", message.getFromUserId());
					User user = userService.getInfo(message.getFromUserId());
					if(null != user)
					{
						userJson.put("nick_name", user.getNickName());
						userJson.put("avatar", user.getAvatar());
						userJson.put("type", user.getType());
					}
					item.put("user", userJson);
					array.put(item);
				}
			}
			json.put("msg_list", array);
			return json;
		}
		catch(Exception e)
		{
			String parameter = "roomid:" + roomId + ",minMessageId:" + minMessageId + ", maxMessageId:" + maxMessageId + ",pageSize:" + pageSize;
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getRoomMessageList throw an error. parameter:" + parameter , e);
			return null;
		}
	}

	/**
	 * 获取指定私聊消息列表
	 * @param fromUserId
	 * @param toUserId
	 * @param minMessageId
	 * @param maxMessageId
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	private JSONObject getPrivateMessageList(long fromUserId, long toUserId, long minMessageId, long maxMessageId, int pageSize)
	{
		try
		{
			PrivateMessageCollection msgCollection = privateMessageService.getPullMessages(fromUserId, toUserId, minMessageId, maxMessageId, pageSize);
			if(null == msgCollection)
				return null;
			
			JSONObject json = new JSONObject();
			json.put("msg_count", msgCollection.getCount());
			JSONArray array = new JSONArray();
			JSONObject item = null;
			List<PrivateMessage> messages = msgCollection.getMessage();
			if(null != messages && messages.size() > 0)
			{
				for(PrivateMessage message : messages)
				{
					item = new JSONObject();
					item.put("msg_id", message.getMessageId());
					item.put("content", message.getContent());
					item.put("msg_type", message.getMessageType());
					item.put("time_stamp", message.getTimeStamp());
					item.put("duration", message.getDuration());
					///构建消息发送者用户信息
					JSONObject userJson = new JSONObject();
					userJson.put("id", message.getFromUserId());
					User user = userService.getInfo(message.getFromUserId());
					if(null != user)
					{
						userJson.put("nick_name", user.getNickName());
						userJson.put("avatar", user.getAvatar());
						userJson.put("type", user.getType());
					}
					item.put("user", userJson);
					array.put(item);
				}
			}
			json.put("msg_list", array);
			return json;
		}
		catch(Exception e)
		{
			String parameter = "fromUserId:" + fromUserId + ", toUserId:" + toUserId + ",minMessageId:" + minMessageId + ", maxMessageId:" + maxMessageId + ",pageSize:" + pageSize;
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getPrivateMessageList throw an error. parameter:" + parameter, e);
			return null;
		}
	}
	
	/**
	 * 获取指定群组消息列表
	 * @param groupId
	 * @param minMessageId
	 * @param maxMessageId
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	private JSONObject getGroupMessageList(long userId, long groupId, long minMessageId, long maxMessageId, int pageSize)
	{
		try
		{
			GroupMessageCollection msgCollection = groupMessageService.getMessages(userId, groupId, minMessageId, maxMessageId, pageSize);
			if(null == msgCollection)
				return null;
			
			JSONObject json = new JSONObject();
			json.put("msg_count", msgCollection.getCount());
			JSONArray array = new JSONArray();
			JSONObject item = null;
			List<GroupMessage> messages = msgCollection.getMessage();
			if(null != messages && messages.size() > 0)
			{
				for(GroupMessage message : messages)
				{
					item = new JSONObject();
					item.put("msg_id", message.getMessageId());
					item.put("content", message.getContent());
					item.put("msg_type", message.getMessageType());
					item.put("time_stamp", message.getTimeStamp());
					item.put("duration", message.getDuration());
					item.put("status", message.getStatus());
					///构建消息发送者用户信息
					JSONObject userJson = new JSONObject();
					userJson.put("id", message.getFromUserId());
					User user = userService.getInfo(message.getFromUserId());
					if(null != user)
					{
						userJson.put("nick_name", user.getNickName());
						userJson.put("avatar", user.getAvatar());
						userJson.put("type", user.getType());
					}
					item.put("user", userJson);
					array.put(item);
				}
			}
			json.put("msg_list", array);
			return json;
		}
		catch(Exception e)
		{
			String parameter = "groupid:" + groupId + ",minMessageId:" + minMessageId + ", maxMessageId:" + maxMessageId + ",pageSize:" + pageSize;
			GlobalObject.ERROR_LOG.error("at MessageLogicImpl.getGroupMessageList throw an error. parameter:" + parameter , e);
			return null;
		}
	}
}