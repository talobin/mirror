package haivo.us.screenmirror;

import android.content.Context;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodec.CodecException;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class ServerController {
    public static final FrameConfig[] frameConfigArray;
    private WSServer wsServer;
    private ViewServer viewServer;
    private MediaCodec mediaCodec;
    private Surface surface;
    private String string;
    public int port1;
    public int port2;
    private boolean isEncoderRunning;
    public CaptureService captureService;
    private boolean isServerRunning;
    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;
    public WebSocket webSocket;
    private boolean isPortraitMode;
    private String errorMessage;
    private ClientListener clientListener;
    private Handler handler;

    static class FrameConfig {
        int bitRate;
        int frameRate;
        int frameInterval;
        int f272d;

        private FrameConfig(int i, int i2, int i3, int i4) {
            bitRate = i;
            frameRate = i2;
            frameInterval = i3;
            f272d = i4;
        }
    }

    static {
        frameConfigArray = new FrameConfig[] {
            new FrameConfig(15, 10, 120, 120),
            new FrameConfig(30, 10, 160, 160),
            new FrameConfig(30, 10, 200, 240),
            new FrameConfig(60, 10, 240, 320),
            new FrameConfig(60, 10, 320, 480),
            //new FrameConfig(120, 10, 480, 640)
        };
    }

    public ServerController(final CaptureService captureService) {
        errorMessage = null;
        handler = new Handler(Looper.getMainLooper());
        this.captureService = captureService;
        OrientationEventListener orientationEventListener = new OrientationEventListener(captureService) {
            @Override
            public void onOrientationChanged(int orientation) {
                boolean portrait = true;
                if (Settings.System.getInt(captureService.getContentResolver(), "accelerometer_rotation", 0) == 1) {
                    DisplayMetrics displayMetrics = captureService.getResources().getDisplayMetrics();
                    if (displayMetrics.widthPixels <= displayMetrics.heightPixels) {
                        portrait = false;
                    }
                    if (isPortraitMode != portrait) {
                        isPortraitMode = portrait;
                        restartEncoder();
                    }
                }
            }
        };
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
    }

    void onError(String str) {
        Log.d("HAIHAI", "onError " + str);
        errorMessage = str;
        releaseEncoder();
        stop();
        notifyConnectionChange();
    }

    public static boolean closeConnection(int port) {
        try {
            ServerSocket server = new ServerSocket(port);
            if (server != null && !server.isClosed()) {
                server.close();
                return true;
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private void closeAllConnections() {
        while (!ServerController.closeConnection(port2)) {
            port2++;
        }
        port1 = 50371;
        while (!ServerController.closeConnection(port1)) {
            port1++;
        }
    }

    private String getClientIP() {
        int ipAddress = ((WifiManager) captureService.getApplicationContext()
                                                     .getSystemService(Context.WIFI_SERVICE)).getConnectionInfo()
                                                                                             .getIpAddress();
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }
        byte[] toByteArray = BigInteger.valueOf((long) ipAddress).toByteArray();
        String str = null;
        try {
            str = InetAddress.getByAddress(toByteArray).getHostAddress();
        } catch (UnknownHostException e) {
            Log.d("HAIHAI", e.getMessage());
        }
        return str;
    }

    private FrameConfig getSavedFrameConfig() {
        return frameConfigArray[Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(captureService)
                                                                  .getString(captureService.getString(R.string.pref_quality_key),
                                                                             captureService.getResources()
                                                                                           .getStringArray(R.array.pref_quality_entryValues)[1]))];
    }

    private void setUpEncoder() {
        releaseEncoder();
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            Log.e("HAIHAI", e.getMessage());
        }
        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

            }
            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec,
                                                int index,
                                                @NonNull MediaCodec.BufferInfo info) {
                try {
                    if (isEncoderRunning()) {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                        if (outputBuffer != null) {
                            outputBuffer.position(info.offset);
                            outputBuffer.limit(info.offset + info.size);
                            wsServer.sendByte(outputBuffer);
                        }
                        codec.releaseOutputBuffer(index, false);
                    }
                } catch (IllegalStateException e) {
                    if (isEncoderRunning()) {
                        restartEncoder();
                    } else {
                        releaseEncoder();
                    }
                }
            }
            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull CodecException e) {

            }
            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });
        FrameConfig q = getSavedFrameConfig();
        DisplayMetrics displayMetrics = captureService.getResources().getDisplayMetrics();
        float minDensity = Math.min(1.0f, ((float) q.f272d) / ((float) displayMetrics.densityDpi));
        int scaledWidth = (int) (((float) displayMetrics.widthPixels) * minDensity);
        scaledWidth = Math.round(((float) scaledWidth) / 16.0f) * 16;
        int round = Math.round(((float) ((int) (((float) displayMetrics.heightPixels) * minDensity))) / 16.0f) * 16;
        HandlerThread handlerThread = new HandlerThread("CaptureThread");
        handlerThread.start();
        virtualDisplay = mediaProjection.createVirtualDisplay("mirror",
                                                              scaledWidth,
                                                              round,
                                                              q.f272d,
                                                              1,
                                                              null,
                                                              null,
                                                              new Handler(handlerThread.getLooper()));
        MediaFormat createVideoFormat = MediaFormat.createVideoFormat("video/avc", scaledWidth, round);
        //try {
        //    MediaCodecInfo.CodecCapabilities codecCapabilities = MediaCodec.createEncoderByType("video/avc").getCodecInfo().getCapabilitiesForType("video/avc");
        //    if(codecCapabilities.)
        //
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
        createVideoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        createVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        createVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 100000*q.frameInterval);
        createVideoFormat.setFloat(MediaFormat.KEY_FRAME_RATE, q.frameInterval);
        createVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        //MediaFormat createVideoFormat = MediaFormat.createVideoFormat("video/avc", scaledWidth, round);
        //createVideoFormat.setInteger("color-format", 2130708361);
        //createVideoFormat.setInteger("bitrate", q.bitRate * 1024);
        //createVideoFormat.setFloat("frame-rate", q.frameRate);
        //createVideoFormat.setInteger("i-frame-interval", q.frameInterval);
        try {
            mediaCodec.configure(createVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();
            virtualDisplay.setSurface(surface);
        } catch (CodecException e2) {
            onError("Encoder does not support quality setting");
        } catch (Exception e3) {
            onError("Unknown error while configuring the h264 encoder");
        }
    }

    public void newConnection() {
        errorMessage = null;
        notifyConnectionChange();
    }

    public MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void setListener(ClientListener clientListener) {
        this.clientListener = clientListener;
    }

    public String getIPString2() {
        return string + ":" + port2;
    }

    public String getIPString1() {
        return string + ":" + port1;
    }

    public void startServer() {
        if (!isServerRunning) {
            string = getClientIP();
            port2 = PreferenceManager.getDefaultSharedPreferences(captureService)
                                     .getInt(captureService.getString(R.string.pref_http_port_key), 8080);
            closeAllConnections();
            viewServer = new ViewServer(this);
            viewServer.putValue("addr", getIPString1());
            viewServer.putValue("version", "1.0.12");
            viewServer.putValue("title", Build.MODEL);
            try {
                newConnection();
                viewServer.start();
                wsServer = new WSServer();
                wsServer.start();
                isServerRunning = true;
                notifyConnectionChange();
            } catch (Exception e) {
                onError(e.getMessage());
            }
        }
    }

    public void stop() {
        if (isServerRunning) {
            releaseEncoder();
            try {
                wsServer.stop();
            } catch (Exception e) {
                Log.d("HAIHAI", e.getMessage());
            } finally {
                Log.d("HAIHAI", "Stopping view server");
                viewServer.stop();
                isServerRunning = false;
                notifyConnectionChange();   
            }
        }
    }

    public void startEncoder() {
        if (!isEncoderRunning) {
            setUpEncoder();
            try {
                mediaCodec.start();
            } catch (IllegalStateException e) {
                onError("Encoder does not support quality setting " + e);
            }
            isEncoderRunning = true;
            notifyConnectionChange();
        }
    }

    public void releaseEncoder() {
        if (isEncoderRunning) {
            try {
                mediaCodec.stop();
                surface.release();
                mediaCodec.release();
            } catch (Exception e) {
            }
            virtualDisplay.release();
            isEncoderRunning = false;
            notifyConnectionChange();
        }
    }

    public void restartEncoder() {
        if (isServerRunning() && isEncoderRunning()) {
            releaseEncoder();
            startEncoder();
        }
    }

    public void restartServer() {
        if (isServerRunning()) {
            stop();
            startServer();
        }
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    public boolean isEncoderRunning() {
        return isEncoderRunning;
    }

    //HAI
    // getLocal or getRemote
    public String getIPAddress() {
        return webSocket != null ? webSocket.getRemoteSocketAddress().getAddress().getHostAddress() : null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void notifyConnectionChange() {
        if (clientListener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    captureService.sendNotification();
                    clientListener.onChange();
                }
            });
        }
    }

    private class WSServer extends WebSocketServer {

        public WSServer() {
            super(new InetSocketAddress(port1));
        }

        public void sendByte(ByteBuffer byteBuffer) {
            if (ServerController.this.webSocket != null && ServerController.this.webSocket.isOpen()) {
                ServerController.this.webSocket.send(byteBuffer);
            }
        }
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            Log.d("HAIHAI",
                  "onOpen "
                      + "local "
                      + webSocket.getLocalSocketAddress()
                      + "remote "
                      + webSocket.getRemoteSocketAddress());
            if (ServerController.this.webSocket == null) {
                ServerController.this.webSocket = webSocket;
                startEncoder();
                newConnection();
                return;
            }
            //webSocket.close();
        }
        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            Log.d("HAIHAI",
                  "onClose "
                      + i
                      + b
                      + s
                      + "local "
                      + webSocket.getLocalSocketAddress()
                      + "remote "
                      + webSocket.getRemoteSocketAddress());
            if (webSocket == ServerController.this.webSocket) {
                ServerController.this.webSocket = null;
                notifyConnectionChange();
                releaseEncoder();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteBuffer buffer) {
            Log.d("HAIHAI",
                  "onMessage "
                      + buffer.array().length
                      + "local "
                      + webSocket.getLocalSocketAddress()
                      + "remote "
                      + webSocket.getRemoteSocketAddress());
        }
        @Override
        public void onMessage(WebSocket webSocket, String s) {
            Log.d("HAIHAI",
                  "onMessage "
                      + s
                      + "local "
                      + webSocket.getLocalSocketAddress()
                      + "remote "
                      + webSocket.getRemoteSocketAddress());
        }
        @Override
        public void onError(WebSocket webSocket, Exception e) {
            Log.d("HAIHAI", "onError " + e.getMessage());
            if (webSocket == ServerController.this.webSocket) {
                ServerController.this.webSocket = null;
                releaseEncoder();
                ServerController.this.onError("Connection lost");
            }
        }
    }
}
