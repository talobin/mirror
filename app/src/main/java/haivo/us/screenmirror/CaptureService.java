package haivo.us.screenmirror;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class CaptureService extends Service implements OnSharedPreferenceChangeListener {
    private final IBinder iBinder;
    private ServerController serverController;

    public CaptureService() {
        iBinder = new CaptureServiceBinder(this);
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        serverController.setMediaProjection(mediaProjection);
        serverController.startServer();
        if (serverController.isServerRunning()) {
            startForeground(1, createNotification());
        } else {
            stop();
        }
    }

    public boolean isServerRunning() {
        return serverController.isServerRunning();
    }

    public void stop() {
        serverController.stop();
        if (serverController.getMediaProjection() != null) {
            serverController.getMediaProjection().stop();
        }
        stopForeground(true);
    }

    public ServerController getServerController() {
        return serverController;
    }

    public Notification createNotification() {
        Log.d("HAIHAI","createNotification");
        Intent intent = new Intent(this, ScreenMirrorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent activity = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Builder builder = new Builder(this);
        if (serverController.getIPAddress() != null) {
            builder.setContentText(getString(R.string.client_connected, serverController.getIPAddress()));
        } else {
            builder.setContentText(getString(R.string.access_at) + serverController.getIPString2());
        }
        builder.setContentTitle(getString(R.string.mirroring_active))
               .setContentIntent(activity)
               .setLocalOnly(true)
               .setAutoCancel(true)
               .setSmallIcon(R.drawable.ic_cast_connected_white_24dp)
               .setColor(Color.parseColor("#607D8B"));
        return builder.build();
    }

    public void sendNotification() {
        Log.d("HAIHAI","sendNotification");
        if (serverController.isServerRunning()) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, createNotification());
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("HAIHAI","onBind");
        return iBinder;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        serverController = new ServerController(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        Log.d("HAIHAI","onSharedPreferenceChanged "+ str);
        if (str.equals(getString(R.string.pref_http_port_key))) {
            serverController.restartServer();
        } else {
            serverController.restartEncoder();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d("HAIHAI","onStartCommand");
        return START_NOT_STICKY;
    }
}
