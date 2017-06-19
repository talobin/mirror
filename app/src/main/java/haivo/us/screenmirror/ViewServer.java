package haivo.us.screenmirror;

import android.content.res.AssetFileDescriptor;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;

class ViewServer extends NanoHTTPD {
    public HashMap hashMap;
    final private ServerController serverController;

    public ViewServer(ServerController serverController) {
        super(serverController.port2);
        this.serverController = serverController;

        this.hashMap = new HashMap();
    }

    private String getValue(String str) {
        for (Object str2 : this.hashMap.keySet()) {
            str = str.replace((CharSequence) str2, (CharSequence) this.hashMap.get(str2));
        }
        return str;
    }

    @Override
    public NanoHTTPD.Response serve(IHTTPSession ihttpSession) {
        try {
            String f = ihttpSession.getUri();
            if (f.equals("/")) {
                f = "/index.html";
            }
            if (f.endsWith(".js")) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "application/javascript",
                                                        getValue(ReadAndWrite.read(this.serverController.captureService.getResources()
                                                                                                                       .getAssets()
                                                                                                                       .open("http"
                                                                                                              + f))));
            } else if (f.endsWith(".html")) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "text/html",
                                                        getValue(ReadAndWrite.read(this.serverController.captureService.getResources()
                                                                                                                       .getAssets()
                                                                                                                       .open("http"
                                                                                                              + f))));
            } else if (f.endsWith(".css")) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "text/css",
                                                        ReadAndWrite.read(this.serverController.captureService.getResources()
                                                                                                              .getAssets()
                                                                                                              .open("http" + f)));
            } else {
                if (f.endsWith(".png")) {
                    AssetFileDescriptor openFd =
                        this.serverController.captureService.getResources().getAssets().openFd("http" + f);
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                            "image/png",
                                                            openFd.createInputStream(),
                                                            openFd.getLength());
                }
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 File Not Found");
            }
        } catch (IOException e) {
            Log.d("HAIHAI", e.getMessage());
        }
        return null;
    }

    public void putValue(String str, String str2) {
        this.hashMap.put("{{" + str + "}}", str2);
    }
}
