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
package com.hipishare.chat.server.handler;

import java.util.concurrent.TimeUnit;

import com.hipishare.chat.server.codec.MsgObjectDecoder;
import com.hipishare.chat.server.codec.MsgObjectEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class SecureChatInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public SecureChatInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
    	// Handler在Pipeline中的执行顺序是InboundHandler顺序执行，OutboundHandler逆序执行
        ChannelPipeline pipeline = ch.pipeline();

        // Add SSL handler first to encrypt and decrypt everything.
        // In this example, we use a bogus certificate in the server side
        // and accept any invalid certificates in the client side.
        // You will need something more complicated to identify both
        // and server in the real world.
//        pipeline.addLast(sslCtx.newHandler(ch.alloc()));

        // On top of the SSL handler, add the text line codec.
//        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
        /*pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());*/
        pipeline.addLast(new MsgObjectDecoder());
        pipeline.addLast(new MsgObjectEncoder());
        
        // 心跳检测
        pipeline.addLast(new IdleStateHandler(160, 130, 0, TimeUnit.SECONDS));
        pipeline.addLast(new HeartBeatHandler());

        // and then business logic.
        pipeline.addLast(new SecureChatHandler());
    }
}
