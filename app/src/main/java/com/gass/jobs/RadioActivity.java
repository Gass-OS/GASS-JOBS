package com.gass.jobs;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class RadioActivity extends Activity {
    static final int BG=0xff0a0b0e, PANEL=0xff191b21, GOLD=0xffffbe2d, WHITE=0xfff5f6fa, MUTED=0xffabb2be;
    static final String[] SERVERS={"https://de1.api.radio-browser.info","https://de2.api.radio-browser.info","https://nl1.api.radio-browser.info"};
    EditText search;
    LinearLayout list;
    TextView status, now;
    Button playButton, previousButton, nextButton, stopButton, exitButton;
    MediaPlayer mp;
    final ArrayList<String[]> stations=new ArrayList<>();
    int currentIndex=-1;
    boolean prepared=false;

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView txt(String s,int z,int c,boolean b){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(z); t.setTextColor(c);
        if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    GradientDrawable bg(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(stroke>0)d.setStroke(dp(1),stroke); return d;
    }
    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setTextColor(GOLD); b.setAllCaps(false);
        b.setTextSize(17); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(0,0,0,0);
        b.setBackground(bg(PANEL,13,0xff3e3728)); return b;
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(10),dp(14),dp(10)); root.setBackgroundColor(BG);

        TextView title=txt("📻  GASS-OS RADIO",21,GOLD,true); root.addView(title);
        root.addView(txt("Stations en direct • Tunisie • Europe • Monde",12,MUTED,false));

        // Always-visible Smart Island player: it stays above the station list.
        LinearLayout island=new LinearLayout(this); island.setGravity(Gravity.CENTER_VERTICAL);
        island.setPadding(dp(9),dp(7),dp(7),dp(7)); island.setBackground(bg(0xff050608,24,0xff66501f));
        LinearLayout islandInfo=new LinearLayout(this); islandInfo.setOrientation(LinearLayout.VERTICAL);
        TextView live=txt("●  RADIO EN DIRECT",9,GOLD,true); islandInfo.addView(live);
        now=txt("Choisissez une station",11,WHITE,true); now.setSingleLine(true);
        now.setEllipsize(TextUtils.TruncateAt.END); islandInfo.addView(now);
        island.addView(islandInfo,new LinearLayout.LayoutParams(0,dp(42),1));
        previousButton=btn("◀"); playButton=btn("▶"); nextButton=btn("▶|"); stopButton=btn("■"); exitButton=btn("✕");
        Button[] controls={previousButton,playButton,nextButton,stopButton,exitButton};
        String[] descriptions={"Station précédente","Lecture ou pause","Station suivante","Arrêter la lecture","Quitter la radio"};
        for(int i=0;i<controls.length;i++){
            Button b=controls[i]; b.setContentDescription(descriptions[i]);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(38),dp(40));
            p.leftMargin=dp(3); island.addView(b,p);
        }
        previousButton.setOnClickListener(v->moveStation(-1));
        nextButton.setOnClickListener(v->moveStation(1));
        playButton.setOnClickListener(v->togglePlayback());
        stopButton.setOnClickListener(v->stopPlayback());
        exitButton.setOnClickListener(v->finish());
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(58));
        ip.topMargin=dp(12); root.addView(island,ip);

        LinearLayout searchRow=new LinearLayout(this); search=new EditText(this);
        search.setSingleLine(); search.setHint("Rechercher une station"); search.setTextColor(WHITE);
        search.setHintTextColor(MUTED); search.setBackground(bg(PANEL,13,0xff3e3728));
        searchRow.addView(search,new LinearLayout.LayoutParams(0,dp(46),1));
        Button go=btn("Chercher"); go.setTextSize(13);
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(92),dp(46)); gp.leftMargin=dp(7); searchRow.addView(go,gp);
        go.setOnClickListener(v->load(search.getText().toString().trim(),""));
        search.setOnEditorActionListener((v,a,e)->{load(search.getText().toString().trim(),"");return true;});
        LinearLayout.LayoutParams srp=new LinearLayout.LayoutParams(-1,dp(46)); srp.topMargin=dp(11); root.addView(searchRow,srp);

        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this);
        String[][] countries={{"Top",""},{"🇹🇳 TN","TN"},{"🇫🇷 FR","FR"},{"🇱🇺 LU","LU"},{"🇩🇿 DZ","DZ"},{"🇲🇦 MA","MA"}};
        for(String[] c:countries){
            Button x=btn(c[0]); x.setTextSize(12);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(40)); p.rightMargin=dp(6); chips.addView(x,p);
            x.setOnClickListener(v->load("",c[1]));
        }
        hs.addView(chips); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(43));
        hp.topMargin=dp(7); root.addView(hs,hp);

        status=txt("Chargement…",12,MUTED,false); LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,-2);
        stp.topMargin=dp(5); root.addView(status,stp);
        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); load("","");
    }

    void load(String name,String country){
        status.setText("Recherche des stations…"); list.removeAllViews();
        new Thread(()->{
            ArrayList<String[]> rows=new ArrayList<>(); String error=null;
            StringBuilder q=new StringBuilder("/json/stations/search?limit=70&hidebroken=true&order=clickcount&reverse=true");
            try{
                if(!name.isEmpty())q.append("&name=").append(URLEncoder.encode(name,"UTF-8"));
                if(!country.isEmpty())q.append("&countrycode=").append(country);
            }catch(Exception ignored){}
            String body=null;
            for(String server:SERVERS)try{body=get(server+q);break;}catch(Exception e){error=e.getMessage();}
            if(body!=null)try{
                JSONArray a=new JSONArray(body);
                for(int i=0;i<a.length();i++){
                    JSONObject o=a.getJSONObject(i); String u=o.optString("url_resolved",o.optString("url",""));
                    if(u.startsWith("http"))rows.add(new String[]{o.optString("name","Station"),u,o.optString("country",""),o.optString("codec","")+" "+o.optInt("bitrate",0)+" kb/s"});
                }
            }catch(Exception e){error=e.getMessage();}
            final ArrayList<String[]> result=rows; final String err=error;
            runOnUiThread(()->show(result,err));
        }).start();
    }

    void show(ArrayList<String[]> rows,String error){
        list.removeAllViews(); stations.clear(); stations.addAll(rows);
        if(rows.isEmpty()){status.setText(error==null?"Aucune station trouvée":"Réseau indisponible : "+error); return;}
        status.setText(rows.size()+" stations disponibles • touchez pour écouter");
        for(int i=0;i<rows.size();i++){
            final int index=i; String[] station=rows.get(i);
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12),dp(10),dp(12),dp(10)); card.setBackground(bg(PANEL,13,0xff3e3728));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(6); list.addView(card,p);
            card.addView(txt("📻  "+station[0],15,WHITE,true));
            card.addView(txt(station[2]+" • "+station[3],11,MUTED,false));
            card.setOnClickListener(v->playStation(index));
        }
    }

    void playStation(int index){
        if(index<0||index>=stations.size())return;
        currentIndex=index; String[] station=stations.get(index); prepared=false;
        now.setText("Connexion : "+station[0]); playButton.setText("…"); release();
        try{
            mp=new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(station[1]);
            mp.setOnPreparedListener(player->{prepared=true;player.start();now.setText("En direct : "+station[0]);playButton.setText("Ⅱ");});
            mp.setOnErrorListener((player,what,extra)->{
                status.setText("Flux indisponible ou bloqué par le diffuseur."); release(); playButton.setText("▶"); return true;
            });
            mp.prepareAsync();
        }catch(Exception e){status.setText("Lecture impossible.");release();playButton.setText("▶");}
    }

    void moveStation(int delta){
        if(stations.isEmpty()){status.setText("Chargez d'abord une liste de stations.");return;}
        int next=currentIndex<0?(delta>0?0:stations.size()-1):(currentIndex+delta+stations.size())%stations.size();
        playStation(next);
    }
    void togglePlayback(){
        if(mp!=null&&prepared){
            if(mp.isPlaying()){mp.pause();playButton.setText("▶");}
            else{mp.start();playButton.setText("Ⅱ");}
        }else if(currentIndex>=0)playStation(currentIndex);
        else status.setText("Choisissez une station pour commencer.");
    }
    void stopPlayback(){
        release(); currentIndex=-1; now.setText("Lecture arrêtée"); playButton.setText("▶");
    }
    String get(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","GASS-JOBS/1.0");
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toString("UTF-8");
        }finally{c.disconnect();}
    }
    void release(){
        if(mp!=null){try{mp.stop();}catch(Exception ignored){}try{mp.release();}catch(Exception ignored){}mp=null;}
        prepared=false;
    }
    @Override protected void onPause(){
        if(mp!=null&&prepared&&mp.isPlaying()){mp.pause();playButton.setText("▶");}
        super.onPause();
    }
    @Override protected void onDestroy(){release();super.onDestroy();}
}
