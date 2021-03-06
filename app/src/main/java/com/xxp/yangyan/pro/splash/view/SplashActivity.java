package com.xxp.yangyan.pro.splash.view;

import android.Manifest;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.kymjs.rxvolley.RxVolley;
import com.kymjs.rxvolley.client.HttpCallback;
import com.orhanobut.logger.Logger;
import com.tapadoo.alerter.Alerter;
import com.xxp.yangyan.R;
import com.xxp.yangyan.mvp.view.MvpLceView;
import com.xxp.yangyan.pro.MainActivity;
import com.xxp.yangyan.pro.base.BaseActivity;
import com.xxp.yangyan.pro.entity.SplashInfo;
import com.xxp.yangyan.pro.listener.RequestPermisListener;
import com.xxp.yangyan.pro.splash.presenter.Presenter;
import com.xxp.yangyan.pro.utils.ActivityManager;
import com.xxp.yangyan.pro.utils.IOUtils;
import com.xxp.yangyan.pro.utils.JudgeUtils;
import com.xxp.yangyan.pro.utils.SettingUtils;
import com.xxp.yangyan.pro.utils.ToastUtils;
import com.xxp.yangyan.pro.utils.UIUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;


public class SplashActivity extends BaseActivity<Presenter>
        implements Animation.AnimationListener, MvpLceView<SplashInfo> {

    //倒计时
    @BindView(R.id.tv_time)
    TextView tv_time;
    @BindView(R.id.activity_splash)
    View splashView;
    private Timer timer;
    //Splash的背景的路径
    private String splashImgPath;
    //设置壁纸
    private boolean isFristOnClick = true;

    private long exitTime;
    //启动图片的存储名字
    private final String SPLASH_IMG_NAME = "bg_splash.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityManager.addActivity(this);
        initView();
        startAnim();
        loadBackground();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    private void initView() {
        //全屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    /**
     * 设置为壁纸
     */
    @OnClick(R.id.activity_splash)
    void setWallPaper() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            exitTime = System.currentTimeMillis();
        } else {
            if (isFristOnClick) {
                isFristOnClick = false;
                //设置壁纸
                SettingUtils.setWallpaper(UIUtils.DrawableToBitmap(splashView.getBackground()));

            }
        }

    }


    private void loadBackground() {
        splashImgPath = IOUtils.getSDPath() + SPLASH_IMG_NAME;
        setDownloadImage();
        presenter.loadData();
    }


    //开始动画
    private void startAnim() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(2000);
        splashView.setAnimation(alphaAnimation);

        ScaleAnimation scaleAnimation = new ScaleAnimation(3.5f, 1, 3.5f, 1,
                UIUtils.getWidthAndHeight()[0] / 2,
                UIUtils.getWidthAndHeight()[1] / 2);
        scaleAnimation.setDuration(1000);
        scaleAnimation.setAnimationListener(this);
        splashView.setAnimation(scaleAnimation);
    }


    //倒计时时间开始
    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            private int time = 9;

            @Override
            public void run() {
                UIUtils.onUIThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_time.setText(time-- + "");
                        if (time == -1) {
                            goMain();
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    public void onAnimationStart(Animation animation) {
        startTimer();
    }

    @Override
    public void onAnimationEnd(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    //设置下载的壁纸
    private void setDownloadImage() {
        Drawable drawable = getLocalImage();
        if (drawable != null) {
            splashView.setBackground(drawable);
        } else {
            setDefaultBackground();
        }
    }

    //下载新的壁纸
    private void downloadNewImage(final String path, final String url) {
        Logger.e("开始下载新的图片");
        requestPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new RequestPermisListener() {
            @Override
            public void onGranted() {
                RxVolley.download(path, url, null, new HttpCallback() {
                    @Override
                    public void onSuccess(String t) {
                        super.onSuccess(t);
                        Logger.e("下载成功");
                    }
                });
            }

            @Override
            public void onDenide(List<String> permissions) {
                ToastUtils.showLongToast(getString(R.string.permission_error_sd));
            }
        });

    }

    //返回本地的存储的Splash的背景图片
    private Drawable getLocalImage() {
        return Drawable.createFromPath(splashImgPath);
    }

    //设置默认的壁纸
    private void setDefaultBackground() {
        splashView.setBackground(UIUtils.getDrawable(R.mipmap.bg_splash));
    }


    //到主界面
    private void goMain() {
        startActivity(new Intent(UIUtils.getContext(), MainActivity.class));
        finish();
        timer.cancel();
    }


    @Override
    public void showLoading(boolean type) {

    }

    @Override
    public void showContent() {

    }

    @Override
    public void showError(Throwable throwable) {
        //如果网络访问失败设置当前储存卡的壁纸
        setDownloadImage();
        Alerter
                .create(this)
                .setBackgroundColor(R.color.colorPrimary)
                .setDuration(3000)
                .setTitle(R.string.load_splash_img_error)
                .show();
    }

    @Override
    public void showData(SplashInfo data) {
        if (JudgeUtils.isSplashImgUpdate(data.getDate())) {
            Logger.e("图片更换了");
            //图片跟换了
            downloadNewImage(splashImgPath, data.getSplashUrl());
        } else setDownloadImage();
    }


    @Override
    protected Presenter bindPresenter() {
        return new Presenter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        ActivityManager.removeActivity(this);
    }
}
