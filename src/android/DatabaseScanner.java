package com.example.plugin;

import android.content.Context;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

public class DatabaseScanner extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if ("listDatabases".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                try {
                    JSONArray results = new JSONArray();
                    Context ctx = cordova.getActivity().getApplicationContext();
                    
                    // Android default databases dir & files dir
                    File[] dirsToScan = new File[] {
                        ctx.getDatabasePath("dummy").getParentFile(),
                        ctx.getFilesDir(),
                        ctx.getNoBackupFilesDir()
                    };

                    for (File dir : dirsToScan) {
                        if (dir != null && dir.exists()) {
                            scanDirectory(dir, results);
                        }
                    }

                    callbackContext.success(results);
                } catch (Exception e) {
                    callbackContext.error("Failed to scan databases: " + e.getMessage());
                }
            });
            return true;
        }
        return false;
    }

    private void scanDirectory(File directory, JSONArray results) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, results);
            } else if (isSQLiteFile(file)) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", file.getName());
                    obj.put("fullPath", file.getAbsolutePath());
                    obj.put("size", file.length());
                    results.put(obj);
                } catch (JSONException ignored) {}
            }
        }
    }

    private boolean isSQLiteFile(File file) {
        if (file.length() < 16) return false;
        byte[] buffer = new byte[16];
        try (InputStream is = new FileInputStream(file)) {
            if (is.read(buffer, 0, 16) == 16) {
                String header = new String(buffer, 0, 15);
                return "SQLite format 3".equals(header);
            }
        } catch (Exception ignored) {}
        return false;
    }
}