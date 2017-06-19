package haivo.us.screenmirror;

import android.os.Binder;

class CaptureServiceBinder extends Binder {
    final private CaptureService captureService;

    CaptureServiceBinder(CaptureService captureService) {
        this.captureService = captureService;
    }

    CaptureService getCaptureSerivce() {
        return captureService;
    }
}
