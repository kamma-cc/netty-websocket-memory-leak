package cc.kamma.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class Server {
    public static void main(String[] args) {
        final EventLoopGroup bossGroup;
        final EventLoopGroup workerGroup;
        final Class<? extends ServerSocketChannel> channelClass;

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        channelClass = NioServerSocketChannel.class;

        try {
            final ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup).channel(channelClass)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast("http-codec", new HttpServerCodec())
                                    .addLast("http-aggregator", new HttpObjectAggregator(65536))
                                    .addLast("websocket", new WebSocketServerProtocolHandler("/ws", true))
                                    .addLast("exception-handler", new SimpleChannelInboundHandler<Object>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            System.out.println("receive");
                                        }
                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            System.out.println(cause.getMessage());
                                            System.gc();
                                        }
                                    })
                            ;
                        }
                    });

            final Channel channel = b.bind(8888).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException ignore) {
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
