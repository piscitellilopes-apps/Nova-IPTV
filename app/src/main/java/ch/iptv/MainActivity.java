package ch.nova.iptv;

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

public class MainActivity extends Activity {
  LinearLayout root, list; EditText search; TextView status; ExoPlayer player;
  final ArrayList<Channel> channels = new ArrayList<>();
  int profile = 1;
  static class Channel { String name,url; Channel(String n,String u){name=n;url=u;} }
  int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
  TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(dp(12),dp(12),dp(12),dp(12));return t;}
  Button button(String s){Button b=new Button(this);b.setText(s);return b;}
  @Override public void onCreate(Bundle b){super.onCreate(b);home();}
  void home(){
    root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(9,17,39));setContentView(root);
    root.addView(text("NOVA IPTV  •  Live-TV",24));
    LinearLayout tabs=new LinearLayout(this);root.addView(tabs);
    for(int i=1;i<=2;i++){final int p=i;Button b=button("Linie "+i);tabs.addView(b,new LinearLayout.LayoutParams(0,dp(55),1));b.setOnClickListener(v->{profile=p;load();});}
    Button settings=button("IPTV-Linie einrichten / ändern");root.addView(settings);settings.setOnClickListener(v->editUrl());
    search=new EditText(this);search.setSingleLine(true);search.setHint("Sender suchen");search.setHintTextColor(Color.LTGRAY);search.setTextColor(Color.WHITE);root.addView(search);
    status=text("",14);root.addView(status);
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render();}public void afterTextChanged(android.text.Editable e){}});
    load();
  }
  void editUrl(){EditText field=new EditText(this);field.setSingleLine(false);field.setMinLines(2);field.setText(getPreferences(0).getString("url"+profile,""));
    new AlertDialog.Builder(this).setTitle("M3U-URL für Linie "+profile).setMessage("Nur eigene berechtigte IPTV-Quelle verwenden. URL wird lokal auf dem Handy gespeichert.").setView(field).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->{getPreferences(0).edit().putString("url"+profile,field.getText().toString().trim()).apply();load();}).show();}
  void load(){channels.clear();render();String url=getPreferences(0).getString("url"+profile,"");if(url.isEmpty()){status.setText("Linie "+profile+": Bitte M3U-Adresse einrichten.");return;}
    status.setText("Linie "+profile+": Sender werden geladen …");
    new Thread(()->{ArrayList<Channel> loaded=new ArrayList<>();String error=null;
      try{URL u=new URL(url);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","NovaIPTV/0.1");
        try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))){String line;String name="";int count=0;
          while((line=r.readLine())!=null && count<30000){count++;line=line.trim();if(line.startsWith("#EXTINF")){int at=line.lastIndexOf(',');name=at>=0?line.substring(at+1).trim():"Sender";}
            else if(!line.isEmpty()&&!line.startsWith("#")){loaded.add(new Channel(name.isEmpty()?"Sender "+loaded.size():name,line));name="";}}
        }c.disconnect();
      }catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}
      String finalError=error;runOnUiThread(()->{channels.clear();channels.addAll(loaded);status.setText(finalError==null?channels.size()+" Sender geladen": "Fehler: "+finalError);render();});
    }).start();
  }
  void render(){if(list==null)return;list.removeAllViews();String q=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT);int shown=0;
    for(Channel c:channels){if(!c.name.toLowerCase(Locale.ROOT).contains(q))continue;TextView t=text("▶  "+c.name,17);list.addView(t);t.setOnClickListener(v->play(c));if(++shown>=300)break;}
    if(channels.size()>300)list.addView(text("Maximal 300 Treffer sichtbar. Bitte Suche verwenden.",13));
  }
  void play(Channel c){root.removeAllViews();root.addView(text(c.name,20));Button back=button("← Zurück zur Senderliste");root.addView(back);back.setOnClickListener(v->{stop();home();});
    PlayerView pv=new PlayerView(this);root.addView(pv,new LinearLayout.LayoutParams(-1,0,1));
    try{player=new ExoPlayer.Builder(this).build();pv.setPlayer(player);player.setMediaItem(MediaItem.fromUri(c.url));player.prepare();player.play();}
    catch(Exception e){Toast.makeText(this,"Wiedergabefehler: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void stop(){if(player!=null){player.release();player=null;}}
  @Override public void onBackPressed(){if(player!=null){stop();home();}else super.onBackPressed();}
  @Override protected void onDestroy(){stop();super.onDestroy();}
}
