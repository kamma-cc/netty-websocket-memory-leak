package cc.kamma.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.net.URISyntaxException;

public class Client {
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap boot = new Bootstrap();

        URI websocketURI = new URI("ws://localhost:8888/ws");

        boot.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        pipeline.addLast("ws-handler", new SimpleChannelInboundHandler<Object>() {

                            private WebSocketClientHandshaker handshaker;

                            @Override
                            public void channelActive(final ChannelHandlerContext ctx) throws Exception {
                                handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                                        websocketURI, WebSocketVersion.V13, null, false, EmptyHttpHeaders.INSTANCE);
                                handshaker.handshake(ctx.channel())                                 ;
                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (!handshaker.isHandshakeComplete()) {
                                    handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                                    ctx.channel().writeAndFlush(new TextWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{0,0,127,127,-1,-1,0,-1})));
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace();
                            }
                        });
                    }
                });
        //进行握手
        for (int i = 0; i < 100; i++) {
            boot.connect(websocketURI.getHost(), websocketURI.getPort()).sync();
        }
    }
}
