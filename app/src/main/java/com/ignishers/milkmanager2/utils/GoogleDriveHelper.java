package com.ignishers.milkmanager2.utils;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;

/**
 * Helper class for Google Drive backup and restore operations.
 * <p>
 * Uploads a copy of the SQLite database file to the user's Google Drive
 * (inside an app-specific folder) and can restore it on demand.
 * The DB file is then replaced and the app must restart to pick it up.
 * </p>
 */
public class GoogleDriveHelper {

    private static final String TAG         = "GoogleDriveHelper";
    private static final String BACKUP_FOLDER_NAME = "MilkManager2_Backups";
    private static final String BACKUP_FILE_NAME   = "milky_mist_db.backup";
    private static final String DB_NAME            = "milky_mist_db";

    public interface DriveCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /** Build a Drive service instance authenticated as the signed-in account. */
    public static Drive buildDriveService(Context context, GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("MilkManager2")
                .build();
    }

    // ──────────────────────────────────────────────
    //  BACKUP
    // ──────────────────────────────────────────────

    /** Upload the SQLite DB to Google Drive. Runs on a background thread. */
    public static void backup(Context context, Drive driveService, DriveCallback callback) {
        new Thread(() -> {
            try {
                java.io.File dbFile = context.getDatabasePath(DB_NAME);
                if (!dbFile.exists()) {
                    callback.onFailure("Database file not found.");
                    return;
                }

                // Find or create the backup folder in Drive
                String folderId = getOrCreateFolder(driveService);

                // Check if backup already exists → delete old one
                String existingId = findFile(driveService, BACKUP_FILE_NAME, folderId);
                if (existingId != null) {
                    driveService.files().delete(existingId).execute();
                    Log.d(TAG, "Deleted old backup: " + existingId);
                }

                // Upload new backup
                File metadata = new File();
                metadata.setName(BACKUP_FILE_NAME);
                metadata.setParents(Collections.singletonList(folderId));
                metadata.setMimeType("application/octet-stream");

                com.google.api.client.http.FileContent content =
                        new com.google.api.client.http.FileContent(
                                "application/octet-stream", dbFile);

                File uploaded = driveService.files().create(metadata, content)
                        .setFields("id, name, modifiedTime")
                        .execute();

                Log.d(TAG, "Backup uploaded: " + uploaded.getId());
                callback.onSuccess("Backup successful!\nFile: " + uploaded.getName());

            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                callback.onFailure("Backup failed: " + e.getMessage());
            }
        }).start();
    }

    // ──────────────────────────────────────────────
    //  RESTORE
    // ──────────────────────────────────────────────

    /** Download the backup from Google Drive and replace local DB. Runs on background thread. */
    public static void restore(Context context, Drive driveService, DriveCallback callback) {
        new Thread(() -> {
            try {
                String folderId = getOrCreateFolder(driveService);
                String fileId   = findFile(driveService, BACKUP_FILE_NAME, folderId);

                if (fileId == null) {
                    callback.onFailure("No backup found on Google Drive.");
                    return;
                }

                // Close all DB connections before replacing the file
                java.io.File dbFile = context.getDatabasePath(DB_NAME);

                // Download to a temp file first
                java.io.File tmpFile = new java.io.File(context.getCacheDir(), "db_restore_tmp");
                try (InputStream in = driveService.files().get(fileId).executeMediaAsInputStream();
                     FileOutputStream out = new FileOutputStream(tmpFile)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }

                // Replace the live DB with the downloaded file
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                try (FileInputStream in  = new FileInputStream(tmpFile);
                     FileOutputStream out = new FileOutputStream(dbFile)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }
                tmpFile.delete();

                Log.d(TAG, "Restore complete. DB replaced at: " + dbFile.getAbsolutePath());
                callback.onSuccess("Restore successful!\nPlease restart the app to load restored data.");

            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                callback.onFailure("Restore failed: " + e.getMessage());
            }
        }).start();
    }

    // ──────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────

    /** Find or create the MilkManager2_Backups folder in Drive. Returns folder ID. */
    private static String getOrCreateFolder(Drive drive) throws Exception {
        // Search for existing folder
        FileList result = drive.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder'" +
                      " and name='" + BACKUP_FOLDER_NAME + "'" +
                      " and trashed=false")
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        // Create it
        File folder = new File();
        folder.setName(BACKUP_FOLDER_NAME);
        folder.setMimeType("application/vnd.google-apps.folder");
        File created = drive.files().create(folder).setFields("id").execute();
        return created.getId();
    }

    /** Find a file by name inside a parent folder. Returns file ID or null. */
    private static String findFile(Drive drive, String name, String folderId) throws Exception {
        FileList result = drive.files().list()
                .setQ("name='" + name + "'" +
                      " and '" + folderId + "' in parents" +
                      " and trashed=false")
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }
        return null;
    }
}
