package com.example.myapplication;

import android.media.MediaMetadataRetriever;
import java.io.File;
import java.util.Locale;

public class MusicUtils {

    // 将毫秒转换为分钟:秒的格式
    public static String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // 获取音频文件的时长
    public static int getAudioDuration(String filePath) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filePath);
            String duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            return Integer.parseInt(duration);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 检查文件是否存在
    public static boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }
}