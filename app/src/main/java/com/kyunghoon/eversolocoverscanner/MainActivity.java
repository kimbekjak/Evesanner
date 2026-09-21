package com.kyunghoon.eversolocoverscanner;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_TREE = 1001;

    private TextView status;
    private TextView result;

    private final Set<String> audioExt = new HashSet<>(Arrays.asList(
            "flac","dsf","dff","wav","aiff","aif","mp3","m4a","aac","ogg","ape","wv"
    ));

    private final Set<String> coverNames = new HashSet<>(Arrays.asList(
            "cover.jpg","cover.jpeg","cover.png",
            "folder.jpg","folder.jpeg","folder.png",
            "front.jpg","front.jpeg","front.png"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        result = findViewById(R.id.result);

        findViewById(R.id.select).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, PICK_TREE);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_TREE || res != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        status.setText("스캔 중… 원본 파일은 수정하지 않습니다.");
        result.setText("");

        Executors.newSingleThreadExecutor().execute(() -> scan(uri));
    }

    private void scan(Uri treeUri) {
        DocumentFile root = DocumentFile.fromTreeUri(this, treeUri);
        Stats s = new Stats();

        if (root != null) walk(root, s);

        runOnUiThread(() -> {
            status.setText("스캔 완료");
            result.setText(
                    "Audio tracks: " + s.audio + "\n" +
                    "Folders with audio: " + s.audioFolders + "\n" +
                    "External cover detected: " + s.externalCoverFolders + "\n" +
                    "Embedded artwork detected: " + s.embedded + "\n" +
                    "No cover detected: " + s.missing + "\n" +
                    "Unable to inspect embedded art: " + s.unknown + "\n\n" +
                    "※ v0.1은 읽기 전용입니다. 삭제/이동/태그수정/커버저장을 하지 않습니다."
            );
        });
    }

    private void walk(DocumentFile dir, Stats s) {
        DocumentFile[] kids;
        try {
            kids = dir.listFiles();
        } catch (Exception e) {
            return;
        }

        boolean folderHasAudio = false;
        boolean external = false;
        List<DocumentFile> audios = new ArrayList<>();

        for (DocumentFile f : kids) {
            if (f.isDirectory()) {
                walk(f, s);
                continue;
            }

            String n = f.getName() == null ? "" : f.getName().toLowerCase(Locale.ROOT);

            if (coverNames.contains(n)) external = true;

            if (audioExt.contains(extension(n))) {
                audios.add(f);
                folderHasAudio = true;
            }
        }

        if (!folderHasAudio) return;

        s.audioFolders++;
        s.audio += audios.size();

        if (external) {
            s.externalCoverFolders++;
            return;
        }

        boolean anyEmbedded = false;
        boolean anyUnknown = false;

        for (DocumentFile a : audios) {
            int r = hasEmbedded(a.getUri());
            if (r == 1) {
                anyEmbedded = true;
                break;
            }
            if (r < 0) anyUnknown = true;
        }

        if (anyEmbedded) s.embedded++;
        else if (anyUnknown) s.unknown++;
        else s.missing++;
    }

    private int hasEmbedded(Uri uri) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(this, uri);
            byte[] art = mmr.getEmbeddedPicture();
            return art != null && art.length > 0 ? 1 : 0;
        } catch (Exception e) {
            return -1;
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }

    private String extension(String n) {
        int p = n.lastIndexOf('.');
        return p < 0 ? "" : n.substring(p + 1);
    }

    static class Stats {
        long audio = 0;
        long audioFolders = 0;
        long externalCoverFolders = 0;
        long embedded = 0;
        long missing = 0;
        long unknown = 0;
    }
}
