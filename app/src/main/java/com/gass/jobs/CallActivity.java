package com.gass.jobs;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CallActivity extends Activity {
    private static final String SUPABASE_URL="https://rwmldlqjwgtjckspqkfl.supabase.co";
    private static final String SUPABASE_KEY="sb_publishable_aYD3mQuWRYLMbqjLCDoBLA_RVdRxu4_";
    private static final String TURN_WORKER="https://tun-jobs-turn.iirc69mrgh.workers.dev";
    private String matchId, remoteId, remoteName, userId, accessToken, initialSignalId;
    private boolean video, outgoing, started=false, muted=false, cameraOff=false, finished=false;
    private JSONObject incomingOffer;
    private LinearLayout root;
    private TextView status;
    private SurfaceViewRenderer remoteView, localView;
    private EglBase egl;
    private PeerConnectionFactory factory;
    private PeerConnection peer;
    private AudioSource audioSource;
    private AudioTrack audioTrack;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private CameraVideoCapturer capturer;
    private SurfaceTextureHelper textureHelper;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<String> seen=new HashSet<>();
    private final List<IceCandidate> queuedCandidates=new ArrayList<>();
    private boolean remoteDescriptionSet=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        matchId=getIntent().getStringExtra("match_id");
        remoteId=getIntent().getStringExtra("remote_id");
        remoteName=getIntent().getStringExtra("remote_name");
        userId=getIntent().getStringExtra("user_id");
        accessToken=getIntent().getStringExtra("access_token");
        video=getIntent().getBooleanExtra("video",false);
        outgoing=getIntent().getBooleanExtra("outgoing",false);
        initialSignalId=getIntent().getStringExtra("signal_id");
        try{incomingOffer=new JSONObject(getIntent().getStringExtra("offer")==null?"{}":getIntent().getStringExtra("offer"));}catch(Exception e){incomingOffer=new JSONObject();}
        if(matchId==null||remoteId==null||userId==null||accessToken==null){finish();return;}
        buildUi();
        if(outgoing) startAfterPermission(); else showIncomingPrompt();
    }

    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(24,24,24,24);root.setBackgroundColor(Color.rgb(10,11,14));
        status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(20);status.setGravity(Gravity.CENTER);status.setText((video?"Video":"Voice")+" call • "+remoteName);root.addView(status,new LinearLayout.LayoutParams(-1,0,1));
        if(video){
            remoteView=new SurfaceViewRenderer(this);localView=new SurfaceViewRenderer(this);
            root.addView(remoteView,new LinearLayout.LayoutParams(-1,0,3));
            root.addView(localView,new LinearLayout.LayoutParams(dp(120),dp(160)));
        }
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);
        addControl(controls,"Mute",()->toggleMute());
        if(video)addControl(controls,"Camera",()->toggleCamera());
        addControl(controls,"Hang up",()->hangup(true));
        root.addView(controls,new LinearLayout.LayoutParams(-1,dp(64)));
        setContentView(root);
    }

    private void addControl(LinearLayout row,String label,Runnable action){
        TextView b=new TextView(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setGravity(Gravity.CENTER);b.setBackgroundColor(Color.rgb(45,47,54));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(4),0,dp(4),0);row.addView(b,p);b.setOnClickListener(v->action.run());
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private void setStatus(String s){runOnUiThread(()->status.setText(s));}

    private void showIncomingPrompt(){
        new AlertDialog.Builder(this).setTitle(video?"Incoming video call":"Incoming voice call")
            .setMessage(remoteName+" is calling you.")
            .setPositiveButton("Accept",(d,w)->startAfterPermission())
            .setNegativeButton("Decline",(d,w)->{sendSignal("reject",new JSONObject());finish();})
            .setCancelable(false).show();
    }

    private void startAfterPermission(){
        ArrayList<String> ask=new ArrayList<>();
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)ask.add(android.Manifest.permission.RECORD_AUDIO);
        if(video&&checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)ask.add(android.Manifest.permission.CAMERA);
        if(!ask.isEmpty()){requestPermissions(ask.toArray(new String[0]),900);return;}
        if(started)return;started=true;
        setStatus("Connecting securely…");
        new Thread(()->{try{JSONArray ice=fetchTurn();runOnUiThread(()->initializePeer(ice));}catch(Exception e){setStatus("Connection setup failed: "+e.getMessage());}}).start();
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==900){for(int r:results)if(r!=PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Microphone/camera permission is required for calls.",Toast.LENGTH_LONG).show();finish();return;}startAfterPermission();}
    }

    private JSONArray fetchTurn()throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(TURN_WORKER).openConnection();
        try{c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+accessToken);c.setRequestProperty("Content-Type","application/json");
            JSONObject body=new JSONObject();body.put("matchId",matchId);try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}
            int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String response=read(in);if(code<200||code>=300)throw new Exception("TURN service HTTP "+code+": "+response);
            return new JSONObject(response).getJSONArray("iceServers");
        }finally{c.disconnect();}
    }

    private void initializePeer(JSONArray iceJson){
        try{
            egl=EglBase.create();\n            if(video){remoteView.init(egl.getEglBaseContext(),null);remoteView.setMirror(false);localView.init(egl.getEglBaseContext(),null);localView.setMirror(true);}
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
            PeerConnectionFactory.Options options=new PeerConnectionFactory.Options();
            factory=PeerConnectionFactory.builder().setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(egl.getEglBaseContext(),true,true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(egl.getEglBaseContext())).createPeerConnectionFactory();
            List<PeerConnection.IceServer> servers=new ArrayList<>();
            for(int i=0;i<iceJson.length();i++){JSONObject x=iceJson.getJSONObject(i);JSONArray urls=x.optJSONArray("urls");if(urls==null)continue;ArrayList<String> values=new ArrayList<>();for(int j=0;j<urls.length();j++)values.add(urls.optString(j));if(values.isEmpty())continue;PeerConnection.IceServer.Builder b=PeerConnection.IceServer.builder(values);if(x.has("username"))b.setUsername(x.optString("username"));if(x.has("credential"))b.setPassword(x.optString("credential"));servers.add(b.createIceServer());}
            PeerConnection.RTCConfiguration config=new PeerConnection.RTCConfiguration(servers);
            peer=factory.createPeerConnection(config,new PeerConnection.Observer(){
                @Override public void onSignalingChange(PeerConnection.SignalingState s){}
                @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s){if(s==PeerConnection.IceConnectionState.CONNECTED||s==PeerConnection.IceConnectionState.COMPLETED)setStatus("Connected • "+remoteName);else if(s==PeerConnection.IceConnectionState.FAILED)setStatus("Connection failed");else if(s==PeerConnection.IceConnectionState.DISCONNECTED)setStatus("Reconnecting…");}
                @Override public void onIceConnectionReceivingChange(boolean b){}
                @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s){}
                @Override public void onIceCandidate(IceCandidate c){try{JSONObject p=new JSONObject();p.put("sdpMid",c.sdpMid);p.put("sdpMLineIndex",c.sdpMLineIndex);p.put("sdp",c.sdp);sendSignal("ice_candidate",p);}catch(Exception ignored){}}
                @Override public void onIceCandidatesRemoved(IceCandidate[] c){}
                @Override public void onAddStream(MediaStream stream){if(video&&stream.videoTracks.size()>0)stream.videoTracks.get(0).addSink(remoteView);}
                @Override public void onRemoveStream(MediaStream stream){}
                @Override public void onDataChannel(DataChannel c){}
                @Override public void onRenegotiationNeeded(){}
                @Override public void onAddTrack(RtpReceiver receiver,MediaStream[] streams){if(video&&receiver.track() instanceof VideoTrack)((VideoTrack)receiver.track()).addSink(remoteView);}
                @Override public void onConnectionChange(PeerConnection.PeerConnectionState s){}
                @Override public void onTrack(RtpTransceiver t){}
            });
            audioSource=factory.createAudioSource(new MediaConstraints());audioTrack=factory.createAudioTrack("tun_jobs_audio",audioSource);peer.addTrack(audioTrack,java.util.Collections.singletonList("tun_jobs_stream"));
            if(video)createCameraTrack();
            if(outgoing)createOffer();else acceptOffer();
            pollSignals();
        }catch(Exception e){setStatus("Call error: "+e.getMessage());}
    }

    private void createCameraTrack()throws Exception{
        Camera2Enumerator enumerator=new Camera2Enumerator(this);String chosen=null;
        for(String n:enumerator.getDeviceNames())if(enumerator.isFrontFacing(n)){chosen=n;break;}
        if(chosen==null)for(String n:enumerator.getDeviceNames())if(enumerator.isBackFacing(n)){chosen=n;break;}
        if(chosen==null)throw new Exception("No camera available");
        capturer=enumerator.createCapturer(chosen,null);if(capturer==null)throw new Exception("Camera unavailable");
        textureHelper=SurfaceTextureHelper.create("TunJobsCapture",egl.getEglBaseContext());videoSource=factory.createVideoSource(capturer.isScreencast());
        capturer.initialize(textureHelper,this,videoSource.getCapturerObserver());capturer.startCapture(640,480,24);
        localVideoTrack=factory.createVideoTrack("tun_jobs_video",videoSource);localVideoTrack.addSink(localView);peer.addTrack(localVideoTrack,java.util.Collections.singletonList("tun_jobs_stream"));
    }

    private void createOffer(){
        peer.createOffer(new SimpleSdp(){@Override public void onCreateSuccess(SessionDescription s){
            peer.setLocalDescription(new SimpleSdp(){@Override public void onSetSuccess(){JSONObject p=new JSONObject();try{p.put("type","offer");p.put("sdp",s.description);p.put("video",video);sendSignal("offer",p);setStatus("Calling "+remoteName+"…");}catch(Exception e){setStatus(e.getMessage());}}},s);
        }},new MediaConstraints());
    }
    private void acceptOffer(){
        try{String sdp=incomingOffer.optString("sdp","");if(sdp.isEmpty())throw new Exception("Call offer is missing");SessionDescription offer=new SessionDescription(SessionDescription.Type.OFFER,sdp);
            peer.setRemoteDescription(new SimpleSdp(){@Override public void onSetSuccess(){remoteDescriptionSet=true;flushCandidates();peer.createAnswer(new SimpleSdp(){@Override public void onCreateSuccess(SessionDescription answer){peer.setLocalDescription(new SimpleSdp(){@Override public void onSetSuccess(){try{JSONObject p=new JSONObject();p.put("type","answer");p.put("sdp",answer.description);sendSignal("answer",p);setStatus("Connecting to "+remoteName+"…");}catch(Exception e){setStatus(e.getMessage());}}},answer);}},new MediaConstraints());}},offer);
        }catch(Exception e){setStatus(e.getMessage());}
    }

    private abstract static class SimpleSdp implements SdpObserver{
        public void onCreateSuccess(SessionDescription s){} public void onSetSuccess(){}
        public void onCreateFailure(String e){} public void onSetFailure(String e){}
    }

    private void sendSignal(String type,JSONObject payload){
        new Thread(()->{try{JSONObject b=new JSONObject();b.put("match_id",matchId);b.put("sender_id",userId);b.put("recipient_id",remoteId);b.put("signal_type",type);b.put("payload",payload);rest("POST","job_call_signals",b);}catch(Exception e){setStatus("Signal error: "+e.getMessage());}}).start();
    }

    private void pollSignals(){
        if(finished)return;
        new Thread(()->{try{String path="job_call_signals?select=id,sender_id,signal_type,payload&match_id=eq."+enc(matchId)+"&recipient_id=eq."+enc(userId)+"&order=created_at.asc&limit=50";JSONArray rows=new JSONArray(rest("GET",path,null));
            for(int i=0;i<rows.length();i++){JSONObject s=rows.getJSONObject(i);String id=s.optString("id","");if(id.isEmpty()||seen.contains(id)||id.equals(initialSignalId))continue;seen.add(id);JSONObject payload=s.optJSONObject("payload");if(payload==null)payload=new JSONObject();String type=s.optString("signal_type","");
                if("answer".equals(type)){String sdp=payload.optString("sdp","");if(!sdp.isEmpty())peer.setRemoteDescription(new SimpleSdp(){@Override public void onSetSuccess(){remoteDescriptionSet=true;flushCandidates();}},new SessionDescription(SessionDescription.Type.ANSWER,sdp));}
                else if("ice_candidate".equals(type)){IceCandidate c=new IceCandidate(payload.optString("sdpMid",""),payload.optInt("sdpMLineIndex",0),payload.optString("sdp",""));if(remoteDescriptionSet)peer.addIceCandidate(c);else queuedCandidates.add(c);}
                else if("reject".equals(type)){setStatus("Call declined");finishCall(false);}
                else if("hangup".equals(type)){setStatus("Call ended");finishCall(false);}
            }
        }catch(Exception ignored){}handler.postDelayed(this::pollSignals,1800);}).start();
    }
    private void flushCandidates(){for(IceCandidate c:queuedCandidates)peer.addIceCandidate(c);queuedCandidates.clear();}
    private void toggleMute(){muted=!muted;if(audioTrack!=null)audioTrack.setEnabled(!muted);Toast.makeText(this,muted?"Microphone muted":"Microphone on",Toast.LENGTH_SHORT).show();}
    private void toggleCamera(){cameraOff=!cameraOff;if(localVideoTrack!=null)localVideoTrack.setEnabled(!cameraOff);Toast.makeText(this,cameraOff?"Camera off":"Camera on",Toast.LENGTH_SHORT).show();}
    private void hangup(boolean notify){if(notify)sendSignal("hangup",new JSONObject());finishCall(false);}
    private void finishCall(boolean ignored){finished=true;handler.removeCallbacksAndMessages(null);cleanup();finish();}
    private void cleanup(){try{if(capturer!=null){capturer.stopCapture();capturer.dispose();}}catch(Exception ignored){}try{if(peer!=null)peer.close();}catch(Exception ignored){}try{if(localVideoTrack!=null)localVideoTrack.dispose();if(videoSource!=null)videoSource.dispose();if(audioTrack!=null)audioTrack.dispose();if(audioSource!=null)audioSource.dispose();if(factory!=null)factory.dispose();if(textureHelper!=null)textureHelper.dispose();if(remoteView!=null){remoteView.release();localView.release();}if(egl!=null)egl.release();}catch(Exception ignored){}}
    @Override public void onBackPressed(){hangup(true);super.onBackPressed();}
    @Override protected void onDestroy(){finished=true;handler.removeCallbacksAndMessages(null);cleanup();super.onDestroy();}

    private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private String rest(String method,String path,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(SUPABASE_URL+"/rest/v1/"+path).openConnection();
        try{c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Authorization","Bearer "+accessToken);c.setRequestProperty("Accept","application/json");if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Prefer","return=minimal");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}}
            int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();String value=read(in);if(code<200||code>=300)throw new Exception("HTTP "+code+" "+value);return value;
        }finally{c.disconnect();}
    }
    private String read(InputStream in)throws Exception{if(in==null)return "";try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=stream.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),"UTF-8");}}
}
