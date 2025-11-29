package wsMercado;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;

public class Communication {
    private ConcurrentHashMap<Integer,AsynchronousSocketChannel>  peersSend = new ConcurrentHashMap<Integer,AsynchronousSocketChannel>();
    private AsynchronousServerSocketChannel server; 

      public void startServer(int port) throws IOException {
        server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));
        acceptLoop();
    }

    private void acceptLoop() {
        server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel ch, Void att) {
                acceptLoop(); // accept next connection
                // start async read for this connection
                startReading(ch);
            }

            @Override
            public void failed(Throwable exc, Void att) { exc.printStackTrace(); }
        });
    }

    // connect to a peer and store the channel
    public void connectToPeer(int peerId, String host, int port) throws Exception {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        client.connect(new InetSocketAddress(host, port)).get();
        peersSend.put(peerId, client);
    }

    public void sendMessage(int peerId, String msg) throws Exception {
        AsynchronousSocketChannel ch = peersSend.get(peerId);
        if (ch != null) {
            msg += "\n";
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            ch.write(buffer);
        }
    }


    public void sendToAllPeers(String msg){
        msg += "\n";
        for(AsynchronousSocketChannel peer : peersSend.values()){
            if(peer != null){
               
                byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                peer.write(buffer);
            }
        }
    }

    private void startReading(AsynchronousSocketChannel ch) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        ch.read(buf, buf, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer bytesRead, ByteBuffer attachment) {
                buf.flip();
                byte[] data = new byte[buf.remaining()];
                buf.get(data);
                String message = new String(data, StandardCharsets.UTF_8);
                System.out.println("Received: " + message);
                // continue reading
                buf.clear();
                ch.read(buf, buf, this);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                 exc.printStackTrace(); 
            }
        });
    }
}   
