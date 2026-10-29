package ch.piscis.nativefire;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.*;
import java.net.*;
import java.util.*;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

public class MainActivity extends Activity {
  LinearLayout root, list; EditText search; TextView status; ExoPlayer player;
  final ArrayList<Channel> channels = new ArrayList<>();
  int profile = 1;
  volatile boolean pairingActive = false;
  java.net.ServerSocket pairingServer;
  String pairingToken;
  static final int PAIR_PORT = 18743;
  static class Channel { String name,url; Channel(String n,String u){name=n;url=u;} }
  int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
  TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(dp(12),dp(12),dp(12),dp(12));return t;}
  Button button(String s){Button b=new Button(this);b.setText(s);return b;}
  @Override public void onCreate(Bundle b){super.onCreate(b);home();}
  void home(){
    root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(9,17,39));setContentView(root);
    root.addView(text("PISCIS NATIVE  •  Live-TV",24));
    LinearLayout tabs=new LinearLayout(this);root.addView(tabs);
    for(int i=1;i<=2;i++){final int p=i;Button b=button("Linie "+i);tabs.addView(b,new LinearLayout.LayoutParams(0,dp(55),1));b.setOnClickListener(v->{profile=p;load();});}
    Button settings=button("Mit Samsung einrichten (QR-Code)");root.addView(settings);settings.setOnClickListener(v->showPairing());
    search=new EditText(this);search.setSingleLine(true);search.setHint("Sender suchen");search.setHintTextColor(Color.LTGRAY);search.setTextColor(Color.WHITE);root.addView(search);
    status=text("",14);root.addView(status);
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render();}public void afterTextChanged(android.text.Editable e){}});
    load();
  }
  void editUrl(){EditText field=new EditText(this);field.setSingleLine(false);field.setMinLines(2);field.setText(getPreferences(0).getString("url"+profile,""));
    new AlertDialog.Builder(this).setTitle("M3U-URL für Linie "+profile).setMessage("Nur eigene berechtigte IPTV-Quelle verwenden. URL wird lokal auf diesem Fire TV gespeichert.").setView(field).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->{getPreferences(0).edit().putString("url"+profile,field.getText().toString().trim()).apply();load();}).show();}
  void load(){channels.clear();render();String url=getPreferences(0).getString("url"+profile,"");if(url.isEmpty()){status.setText("Linie "+profile+": QR-Code wählen und mit Samsung einrichten.");return;}
    status.setText("Linie "+profile+": Sender werden geladen …");
    new Thread(()->{ArrayList<Channel> loaded=new ArrayList<>();String error=null;
      try{URL u=new URL(url);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","NovaIPTV/0.1");
        try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))){String line;String name="";int count=0;
          while((line=r.readLine())!=null && count<200000){count++;line=line.trim();if(line.startsWith("#EXTINF")){int at=line.lastIndexOf(',');name=at>=0?line.substring(at+1).trim():"Sender";}
            else if(!line.isEmpty()&&!line.startsWith("#")){loaded.add(new Channel(name.isEmpty()?"Sender "+loaded.size():name,line));name="";}}
        }c.disconnect();
      }catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}
      String finalError=error;runOnUiThread(()->{channels.clear();channels.addAll(loaded);status.setText(finalError==null?channels.size()+" Sender geladen": "Fehler: "+finalError);render();});
    }).start();
  }
  void render(){if(list==null)return;list.removeAllViews();String q=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT);int shown=0;
    for(Channel c:channels){if(!c.name.toLowerCase(Locale.ROOT).contains(q))continue;TextView t=text("▶  "+c.name,17);list.addView(t);t.setOnClickListener(v->play(c));if(++shown>=300)break;}
    if(shown>=300)list.addView(text("Maximal 300 Treffer sichtbar. Bitte Suche verwenden.",13));
  }
  void play(Channel c){root.removeAllViews();root.addView(text(c.name,20));Button back=button("← Zurück zur Senderliste");root.addView(back);back.setOnClickListener(v->{stop();home();});
    PlayerView pv=new PlayerView(this);root.addView(pv,new LinearLayout.LayoutParams(-1,0,1));
    try{player=new ExoPlayer.Builder(this).build();pv.setPlayer(player);player.setMediaItem(MediaItem.fromUri(c.url));player.prepare();player.play();player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){runOnUiThread(()->Toast.makeText(MainActivity.this,"Streamfehler: "+error.getErrorCodeName(),Toast.LENGTH_LONG).show());}});}
    catch(Exception e){Toast.makeText(this,"Wiedergabefehler: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void showPairing(){
    stopPairing();
    root.removeAllViews();
    root.addView(text("PISCIS IPTV – Einrichtung mit Samsung",24));
    root.addView(text("Scanne den QR-Code mit dem Samsung im gleichen WLAN. Die Zugangsdaten bleiben auf deinem Fire TV.",16));
    pairingToken=UUID.randomUUID().toString().replace("-","");
    try { pairingServer=new ServerSocket(PAIR_PORT,4); pairingActive=true; }
    catch(Exception e){root.addView(text("Einrichtungsserver nicht verfügbar: "+e.getMessage(),16));return;}
    String ip="";try{Enumeration<NetworkInterface> networks=NetworkInterface.getNetworkInterfaces();while(networks.hasMoreElements()){NetworkInterface ni=networks.nextElement();Enumeration<InetAddress> ips=ni.getInetAddresses();while(ips.hasMoreElements()){InetAddress a=ips.nextElement();if(a instanceof Inet4Address && !a.isLoopbackAddress() && a.isSiteLocalAddress()){ip=a.getHostAddress();break;}}if(!ip.isEmpty())break;}}catch(Exception ignored){}
    if(ip.isEmpty()){root.addView(text("Keine WLAN-IP gefunden.",16));stopPairing();return;}
    String url="http://"+ip+":"+PAIR_PORT+"/?key="+pairingToken;
    ImageView qr=new ImageView(this);
    try{BitMatrix matrix=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,350,350);Bitmap bmp=Bitmap.createBitmap(350,350,Bitmap.Config.ARGB_8888);for(int y=0;y<350;y++)for(int x=0;x<350;x++)bmp.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);qr.setImageBitmap(bmp);root.addView(qr,new LinearLayout.LayoutParams(dp(350),dp(350)));}catch(Exception e){root.addView(text("QR-Fehler: "+e.getMessage(),16));}
    root.addView(text("Alternativ auf dem Samsung öffnen: "+url,14));
    Button back=button("Zurück zur Senderliste");root.addView(back);back.setOnClickListener(v->{stopPairing();home();});
    new Thread(()->{while(pairingActive){try{java.net.Socket socket=pairingServer.accept();new Thread(()->handlePairClient(socket)).start();}catch(Exception ignored){if(!pairingActive)break;}}}).start();
  }
  void handlePairClient(java.net.Socket socket){
    try(socket){
      socket.setSoTimeout(10000);
      BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
      String first=in.readLine();if(first==null)return;
      String[] req=first.split(" ");if(req.length<2)return;
      String method=req[0],path=req[1];
      int size=0;String line;
      while((line=in.readLine())!=null&&!line.isEmpty()){if(line.toLowerCase(Locale.ROOT).startsWith("content-length:"))size=Integer.parseInt(line.substring(15).trim());}
      if(!path.contains("key="+pairingToken)){sendPairResponse(socket,403,"Zugriff verweigert");return;}
      if("GET".equals(method)){
        String page="<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><meta charset='UTF-8'><title>Piscis IPTV</title><style>body{font-family:Arial;background:#0b1728;color:white;padding:20px}input,button{width:100%;padding:14px;margin:9px 0;box-sizing:border-box;font-size:18px;border-radius:9px}button{background:#0b9f84;color:white}</style></head><body><h2>Piscis IPTV – Fire TV verbinden</h2><p>Die Daten gehen nur an deinen Fire TV im WLAN.</p><form method='POST' action='/?key="+pairingToken+"'><label>Serveradresse</label><input name='host' required placeholder='http://anbieter.example'><label>Benutzername</label><input name='user' required><label>Passwort</label><input type='password' name='pass' required><button>Auf Fire TV speichern</button></form></body></html>";
        sendPairResponse(socket,200,page);return;
      }
      if("POST".equals(method)&&size>0&&size<8192){
        char[] chars=new char[size];int read=0;while(read<size){int n=in.read(chars,read,size-read);if(n<0)break;read+=n;}
        HashMap<String,String> form=new HashMap<>();for(String part:new String(chars,0,read).split("&")){String[] v=part.split("=",2);if(v.length==2)form.put(v[0],java.net.URLDecoder.decode(v[1],"UTF-8"));}
        String host=form.getOrDefault("host","").trim().replaceAll("/+$","");
        String user=form.getOrDefault("user","").trim();
        String pass=form.getOrDefault("pass","");
        if(!(host.startsWith("http://")||host.startsWith("https://"))||user.isEmpty()||pass.isEmpty()){sendPairResponse(socket,400,"Ungültige Angaben");return;}
        String playlist=host+"/get.php?username="+java.net.URLEncoder.encode(user,"UTF-8")+"&password="+java.net.URLEncoder.encode(pass,"UTF-8")+"&type=m3u_plus&output=ts";
        getPreferences(0).edit().putString("url"+profile,playlist).apply();
        sendPairResponse(socket,200,"<html><body><h2>Gespeichert!</h2><p>Die Fire-TV-App lädt jetzt deine Sender. Du kannst diese Seite schliessen.</p></body></html>");
        pairingActive=false;
        runOnUiThread(()->{stopPairing();home();});
      } else sendPairResponse(socket,405,"Nicht unterstützt");
    }catch(Exception ignored){}
  }
  void sendPairResponse(java.net.Socket socket,int code,String body)throws Exception{
    byte[] content=body.getBytes("UTF-8");
    java.io.OutputStream out=socket.getOutputStream();
    out.write(("HTTP/1.1 "+code+" "+(code==200?"OK":"Error")+"\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'\r\nContent-Length: "+content.length+"\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));out.write(content);out.flush();
  }
  void stopPairing(){pairingActive=false;if(pairingServer!=null){try{pairingServer.close();}catch(Exception ignored){}pairingServer=null;}}
  void stop(){if(player!=null){player.release();player=null;}}
  @Override public void onBackPressed(){if(player!=null){stop();home();}else super.onBackPressed();}
  @Override protected void onDestroy(){stopPairing();stop();super.onDestroy();}
}
