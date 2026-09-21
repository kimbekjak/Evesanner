package com.kyunghoon.eversolocoverscanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_STORAGE = 77;
    private TextView status, result;

    private final Set<String> audioExt = new HashSet<>(Arrays.asList(
            "flac","dsf","dff","wav","aiff","aif","mp3","m4a","aac","ogg","ape","wv"
    ));
    private final Set<String> coverNames = new HashSet<>(Arrays.asList(
            "cover.jpg","cover.jpeg","cover.png","folder.jpg","folder.jpeg","folder.png",
            "front.jpg","front.jpeg","front.png"
    ));

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        status=findViewById(R.id.status);
        result=findViewById(R.id.result);
        findViewById(R.id.select).setOnClickListener(v -> ensurePermissionAndScan());
        status.setText("A6 Music 폴더 자동탐색 준비");
    }

    private void ensurePermissionAndScan() {
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
        } else startScan();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) startScan();
    }

    private File findMusicRoot() {
        String[] candidates = {
                "/storage/E6B9-EC04/Music",
                "/mnt/media_rw/E6B9-EC04/Music",
                "/storage/usb0/Music",
                "/storage/usb1/Music",
                "/sdcard/Music"
        };
        for (String p : candidates) {
            File f = new File(p);
            if (f.isDirectory() && f.canRead()) return f;
        }
        return null;
    }

    private void startScan() {
        File root=findMusicRoot();
        if(root==null){
            status.setText("Music 폴더를 찾지 못했습니다.");
            result.setText("찾은 후보 없음: /storage/E6B9-EC04/Music 등");
            return;
        }
        status.setText("스캔 중: "+root.getAbsolutePath());
        result.setText("원본 음원은 수정하지 않습니다.");
        Executors.newSingleThreadExecutor().execute(() -> {
            Stats s=new Stats(); walk(root,s);
            runOnUiThread(() -> {
                status.setText("스캔 완료: "+root.getAbsolutePath());
                result.setText(
                        "Audio tracks: "+s.audio+"\n"+
                        "Folders with audio: "+s.audioFolders+"\n"+
                        "External cover detected: "+s.external+"\n"+
                        "Embedded artwork detected: "+s.embedded+"\n"+
                        "No cover detected: "+s.missing+"\n"+
                        "Unable to inspect embedded art: "+s.unknown+"\n\n"+
                        "v0.1.2 읽기 전용"
                );
            });
        });
    }

    private void walk(File dir, Stats s) {
        File[] fs;
        try { fs=dir.listFiles(); } catch(Exception e) { return; }
        if(fs==null) return;
        boolean hasAudio=false, extCover=false, embedded=false, unknown=false;
        long local=0;
        for(File f:fs){
            if(f.isDirectory()){ walk(f,s); continue; }
            String n=f.getName().toLowerCase(Locale.ROOT);
            if(coverNames.contains(n)) extCover=true;
            if(audioExt.contains(ext(n))){
                hasAudio=true; local++;
                if(!embedded){
                    int r=embedded(f);
                    if(r==1) embedded=true;
                    else if(r<0) unknown=true;
                }
            }
        }
        if(!hasAudio) return;
        s.audio+=local; s.audioFolders++;
        if(extCover) s.external++;
        else if(embedded) s.embedded++;
        else if(unknown) s.unknown++;
        else s.missing++;
    }

    private int embedded(File f){
        MediaMetadataRetriever m=new MediaMetadataRetriever();
        try{
            m.setDataSource(f.getAbsolutePath());
            byte[] a=m.getEmbeddedPicture();
            return a!=null && a.length>0 ? 1:0;
        }catch(Exception e){ return -1; }
        finally{ try{m.release();}catch(Exception ignored){} }
    }

    private String ext(String n){ int p=n.lastIndexOf('.'); return p<0?"":n.substring(p+1); }
    static class Stats { long audio,audioFolders,external,embedded,missing,unknown; }
}
