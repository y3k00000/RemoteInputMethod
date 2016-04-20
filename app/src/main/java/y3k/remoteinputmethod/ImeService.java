package y3k.remoteinputmethod;

import android.graphics.Color;
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

    // 這個Sample參考http://developer.android.com/guide/topics/text/creating-input-method.html的內容製作
    // 基本框架具備，細節很多待修Q_Q

    @Override
    public void onCreate() {
        Log.d(tag, "onCreate()");
        super.onCreate();
        // NanoHttpd套件，帶port給他並override serve()這個function，就有一個簡單的Http Service
        this.nanoHTTPD = new NanoHTTPD(4096) {
            @Override
            // 在這個範例，只要裝置從4096 port收到Http的呼叫，像這樣 http://192.168.1.56:4096/
            public Response serve(IHTTPSession session) {
                // 在這個範例，只要裝置從4096 port收到Http的呼叫，像這樣 http://192.168.1.56:4096/?text=長茂
                // 就會發一個Message給Handler。
                if(session.getParms()!=null) {
                    String text = session.getParms().get("text");
                    if(text!=null){
                        Log.d(tag, "NanoHTTPD got text = " + text);
                        Message message = new Message();
                        message.obj = text;
                        handler.sendMessage(message);
                    }
                }
                return newFixedLengthResponse("ok");
            }
        };
        // 準備好要顯示的Layout內容...
        this.addressTextView = new TextView(this);
        this.addressTextView.setText("http://" + getLocalIpAddress() + ":4096");
        this.addressTextView.setTextColor(Color.WHITE);
        this.addressTextView.setTextSize(30);
        this.addressLayout = new FrameLayout(this);
        this.addressLayout.setBackgroundColor(Color.BLACK);
        this.addressLayout.addView(this.addressTextView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        // 這裡有掛了一個OnClickListener在this上，目前用來測試Backspace的效果。
        this.addressLayout.setOnClickListener(this);
        // 要呼叫NanoHttpd的start()，才能正式啟動他
        try {
            this.nanoHTTPD.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 當使用者focus到需要輸入法的地方，系統就會把指定的輸入法onCreate()後再把這個function return的View放上來(一般就是鍵盤)
    // 要注意onCreate()會被呼叫一次，之後輸入法不管有無使用都會在背景再活一陣子；所以這個onCreateInputView()在整個過程中
    // 會被呼叫很多次，每次被叫下去或叫出來都會呼叫一次。
    @Override
    public View onCreateInputView() {
        Log.d(tag, "onCreateInputView()");
        return this.addressLayout;
    }

    // Handler的callback在這裡，從NanoHttpd收到text的字串後，就在這裡getCurrentInputConnection().commitText()把字串丟出去
    // 有時候會發生非預期的NullPointerException，待調查原因；同學有發現歡迎交流一下XD
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

    // 這個應該是顯示選字的View，給null不會錯，就這樣了。
    @Override
    public View onCreateCandidatesView() {
        Log.d(tag, "onCreateCandidatesView()");
        return null;
    }

    // 這個會在info給一些資訊進來，目前沒用到只呼叫super而已。
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

    // 從NetworkInterface找出local ip，如果找不到就直接爆炸，因為是測試所以沒關係XD
    public final static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
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
