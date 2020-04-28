package com.cheng.xing.shoppinglist.utils;

import com.cheng.xing.shoppinglist.R;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.WriteMode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DropBoxUtils {
    private String access_token;
    private String file_name;
    private String file_path;
    private DbxClientV2 client;

    public DropBoxUtils(String access_token, String file_name, String file_path){
        this.access_token = access_token;
        this.file_name = file_name;
        this.file_path = file_path;
        client = new DbxClientV2(DbxRequestConfig.newBuilder("shopping-list").build(),
                access_token);
    }
    public void downloadLatestFile() {
        try (FileOutputStream out = new FileOutputStream(
                file_path + "/" + file_name)){
            DbxDownloader<FileMetadata> downloader = client.files().download("/" + file_name);
            downloader.download(out);
        } catch (DbxException | IOException ex) {
            //To do
        }
    }

    public void uploadToDropBox() {
        try (InputStream in = new FileInputStream(file_path + "/" + file_name)) {
            FileMetadata metadata = client.files().uploadBuilder("/" + file_name).withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        } catch (IOException | DbxException e) {
            // To do
        }
    }
}
