package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI组件
    private Button btnPlay, btnPause, btnStop;
    private Button btnPlayOnline, btnPlayLocal;
    private SeekBar seekBar;
    private TextView tvStatus, tvCurrentTime, tvTotalTime;

    // 媒体播放器
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

    // 权限请求码
    private static final int PERMISSION_REQUEST_CODE = 100;

    // 示例网络音乐URL（这是一个公共的测试音频文件，可以替换为其他URL）
    private final String ONLINE_MUSIC_URL = "http://music.163.com/song/media/outer/url?id=2651447783.mp3";

    // 本地音乐文件路径（默认在SD卡根目录的Music文件夹）
    private String localMusicPath = "";

    // 当前播放状态
    private boolean isPlaying = false;
    private boolean isPrepared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI
        initViews();

        // 初始化MediaPlayer
        initMediaPlayer();

        // 检查并请求权限
        checkAndRequestPermissions();

        // 设置点击监听器
        setupClickListeners();

        // 设置SeekBar监听器
        setupSeekBarListener();

        // 扫描SD卡音乐文件
        findLocalMusicFile();
    }

    private void initViews() {
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnPlayOnline = findViewById(R.id.btn_play_online);
        btnPlayLocal = findViewById(R.id.btn_play_local);
        seekBar = findViewById(R.id.seekBar);
        tvStatus = findViewById(R.id.tv_status);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();

        // 设置准备完成的监听器
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPrepared = true;
                int duration = mediaPlayer.getDuration();
                seekBar.setMax(duration);
                tvTotalTime.setText(MusicUtils.formatTime(duration));
                tvStatus.setText("准备完成，点击播放");
            }
        });

        // 设置播放完成的监听器
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPlaying = false;
                tvStatus.setText("播放完成");
                btnPlay.setText("播放");
            }
        });

        // 设置错误监听器
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(MainActivity.this, "播放出错", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // 如果有权限需要请求
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "权限已获取", Toast.LENGTH_SHORT).show();
                findLocalMusicFile();
            } else {
                Toast.makeText(this, "需要权限来播放本地音乐", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        // 播放按钮
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    playMusic();
                } else {
                    pauseMusic();
                }
            }
        });

        // 暂停按钮
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseMusic();
            }
        });

        // 停止按钮
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMusic();
            }
        });

        // 播放网络音乐按钮
        btnPlayOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playOnlineMusic();
            }
        });

        // 播放本地音乐按钮
        btnPlayLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playLocalMusic();
            }
        });
    }

    private void setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(progress);
                }
                tvCurrentTime.setText(MusicUtils.formatTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时暂停更新
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖动时恢复更新
            }
        });
    }

    private void findLocalMusicFile() {
        // 查找SD卡中的音乐文件
        File musicDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC);

        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")
                            || fileName.endsWith(".ogg")) {
                        localMusicPath = file.getAbsolutePath();
                        tvStatus.setText("找到本地音乐: " + file.getName());
                        return;
                    }
                }
            }
        }

        // 如果Music目录没有音乐，检查根目录
        File sdCard = Environment.getExternalStorageDirectory();
        File[] allFiles = sdCard.listFiles();
        if (allFiles != null) {
            for (File file : allFiles) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".mp3")) {
                        localMusicPath = file.getAbsolutePath();
                        tvStatus.setText("找到本地音乐: " + file.getName());
                        return;
                    }
                }
            }
        }

        tvStatus.setText("未找到本地音乐文件，请将MP3文件放在SD卡根目录");
    }

    private void playMusic() {
        if (!isPrepared) {
            Toast.makeText(this, "请先选择音乐源", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
            btnPlay.setText("暂停");
            tvStatus.setText("正在播放...");

            // 开始更新进度
            startUpdatingProgress();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlay.setText("播放");
            tvStatus.setText("已暂停");
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPlaying = false;
            isPrepared = false;
            btnPlay.setText("播放");
            seekBar.setProgress(0);
            tvCurrentTime.setText("00:00");
            tvTotalTime.setText("00:00");
            tvStatus.setText("已停止");
        }
    }

    private void playOnlineMusic() {
        try {
            stopMusic(); // 先停止当前播放
            initMediaPlayer(); // 重新初始化

            mediaPlayer.setDataSource(ONLINE_MUSIC_URL);
            mediaPlayer.prepareAsync(); // 异步准备
            tvStatus.setText("正在加载网络音乐...");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "网络音乐加载失败: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void playLocalMusic() {
        if (localMusicPath.isEmpty()) {
            Toast.makeText(this, "未找到本地音乐文件", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            stopMusic(); // 先停止当前播放
            initMediaPlayer(); // 重新初始化

            mediaPlayer.setDataSource(localMusicPath);
            mediaPlayer.prepareAsync(); // 异步准备
            tvStatus.setText("正在加载本地音乐...");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "本地音乐加载失败: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startUpdatingProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    tvCurrentTime.setText(MusicUtils.formatTime(currentPosition));

                    // 每秒更新一次
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}