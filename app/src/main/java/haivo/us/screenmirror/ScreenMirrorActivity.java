package haivo.us.screenmirror;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ScreenMirrorActivity extends Activity implements ClientListener {
    private ToggleButton toggleButton;
    private TextView serverStatusTxt;
    private MediaProjectionManager mediaProjectionManager;
    private boolean isServiceBound;
    private CaptureService captureService;
    private MediaProjection mediaProjection;
    private TextView serverStatusHeadlineTxt;
    private TextView clientStatusTxt;
    private View f241i;
    private TextView errorTxt;
    private ServiceConnection serviceConnection;

    public ScreenMirrorActivity() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                captureService = ((CaptureServiceBinder) service).getCaptureSerivce();
                captureService.getServerController().setListener(ScreenMirrorActivity.this);
                updateView();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                captureService.getServerController().setListener(null);
                captureService = null;
            }
        };
    }

    private void updateToggleButton() {
        if (captureService == null || !captureService.isServerRunning()) {
            toggleButton.setChecked(false);
        } else {
            toggleButton.setChecked(true);
        }
    }

    private void ShowStartDialog() {
        new Builder(this).setTitle(R.string.dialog_wifi_title)
                         .setMessage(getString(R.string.dialog_wifi_text))
                         .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                             }
                         })
                         .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 dialog.cancel();
                             }
                         })
                         .show();
    }

    private void updateView() {
        if (captureService == null || !captureService.isServerRunning()) {
            f241i.setVisibility(View.GONE);
            serverStatusHeadlineTxt.setVisibility(View.GONE);
            serverStatusTxt.setText(getString(R.string.server_status_stopped));
            clientStatusTxt.setVisibility(View.GONE);
        } else {
            serverStatusHeadlineTxt.setVisibility(View.VISIBLE);
            serverStatusTxt.setText(getString(R.string.server_status,
                                              new Object[] { captureService.getServerController().getIPString2() }));
            f241i.setVisibility(View.VISIBLE);
            clientStatusTxt.setVisibility(View.VISIBLE);
        }
        if (captureService.getServerController().getIPAddress() != null) {
            clientStatusTxt.setText(getString(R.string.client_connected, captureService.getServerController().getIPAddress()));
            clientStatusTxt.setTextColor(getResources().getColor(R.color.client_connected));
        } else {
            clientStatusTxt.setText(getString(R.string.client_not_connected));
            clientStatusTxt.setTextColor(getResources().getColor(R.color.client_disconnected));
        }
        if (captureService.getServerController().getErrorMessage() != null) {
            errorTxt.setVisibility(View.VISIBLE);
            errorTxt.setText(getString(R.string.error, captureService.getServerController().getErrorMessage()));
        } else {
            errorTxt.setVisibility(View.GONE);
        }
        updateToggleButton();
    }

    void bindService() {
        bindService(new Intent(this, CaptureService.class), serviceConnection, 0);
        isServiceBound = true;
    }

    void unbindService() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public void startService() {
        startService(new Intent(this, CaptureService.class));
    }

    public boolean isConnectedToWifi() {
        NetworkInfo activeNetworkInfo =
            ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null
            && activeNetworkInfo.isConnected()
            && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        mediaProjection = mediaProjectionManager.getMediaProjection(i2, intent);
        if (mediaProjection != null) {
            if (VERSION.SDK_INT >= 23) {
                mediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        captureService.stop();
                    }
                }, new Handler(Looper.getMainLooper()));
            }
            captureService.setMediaProjection(mediaProjection);
        }
        updateView();
    }
    //HAI

    @Override
    public void onChange() {
        updateView();
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startService();
        setContentView(R.layout.main);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (captureService == null || captureService.isServerRunning()) {
                    if (captureService != null) {
                        captureService.stop();
                    }
                } else if (isConnectedToWifi()) {
                    mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 1);
                } else {
                    ShowStartDialog();
                    updateView();
                    return;
                }
                updateToggleButton();
            }
        });
        serverStatusTxt = (TextView) findViewById(R.id.serverStatus);
        serverStatusHeadlineTxt = (TextView) findViewById(R.id.serverStatusHeadline);
        f241i = findViewById(R.id.serverStatusLayout);
        clientStatusTxt = (TextView) findViewById(R.id.clientStatus);
        errorTxt = (TextView) findViewById(R.id.error);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, ScreenMirrorActivity.class));
                return true;
            case R.id.action_preferences:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService();
    }
}
