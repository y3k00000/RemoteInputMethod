package y3k.remoteinputmethod;

import android.graphics.Color;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import fi.iki.elonen.NanoHTTPD;

public class ImeService extends InputMethodService implements View.OnClickListener, Handler.Callback{

    private final static String tag = ImeService.class.getName();
    private NanoHTTPD nanoHTTPD;
    TextView addressTextView;
    FrameLayout addressLayout;
    Handler handler = new Handler(this);

    @Override
    public void onCreate() {
        Log.d(tag, "onCreate()");
        super.onCreate();
        this.nanoHTTPD = new NanoHTTPD(4096) {
            @Override
            public Response serve(IHTTPSession session) {
                String text = session.getParms().get("text");
                Log.d(tag,"NanoHTTPD got text = "+text);
                Message message = new Message();
                message.obj = text;
                handler.sendMessage(message);
                return newFixedLengthResponse("ok");
            }
        };
        this.addressTextView = new TextView(this);
        this.addressTextView.setText("http://" + getLocalIpAddress() + ":4096");
        this.addressTextView.setTextColor(Color.WHITE);
        this.addressTextView.setTextSize(30);
        this.addressLayout = new FrameLayout(this);
        this.addressLayout.setBackgroundColor(Color.BLACK);
        this.addressLayout.addView(this.addressTextView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        this.addressLayout.setOnClickListener(this);
        try {
            this.nanoHTTPD.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateInputView() {
        Log.d(tag, "onCreateInputView()");
        return this.addressLayout;
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.d(tag, "handleMessage()");
        if(message.obj!=null){
            if(message.obj instanceof String) {
                this.getCurrentInputConnection().commitText((String) message.obj, 1);
            }
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        this.getCurrentInputConnection().deleteSurroundingText(1,0);
    }

    @Override
    public View onCreateCandidatesView() {
        Log.d(tag, "onCreateCandidatesView()");
        return null;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        Log.d(tag, "onStartInputView()");
        super.onStartInputView(info, restarting);
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "onDestroy()");
        super.onDestroy();
    }

    public final static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
