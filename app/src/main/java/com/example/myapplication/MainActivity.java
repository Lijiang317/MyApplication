package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI组件
    private Button btnPlay, btnPause, btnStop,btnFavorite;

    private SeekBar seekBar;
    private TextView tvStatus, tvCurrentTime, tvTotalTime;
    private Spinner musicSpinner;

    // 适配器和列表
    private MusicAdapter musicAdapter;
    private List<String> musicNameList = new ArrayList<>();
    private List<String> musicPathList = new ArrayList<>();



    // 媒体播放器
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

    // 权限请求码
    private static final int PERMISSION_REQUEST_CODE = 100;

    // ========== 新增：SD卡权限请求码 ==========
    private static final int SD_PERMISSION_REQUEST_CODE = 200;

    // 示例网络音乐URL
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

        // 初始化RecyclerView
        initRecyclerView();

        // 检查并请求权限
        checkAndRequestPermissions();

        // 设置点击监听器
        setupClickListeners();

        // 设置SeekBar监听器
        setupSeekBarListener();

        // 初始化Spinner
        initSpinner();


        // 扫描SD卡音乐文件
        findLocalMusicFile();

        // ========== 新增：初始化SD卡保存按钮 ==========
        setupSDCardButton();
    }

    private void initViews() {
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnFavorite = findViewById(R.id.btn_favorite);
        seekBar = findViewById(R.id.seekBar);
        tvStatus = findViewById(R.id.tv_status);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        musicSpinner = findViewById(R.id.music_spinner);
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rv_music_list);
        // 设置布局管理器（让列表垂直排列）
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 在 initRecyclerView 方法中修改适配器的监听器
        musicAdapter = new MusicAdapter(musicNameList, musicPathList, new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // 1. 核心：获取点击的那首歌的绝对路径
                localMusicPath = musicPathList.get(position);

                // 2. 核心：立刻调用播放方法，让播放器进入 Prepare 状态
                playLocalMusic();

                // 3. 更新 UI，告诉用户正在加载哪首歌
                tvStatus.setText("正在加载：" + musicNameList.get(position));
            }
        });

        recyclerView.setAdapter(musicAdapter);
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
        // ========== 新增：处理SD卡权限请求结果 ==========
        else if (requestCode == SD_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                savePlaybackLogToSD();
            } else {
                Toast.makeText(this, "需要存储权限来保存日志", Toast.LENGTH_SHORT).show();
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



        //收藏按钮（连接数据库）
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { addToFavorites();}
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
    private void initSpinner() {
        // 1. 定义下拉列表的选项
        String[] items = {"本地音乐", "网络音乐", "我的收藏"};

        // 2. 创建适配器（控制文字显示样式）
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 3. 将适配器设置给 Spinner
        musicSpinner.setAdapter(adapter);

        // 4. 设置选择监听器
        musicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 本地音乐
                        tvStatus.setText("模式：播放本地音乐");
                        findLocalMusicFile(); // <--- 切换模式时重新扫描并刷新列表
                        break;
                    case 1: // 网络音乐
                        tvStatus.setText("模式：播放网络音乐");
                        // 清空本地列表显示
                        musicNameList.clear();
                        musicPathList.clear();
                        musicAdapter.notifyDataSetChanged();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void findLocalMusicFile() {
        // 1. 清空旧列表，准备装新歌
        musicNameList.clear();
        musicPathList.clear();

        // 2. 获取音乐目录
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.toLowerCase().endsWith(".mp3")) {
                        // 3. 核心：必须 add 到列表里！
                        musicNameList.add(fileName);
                        musicPathList.add(file.getAbsolutePath());
                    }
                }
            }
        }

        // 4. 关键：通知 RecyclerView 别睡了，赶紧起来刷界面
        if (musicAdapter != null) {
            musicAdapter.notifyDataSetChanged();
        }

        // 5. 更新状态文字
        if (musicNameList.isEmpty()) {
            tvStatus.setText("未找到音乐，请检查 Music 文件夹");
        } else {
            tvStatus.setText("已加载 " + musicNameList.size() + " 首歌曲");
        }
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

    private void addToFavorites(){
        // 判断当前是什么歌曲
        final String title;
        final String path;
        final String type;

        if(localMusicPath != null && !localMusicPath.isEmpty()) {
            File file = new File(localMusicPath);
            title = file.getName();
            path = localMusicPath;
            type = "local";
        }else {
            title = "网络歌曲";
            path = ONLINE_MUSIC_URL;
            type = "online";
        }

        new Thread(new Runnable() {
            public void run(){
                final boolean success = JDBCUtil.addFavorite(title, path, type);
                // 回到主线程更新 UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            Toast.makeText(MainActivity.this, "收藏成功！", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "收藏失败，请检查数据库连接", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
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

    // ========== 新增：SD卡保存日志功能（以下全部是新增代码） ==========

    private void setupSDCardButton() {
        Button btnSaveLog = findViewById(R.id.btn_save_log);
        if (btnSaveLog != null) {
            btnSaveLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkSDCardPermissions();
                }
            });
        }
    }

    private void checkSDCardPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Android 11+ 需要特殊权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestAllFilesAccessForSD();
                return;
            }
        } else {
            // Android 10及以下需要写入权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    SD_PERMISSION_REQUEST_CODE);
        } else {
            savePlaybackLogToSD();
        }
    }
    private void savePlaybackLogToSD(String musicPath, boolean isPrepared) {
        try {
            // 直接使用SD卡根路径
            String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();

            // 创建日志目录
            File logDir = new File(sdCardPath + "/Music/playmusic/logs/");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 创建日志文件
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            File logFile = new File(logDir, "playback_log_" + timestamp + ".txt");

            // 写入日志内容
            FileWriter writer = new FileWriter(logFile);
            writer.write("时间: " + timestamp + "\n");
            writer.write("音乐路径: " + musicPath + "\n");
            writer.write("准备状态: " + (isPrepared ? "已准备" : "未准备") + "\n");
            writer.flush();
            writer.close();

            // 提示用户
            Toast.makeText(this, "日志已保存到:\n" + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void savePlaybackLogToSD() {
        // 获取当前播放的音乐信息
        String currentMusicPath;
        if (localMusicPath != null && !localMusicPath.isEmpty()) {
            currentMusicPath = localMusicPath;
        } else {
            currentMusicPath = ONLINE_MUSIC_URL;
        }

        // 获取当前准备状态
        boolean currentPreparedStatus = isPrepared;

        // 调用带参数的方法
        savePlaybackLogToSD(currentMusicPath, currentPreparedStatus);
    }

    private void requestAllFilesAccessForSD() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                android.content.Intent intent = new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                android.content.Intent intent = new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
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