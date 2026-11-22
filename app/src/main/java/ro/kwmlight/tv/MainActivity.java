package ro.kwmlight.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.hardware.usb.UsbDevice;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private RobobloqUsb usb;
    private TextView status;
    private EditText ledCount;
    private SeekBar brightness;
    private int selected=Color.rgb(255,60,0);
    private final int[] colors={Color.RED,Color.GREEN,Color.BLUE,Color.WHITE,Color.rgb(255,120,0),Color.MAGENTA,Color.CYAN};
    private final String[] names={"Rosu","Verde","Albastru","Alb","Portocaliu","Mov","Cyan"};
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            if(RobobloqUsb.PERMISSION.equals(i.getAction())){
                UsbDevice d=usb.find();
                if(d!=null && usb.hasPermission(d))connect(); else say("Permisiunea USB nu a fost acordata");
            } else if(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED.equals(i.getAction())) {
                usb.close();say("USB deconectat");
            }
        }
    };
    @Override public void onCreate(Bundle b){super.onCreate(b);usb=new RobobloqUsb(this);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(17,22,33));
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setPadding(36,24,36,36);sc.addView(root);setContentView(sc);
        TextView title=text("KWMLIGHT Control • TCL Android TV",26);root.addView(title);
        root.addView(text("ROBOBLOQ USB HID • VID 1A86 / PID FE07",15));
        status=text("USB: neconectat",17);root.addView(status);
        button(root,"1. Detecteaza si conecteaza USB",()->{
            UsbDevice d=usb.find();if(d==null){say("Controllerul 1A86:FE07 nu este detectat");return;}
            if(!usb.hasPermission(d)){usb.requestPermission(this,d);say("Accepta permisiunea USB");}else connect();
        });
        root.addView(text("Numar total LED-uri (verifica in DX-Light pe PC):",17));
        ledCount=new EditText(this);ledCount.setInputType(2);ledCount.setSingleLine(true);
        ledCount.setHint("Obligatoriu: ex. 54");ledCount.setText(getPreferences(0).getString("leds",""));
        ledCount.setTextColor(Color.WHITE);ledCount.setHintTextColor(0xFFAAAAAA);root.addView(ledCount);
        root.addView(text("Luminozitate",17));brightness=new SeekBar(this);brightness.setMax(255);
        brightness.setProgress(getPreferences(0).getInt("brightness",128));root.addView(brightness);
        root.addView(text("Alege o culoare (apasa OK pe telecomanda):",17));
        for(int i=0;i<colors.length;i++){final int color=colors[i];button(root,names[i],()->{selected=color;apply();});}
        button(root,"Aplica din nou culoarea",this::apply);
        button(root,"Stinge LED-urile",()->runUsb(()->usb.off()));
        root.addView(text("Versiune de test. Nu sincronizeaza imaginea TV. Nu modifica firmware-ul.",14));
        IntentFilter f=new IntentFilter(RobobloqUsb.PERMISSION);
        f.addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED);
        if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver,f);
    }
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(4,12,4,12);return t;}
    private void button(LinearLayout root,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setFocusable(true);root.addView(b);b.setOnClickListener(v->r.run());}
    private void say(String s){status.setText(s);}
    private void connect(){try{usb.open(usb.find());say("USB conectat: HID interface 0");}catch(Exception e){say("Eroare: "+e.getMessage());}}
    private interface UsbJob {void run()throws Exception;}
    private void runUsb(UsbJob job){try{job.run();say("Comanda USB trimisa (verifica LED-urile)");}catch(Exception e){say("Eroare USB: "+e.getMessage());}}
    private void apply(){
        int n;try{n=Integer.parseInt(ledCount.getText().toString().trim());}catch(Exception e){say("Introdu numarul real de LED-uri");return;}
        if(n<1||n>253){say("Numarul LED-urilor trebuie sa fie intre 1 si 253");return;}
        getPreferences(0).edit().putString("leds",String.valueOf(n)).putInt("brightness",brightness.getProgress()).apply();
        final int count=n;
        runUsb(()->{
            usb.brightness(brightness.getProgress());
            // Hardware may need a short delay between brightness and colour commands.
            Thread.sleep(220);
            usb.staticColor(count,Color.red(selected),Color.green(selected),Color.blue(selected));
        });
    }
    @Override protected void onDestroy(){unregisterReceiver(receiver);usb.close();super.onDestroy();}
}
