package sha;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;


public class BenchmarkTcpServer extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger();

    private byte[] readArray = new byte[1024 * 1024];
static Utils.LatencyTimer t = new Utils.LatencyTimer("serverLatency", 0, 2000, 2000);
    static Utils.Timer timer= new Utils.Timer("serverTrpt");

    private final int msgSize;

    public BenchmarkTcpServer() throws IOException {
        super();
        this.msgSize = 256;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        log.debug("came oon server");
        ByteBuf in = (ByteBuf) msg;
        ByteBuffer bb = in.nioBuffer();

        handleBuffer(ctx, bb, msg);
    }

    private void handleBuffer(ChannelHandlerContext ctx, ByteBuffer buf, Object msg) {

        while(buf.remaining() >= msgSize) {
            int pos = buf.position();
            int lim = buf.limit();
            buf.limit(pos + msgSize);
            handleMessage(ctx, buf, msg);
            buf.limit(lim).position(pos + msgSize);
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, ByteBuffer buf, Object msg) {
        int pos = buf.position();

        long tsReceived = buf.getLong();

        buf.get(readArray, 0, buf.remaining());
        timer.count(msgSize);
        t.count(System.nanoTime() - tsReceived);
        if (tsReceived > 0) {

        } else if (tsReceived == -1) {
            // first message
        } else if (tsReceived == -2) {
            // last message
            ctx.close();
            printResults();
            return;
        } else if (tsReceived < 0) {
            System.err.println("Received bad timestamp: " + tsReceived);
            ctx.close();
            return;
        }

        buf.position(pos);
        ctx.writeAndFlush(msg);
    }

    private void printResults() {
        StringBuilder results = new StringBuilder();
        results.append("results=");
//        results.append(bench.results());
        System.out.println(results);
    }

    public static void main(String[] args) throws Exception {

        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8087;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new BenchmarkTcpServer());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync(); // (7)
            BenchmarkTcpClient.main(new String[0]);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
