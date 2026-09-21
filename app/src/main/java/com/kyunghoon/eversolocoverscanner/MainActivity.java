package com.kyunghoon.eversolocoverscanner;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
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

        Uri treeUri = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                    treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        status.setText("스캔 중… 원본 파일은 수정하지 않습니다.");
        result.setText("");

        Executors.newSingleThreadExecutor().execute(() -> scan(treeUri));
    }

    private void scan(Uri treeUri) {
        Stats s = new Stats();

        try {
            String rootId = DocumentsContract.getTreeDocumentId(treeUri);
            scanDirectory(treeUri, rootId, s);
        } catch (Exception e) {
            s.fatal = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        runOnUiThread(() -> {
            if (s.fatal != null) {
                status.setText("스캔 실패");
                result.setText("Error: " + s.fatal);
                return;
            }

            status.setText("스캔 완료");
            result.setText(
                    "Audio tracks: " + s.audio + "\n" +
                    "Folders with audio: " + s.audioFolders + "\n" +
                    "External cover detected: " + s.externalCoverFolders + "\n" +
                    "Embedded artwork detected: " + s.embedded + "\n" +
                    "No cover detected: " + s.missing + "\n" +
                    "Unable to inspect embedded art: " + s.unknown + "\n\n" +
                    "※ v0.1.1은 읽기 전용입니다. 삭제/이동/태그수정/커버저장을 하지 않습니다."
            );
        });
    }

    private void scanDirectory(Uri treeUri, String parentDocumentId, Stats s) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri, parentDocumentId
        );

        boolean folderHasAudio = false;
        boolean externalCover = false;
        boolean embeddedFound = false;
        boolean unknownFound = false;
        long localAudioCount = 0;

        try (Cursor c = getContentResolver().query(
                childrenUri,
                new String[] {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null
        )) {
            if (c == null) return;

            while (c.moveToNext()) {
                String docId = c.getString(0);
                String name = c.getString(1);
                String mime = c.getString(2);

                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    scanDirectory(treeUri, docId, s);
                    continue;
                }

                String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);

                if (coverNames.contains(lower)) {
                    externalCover = true;
                }

                if (audioExt.contains(extension(lower))) {
                    folderHasAudio = true;
                    localAudioCount++;

                    if (!embeddedFound) {
                        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                        int r = hasEmbedded(fileUri);
                        if (r == 1) embeddedFound = true;
                        else if (r < 0) unknownFound = true;
                    }
                }
            }
        } catch (Exception e) {
            return;
        }

        if (!folderHasAudio) return;

        s.audioFolders++;
        s.audio += localAudioCount;

        if (externalCover) s.externalCoverFolders++;
        else if (embeddedFound) s.embedded++;
        else if (unknownFound) s.unknown++;
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
        String fatal = null;
    }
}
