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
package sha;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;
import static sha.EchoServer.magic;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public final class EchoClient {
    private static final Logger log = LogManager.getLogger();

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.git
        final SslContext sslCtx;
        if (SSL) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new EchoClientHandler());
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.
//            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            //group.shutdownGracefully();
        }
    }

    static int bf = 4;
    static Utils.LatencyTimer t = new Utils.LatencyTimer("endToend");
    static class MyFutureListener implements ChannelFutureListener {
        ByteBuf msg;
        ChannelHandlerContext ctx;
        long tt = 0;

        public MyFutureListener(ByteBuf msg, ChannelHandlerContext ctx) {
            this.msg = msg;
            this.ctx = ctx;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            log.debug("success write success:{}", future.isSuccess());
            if(future.isSuccess()) {

                try {
                    log.debug("success");
                    msg.clear();
                    msg.writerIndex(msg.capacity());
                    msg.retain();

                    t.count(System.nanoTime()-tt);
                    tt = System.nanoTime();

                    log.debug("sending readindex:{} wrind:{} readablebytes:{} capacity:{} magic:{}", msg.readerIndex(), msg.writerIndex(), msg.readableBytes(), msg.capacity(), msg.getInt(0)==magic);
                    ChannelFuture channelFuture = ctx.writeAndFlush(msg);
                    channelFuture.addListener(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                log.debug("exception happened");
                future.cause().printStackTrace();
                ctx.close();
                ctx.channel().eventLoop().shutdownGracefully();
            }
        }
    }
    /**
     * Handler implementation for the echo client.  It initiates the ping-pong
     * traffic between the echo client and server by sending the first message to
     * the server.
     */
    static class EchoClientHandler extends ChannelInboundHandlerAdapter {

        private ByteBuf msg = null;

        public EchoClientHandler() {
            msg = Unpooled.directBuffer(4+8+4+bf);
            for(int i=0; i<msg.capacity(); i+=4) {
                msg.setInt(i, i);
            }
            msg.writeInt(magic);
            msg.writeLong(System.nanoTime());
            msg.writeInt(bf);
            msg.writerIndex(msg.capacity());
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {

            MyFutureListener mfl = new MyFutureListener(msg, ctx);
            msg.retain();
            log.debug("{} ", ByteBufUtil.hexDump(msg));
            ChannelFuture channelFuture = ctx.writeAndFlush(msg);
            channelFuture.addListener(mfl);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ctx.write(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }
}
