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
    private String purchased_file_name;
    private String shop_file_name;
    private String filePath;
    private DbxClientV2 client;

    public DropBoxUtils(String filePath, String access_token, String[] fileNames){
        this.access_token = access_token;
        this.filePath = filePath;
        if(fileNames.length >= 1){
            this.file_name = fileNames[0];
        }
        if(fileNames.length >= 2){
            this.purchased_file_name = fileNames[1];
        }
        if(fileNames.length >= 3){
            this.shop_file_name = fileNames[2];
        }
        client = new DbxClientV2(DbxRequestConfig.newBuilder("shopping-list").build(),
                access_token);
    }
    public void downloadLatestFile() {
        try (FileOutputStream out = new FileOutputStream(
                filePath + "/" + file_name)){
            DbxDownloader<FileMetadata> downloader = client.files().download("/" + file_name);
            downloader.download(out);
        } catch (DbxException | IOException ex) {
            //To do
        }
    }

    public void uploadToDropBox() {
        try (InputStream in = new FileInputStream(filePath + "/" + file_name)) {
            FileMetadata metadata = client.files().uploadBuilder("/" + file_name).withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        } catch (IOException | DbxException e) {
            // To do
        }
    }

    public void downloadLatestPurchasedFile() {
        try (FileOutputStream out = new FileOutputStream(
                filePath + "/" + purchased_file_name)){
            DbxDownloader<FileMetadata> downloader = client.files().download("/" + purchased_file_name);
            downloader.download(out);
        } catch (DbxException | IOException ex) {
            //To do
        }
    }

    public void uploadPurchasedToDropBox() {
        try (InputStream in = new FileInputStream(filePath + "/" + purchased_file_name)) {
            FileMetadata metadata = client.files().uploadBuilder("/" + purchased_file_name).withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        } catch (IOException | DbxException e) {
            // To do
        }
    }
}
