/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.hipishare.chat.securetest;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.hipishare.chat.server.domain.ChatObject;
import com.hipishare.chat.server.domain.MsgObject;
import com.hipishare.chat.server.domain.RegisterCode;
import com.hipishare.chat.server.domain.User;
import com.hipishare.chat.server.enums.CmdEnum;

public final class SecureChatClient {

	private static Logger LOG = LogManager.getLogger(SecureChatClient.class.getName());

	private static ChannelFutureListener reConnectListener = null;

	private static Bootstrap bootstrap = null;

	private static Channel channel;

	static final String HOST = System.getProperty("host", "127.0.0.1");
//	static final String HOST = System.getProperty("host", "120.25.160.18");
	static final int PORT = Integer.parseInt(System.getProperty("port", "11210"));

	public static void main(String[] args) throws Exception {
		// Configure SSL.
		final SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();

		EventLoopGroup group = new NioEventLoopGroup();
		try {
			bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class)
				.handler(new SecureChatClientInitializer(sslCtx));

			// Start the connection attempt.
			ChannelFuture channelFuture = bootstrap.connect(HOST, PORT).sync();
			channel = channelFuture.channel();

			// add reconnect listener
			reConnectListener = new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						channel.close();
						channel = future.channel();
						LOG.info("重新连接服务器成功");
					} else {
						LOG.info("重新连接服务器失败");
						// 3秒后重新连接
						future.channel().eventLoop().schedule(new Runnable() {
							@Override
							public void run() {
								reConnect();
							}
						}, 3, TimeUnit.SECONDS);
					}
				}
			};

			// Read commands from the stdin.
			ChannelFuture lastWriteFuture = null;
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			for (;;) {
				String line = in.readLine();
				if (line == null) {
					break;
				}

				// Sends the received line to the server.
				if (!channel.isOpen()) {
					reConnect();
				}
				MsgObject msgObj = new MsgObject();
				Gson gson = new Gson();
				if ("1".equals(line)) {
					User user = new User();
					user.setAccount("peter");
					user.setPwd("666666");
					msgObj.setC(CmdEnum.LOGIN.getCmd());
					msgObj.setM(gson.toJson(user));
				} else if ("2".equals(line)) {
					ChatObject co = new ChatObject();
					co.setContent("hello,jack. I am peter.");
					co.setMsgType("text");
					co.setMsgTo("jack");
					co.setMsgFrom("peter");
					co.setCreateTime(System.currentTimeMillis());
					msgObj.setC(CmdEnum.CHAT.getCmd());
					msgObj.setM(gson.toJson(co));
				} else if ("3".equals(line)) {
					RegisterCode rc = new RegisterCode();
					rc.setMobile("13410969042");
					rc.setSign("fdsafsadf");
					msgObj.setC(CmdEnum.REGISTER_CODE.getCmd());
					msgObj.setM(gson.toJson(rc));
				} else if ("4".equals(line)) {
					RegisterCode rc = new RegisterCode();
					rc.setMobile("13410969042");
					rc.setCode("3243");
					rc.setSign("fdsafsadf");
					msgObj.setC(CmdEnum.REGISTER.getCmd());
					msgObj.setM(gson.toJson(rc));
				}
				String msg = gson.toJson(msgObj);
				ByteBuf buf = Unpooled.copiedBuffer(msg.getBytes());
				lastWriteFuture = channel.writeAndFlush(buf);

				// If user typed the 'bye' command, wait until the server closes
				// the connection.
				if ("bye".equals(line.toLowerCase())) {
					channel.closeFuture().sync();
					break;
				}
			}

			// Wait until all messages are flushed before closing the channel.
			if (lastWriteFuture != null) {
				lastWriteFuture.sync();
			}
			channel.closeFuture().sync();
		} finally {
			// The connection is closed automatically on shutdown.
			group.shutdownGracefully();
		}
	}

	public static void reConnect() {
		LOG.info("reConnect");
		ChannelFuture future = null;
		try {
			future = bootstrap.connect(HOST, PORT).sync();
			future.addListener(reConnectListener);
		} catch (Exception e) {
			e.printStackTrace();
			// future.addListener(channelFutureListener);
			future.removeListener(reConnectListener);
			LOG.info("关闭连接");
		}
	}
}
