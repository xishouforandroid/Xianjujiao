package xidian.xianjujiao.com.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsoluteLayout;

import com.google.gson.Gson;

import com.software.shell.fab.ActionButton;

import com.squareup.picasso.Picasso;
import com.umeng.socialize.Config;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fm.jiecao.jcvideoplayer_lib.JCUtils;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import xidian.xianjujiao.com.R;
import xidian.xianjujiao.com.entity.NewsDetailData;
import xidian.xianjujiao.com.utils.API;
import xidian.xianjujiao.com.utils.Constant;
import xidian.xianjujiao.com.utils.DateUtils;
import xidian.xianjujiao.com.utils.JsonUtils;
import xidian.xianjujiao.com.utils.SystemBarTintManager;
import xidian.xianjujiao.com.utils.ToastUtil;
import xidian.xianjujiao.com.utils.UiUtils;

/**
 * 文章详情界面
 */
public class VideoNewsDetailActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "NewsDetailActivity";
    private WebView comment_web;
    private Toolbar toolbar;
    private String body;
    private String id;
    private String newsId;
    private String title;//标题
    private String writer;//作者
    private String senddate;//发布时间
    private SmoothProgressBar webProgress;//进度条
    private ActionButton actionButton;//评论按钮
    private String audioUrl;
    private String videoUrl;

    final SHARE_MEDIA[] displaylist = new SHARE_MEDIA[]
            {
                    SHARE_MEDIA.WEIXIN, SHARE_MEDIA.WEIXIN_CIRCLE,
                    SHARE_MEDIA.QQ, SHARE_MEDIA.QZONE
            };
    private String decode;
//    private String arcurl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);
        initWindow();
        initView();
//        id = getIntent().getStringExtra("type");
        newsId = getIntent().getStringExtra("newsId");

        Log.e(TAG,"newsId="+newsId);
        String url = String.format(API.NEWS_DETAIL_URL, newsId);
        Log.e(TAG,url);
        //下载网络数据
        x.http().get(new RequestParams(url), new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                String json = new String(result);
                Log.e(TAG,json);
//                //json解析
                NewsDetailData detail = new Gson().fromJson(JsonUtils.removeBOM(json), NewsDetailData.class);
                body = detail.data.content;//文章内容
                title = detail.data.title;//文章标题
                writer = "张三";//文章作者
                senddate = detail.data.create_time;//文章发布时间
                audioUrl = detail.data.audio;
                videoUrl = detail.data.video;
//                arcurl = detail.getArcurl();
                initData();
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {

            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }
        });
        initListener();
    }



    @Override
    protected void onResume() {
        super.onResume();
//        MobclickAgent.onPause(this);
    }

    //初始化窗体布局
    private void initWindow() {
        SystemBarTintManager tintManager;
        //由于沉浸式状态栏需要在Android4.4.4以上才能使用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorBackground));
            tintManager.setStatusBarTintEnabled(true);
        }
    }


    //获取控件
    private void initView() {
        comment_web = (WebView) findViewById(R.id.coment_web);
        actionButton = (ActionButton) findViewById(R.id.action_button);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        webProgress = (SmoothProgressBar) findViewById(R.id.web_progress);
        //2.替代
        setSupportActionBar(toolbar);
        //加载背景颜色
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorBackground)));
        //设置显示返回上一级的图标
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //设置标题
        getSupportActionBar().setTitle("文章详情");
        //设置标题栏字体颜色
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
        //设置悬浮按钮的背景图片
        actionButton.setImageResource(R.drawable.note_publish_img_unpressed);//设置按钮资源文件
        actionButton.setImageSize(65);//设置图片按钮的大小
        //修改友盟分享对话框
        ProgressDialog dialog =  new ProgressDialog(this);
        dialog.setMessage("分享中...");
        Config.dialog = dialog;

    }


    //初始化数据
    private void initData() {
        //启用支持javascript
        WebSettings settings = comment_web.getSettings();
        settings.setJavaScriptEnabled(true);
//        settings.setAllowFileAccess(true);

//        settings.setLoadWithOverviewMode(true);
        // by ROM
//        settings.setTextSize(WebSettings.TextSize.LARGER);//设置字体大小
//        settings.setDefaultTextEncodingName("utf-8");//设置默认编码格式
//        //自适应屏幕
//        settings.setUseWideViewPort(true);
        comment_web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                webProgress.setProgress(newProgress);
                if (webProgress != null && newProgress != 100) {
                    webProgress.setVisibility(View.VISIBLE);
                } else if (webProgress != null) {
                    webProgress.setVisibility(View.GONE);
                }
            }
        });

        comment_web.addJavascriptInterface(new JCCallBack(), "jcvd");
//        comment_web.loadUrl("file:///android_asset/jcvd.html");

        //加载网络资源
        if (body != null) {
            try {
                //由于body的数据进行了URLEncode编码，所以需要我们再进行URLDecoder解码
                //否则只能显示图片
                decode = URLDecoder.decode(body, "utf-8");
                Log.e("result", "" + decode);
                String date = DateUtils.dateFormat(senddate);//发布时间
                String html2 = "<!DOCTYPE html>" +
                        "<html>" +
                        "<body>" +
                        "<h2 style=\"width:100%;height:50px;text-align:center\" >" +title+
                        "</h2>" +
                        "<p style=\"width:100%;height:20px;\">" +
                        "作者:"+writer+"&nbsp;&nbsp;"+"发布时间" + date+
                        "</p>" +

                        "<div id=\"cont\" style=\"width:100%;height:200px\">" +
                        "<p style=\"width:100%;height:200px;\">" +

                        "<style>img{width:100%;height:auto}</style>" +
                        body+
                        "<script>" +
                        "    var cont=document.getElementById(\"cont\");" +
                        "    jcvd.adViewJieCaoVideoPlayer(-1,200,120,0,0)" +
                        "</script>" +
                        "</body>" +
                        "</html>";
//                String html = "<html><body>"
//                        + "<h3>"
//                        + title
//                        + "</h3>"
//                        + "<p>"
//                        + "作者:" + writer
//                        + "&nbsp&nbsp"
//                        + "发布时间:" + date
//                        + "</p>"
//                        + "<style>"
//                        + "img{width:100%;height:auto;}"//自定义样式，设置图片显示大小
//                        + "</style>"
//                        + decode
//                        + "</body></html>";
//                //使用这种方法，前面添加网站的地址 http://www.3dmgame.com，可以解决，有些图片前面乜有完整请求地址的问题
////                String video_url = "http://toutiao.com/group/6301854282790470146/";
                // 加载url
//                comment_web.loadUrl(html2);



                comment_web.loadDataWithBaseURL(null, html2, "text/html", "charset=UTF-8", null);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }

    }

    //改写物理按键——返回的逻辑
    //返回无效是重定向的原因
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (comment_web != null && comment_web.canGoBack()) {
                comment_web.canGoBack();
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);

    }

    //设置事件监听
    private void initListener() {
        //toolbard的返回按钮事件监听
        toolbar.setNavigationOnClickListener(this);
        //点击按钮跳转到评论界面
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//               Intent intent = new Intent(ArticleDetailActivity.this, CommentActivity.class);
//                Bundle bundle = new Bundle();
//                bundle.putString("id", id);
//                bundle.putString("typeid", typeid);
//                intent.putExtras(bundle);
//                startActivity(intent);
            }
        });

    }

    //toolbar事件监听方法
    @Override
    public void onClick(View v) {
        //返回上一页
        finish();
    }



    public class JCCallBack {

        @JavascriptInterface
        public void adViewJieCaoVideoPlayer(final int width, final int height, final int top, final int left, final int index) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (index == 0) {
                        JCVideoPlayerStandard webVieo = new JCVideoPlayerStandard(VideoNewsDetailActivity.this);
                        webVieo.setUp("http://video.jiecao.fm/11/16/c/68Tlrc9zNi3JomXpd-nUog__.mp4",
                                JCVideoPlayer.SCREEN_LAYOUT_LIST, "嫂子骑大马");
                        Picasso.with(VideoNewsDetailActivity.this)
                                .load("http://img4.jiecaojingxuan.com/2016/11/16/1d935cc5-a1e7-4779-bdfa-20fd7a60724c.jpg@!640_360")
                                .into(webVieo.thumbImageView);
                        ViewGroup.LayoutParams ll = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        AbsoluteLayout.LayoutParams layoutParams = new AbsoluteLayout.LayoutParams(ll);
                        layoutParams.y = UiUtils.dip2px( top);
                        layoutParams.x = UiUtils.dip2px( left);
                        layoutParams.height = UiUtils.dip2px(height);
                        layoutParams.width = UiUtils.dip2px( width);;
                        comment_web.addView(webVieo, layoutParams);
                    } else {
                        JCVideoPlayerStandard webVieo = new JCVideoPlayerStandard(VideoNewsDetailActivity.this);
                        webVieo.setUp("http://video.jiecao.fm/11/14/xin/%E5%90%B8%E6%AF%92.mp4",
                                JCVideoPlayer.SCREEN_LAYOUT_LIST, "嫂子失态了");
                        Picasso.with(VideoNewsDetailActivity.this)
                                .load("http://img4.jiecaojingxuan.com/2016/11/14/a019ffc1-556c-4a85-b70c-b1b49811d577.jpg@!640_360")
                                .into(webVieo.thumbImageView);
                        ViewGroup.LayoutParams ll = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        AbsoluteLayout.LayoutParams layoutParams = new AbsoluteLayout.LayoutParams(ll);
                        layoutParams.y = UiUtils.dip2px( top);
                        layoutParams.x = UiUtils.dip2px( left);
                        layoutParams.height = UiUtils.dip2px(height);
                        layoutParams.width = UiUtils.dip2px( width);
                        comment_web.addView(webVieo, layoutParams);
                    }

                }
            });

        }
    }

    @Override
    public void onBackPressed() {
        if (JCVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        JCVideoPlayer.releaseAllVideos();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
