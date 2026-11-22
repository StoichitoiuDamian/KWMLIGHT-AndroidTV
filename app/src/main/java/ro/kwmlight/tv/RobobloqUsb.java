package ro.kwmlight.tv;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.*;
import java.util.Arrays;

/** USB HID interface 0 only; no firmware writes. */
public final class RobobloqUsb {
    public static final int VID=0x1A86, PID=0xFE07;
    public static final String PERMISSION="ro.kwmlight.tv.USB_PERMISSION";
    private final UsbManager manager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbInterface lighting;
    private UsbEndpoint output;
    private int seq=1;

    public RobobloqUsb(Context context) { manager=(UsbManager)context.getSystemService(Context.USB_SERVICE); }
    public UsbDevice find() {
        for(UsbDevice d: manager.getDeviceList().values()) if(d.getVendorId()==VID && d.getProductId()==PID) return d;
        return null;
    }
    public boolean hasPermission(UsbDevice d) { return manager.hasPermission(d); }
    public void requestPermission(Context context, UsbDevice d) {
        PendingIntent pi=PendingIntent.getBroadcast(context,0,new Intent(PERMISSION).setPackage(context.getPackageName()),PendingIntent.FLAG_IMMUTABLE);
        manager.requestPermission(d,pi);
    }
    public synchronized void open(UsbDevice d) throws Exception {
        close();
        if(d==null || d.getVendorId()!=VID || d.getProductId()!=PID) throw new Exception("Dispozitiv USB incorect");
        if(!manager.hasPermission(d)) throw new Exception("Permisiunea USB lipseste");
        // Never claim interface 1 (keyboard).
        for(int i=0;i<d.getInterfaceCount();i++) {
            UsbInterface it=d.getInterface(i);
            if(it.getId()==0 && it.getInterfaceClass()==UsbConstants.USB_CLASS_HID) { lighting=it; break; }
        }
        if(lighting==null) throw new Exception("Interfata HID 0 nu exista");
        connection=manager.openDevice(d);
        if(connection==null || !connection.claimInterface(lighting,true)) { close(); throw new Exception("Nu pot deschide interfata HID 0"); }
        for(int i=0;i<lighting.getEndpointCount();i++) {
            UsbEndpoint e=lighting.getEndpoint(i);
            if(e.getDirection()==UsbConstants.USB_DIR_OUT && e.getType()==UsbConstants.USB_ENDPOINT_XFER_INT) output=e;
        }
        device=d;
    }
    public synchronized void close() {
        if(connection!=null) { if(lighting!=null) connection.releaseInterface(lighting); connection.close(); }
        connection=null; lighting=null; output=null; device=null;
    }
    public synchronized boolean isOpen(){return connection!=null;}
    private int nextSeq() { int s=seq++; if(seq>254)seq=1; return s; }
    /** RB frame: 'R','B',len,seq,command,payload,checksum. */
    public static byte[] frame(int seq,int cmd,byte... payload) {
        int n=6+payload.length;
        if(n>64) throw new IllegalArgumentException("Pachet prea mare");
        byte[] p=new byte[n]; p[0]='R';p[1]='B';p[2]=(byte)n;p[3]=(byte)seq;p[4]=(byte)cmd;
        System.arraycopy(payload,0,p,5,payload.length);
        int sum=0;for(int i=0;i<n-1;i++)sum+=p[i]&255;
        p[n-1]=(byte)sum;
        return p;
    }
    private void send(byte[] packet) throws Exception {
        if(connection==null)throw new Exception("USB neconectat");
        byte[] report=Arrays.copyOf(packet,64); // Report ID 0 is implicit in Android HID SET_REPORT.
        int written;
        if(output!=null) written=connection.bulkTransfer(output,report,report.length,1500);
        else written=connection.controlTransfer(0x21,0x09,0x0200,lighting.getId(),report,report.length,1500);
        if(written!=64) throw new Exception("Trimitere HID esuata: "+written+" bytes");
    }
    public synchronized void brightness(int value) throws Exception {
        send(frame(nextSeq(),0x87,(byte)Math.max(5,Math.min(255,value))));
    }
    public synchronized void staticColor(int ledCount,int r,int g,int b) throws Exception {
        if(ledCount<1 || ledCount>254)throw new Exception("Numar LED invalid (1-254)");
        // Same range format as DX-Light: [1,R,G,B,last, last+1,0,0,0,254].
        // Compression caps total RGB intensity to 255 to match DX-Light USB power limit.
        int sum=r+g+b;
        if(sum>255){r=r*255/sum;g=g*255/sum;b=b*255/sum;}
        send(frame(nextSeq(),0x86,(byte)1,(byte)r,(byte)g,(byte)b,(byte)ledCount,
                (byte)(ledCount+1),(byte)0,(byte)0,(byte)0,(byte)254));
    }
    public synchronized void off() throws Exception {send(frame(nextSeq(),0x97));}
}
