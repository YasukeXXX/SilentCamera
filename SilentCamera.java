package com.example.testcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;


public class SilentCamera extends Activity {

    private SurfaceView mySurfaceView;
    private Camera myCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_camera);

        mySurfaceView = (SurfaceView) findViewById(R.id.mySurfaceView);

        // クリック時のコールバック(リスナ)を設定
        mySurfaceView.setOnClickListener(onSurfaceClickListener);

        SurfaceHolder holder = mySurfaceView.getHolder();

        holder.addCallback(SurfaceCallback);
    }

    private SurfaceHolder.Callback SurfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            myCamera = Camera.open();
            myCamera.setDisplayOrientation(90);

            try {
                myCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // 最適なサイズを取得
            // myCameraのパラメータを取得
            Camera.Parameters param = myCamera.getParameters();
            // プレビューで表示できる横幅、縦幅、縦横比を格納
            List<Camera.Size> previewSizes = myCamera.getParameters().getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(previewSizes, width, height);
            param.setPreviewSize(optimalSize.width, optimalSize.height);
            myCamera.setParameters(param);

            myCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            myCamera.release();
            myCamera = null;
        }
    };


    // クリックしたとき
    private View.OnClickListener onSurfaceClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (myCamera != null)
                //オートフォーカス実行、実行時のコールバック設定
                myCamera.autoFocus(autoFocusCallback);
        }
    };

    // フォーカスしたとき
    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // プレビューを一枚切り取る、コールバック設定
            camera.setOneShotPreviewCallback(previewCallback);
        }
    };

    // プレビューを一枚切り取ったとき
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            int w = camera.getParameters().getPreviewSize().width;
            int h = camera.getParameters().getPreviewSize().height;
            Toast.makeText(getApplicationContext(), "フォーカスが合い画像を切り取りました。" + w, Toast.LENGTH_SHORT).show();

//            Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length,null);
            Bitmap bmp = getBitmapImageFromYUV(data, w, h);

            //回転
            Matrix m = new Matrix();
            m.setRotate(90);

            //保存用 bitmap生成
            Bitmap rotated_bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);

            //保存
            MediaStore.Images.Media.insertImage(getContentResolver(), rotated_bmp, "0000.jpg", null);

        }
    };


    public static Bitmap getBitmapImageFromYUV(byte[] data, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;
    }


    // 最適なサイズを取得
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> previewSizes, int width, int height) {

        final double ASPECT_TOLERANCE = 0.1;

        double targetRatio = (double) height / width; // 画面の縦横比

        if (previewSizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        // サポートされているサイズの中で最も最適なサイズを選ぶ
        for (Camera.Size size : previewSizes) {
            double ratio = (double) size.width / size.height; // プレビューの縦横比
            // 画面とプレビューの縦横比の差が大きい場合続行
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            // 画面とプレビューの縦幅の差が小さいとき
            if (Math.abs(size.height - targetHeight) < minDiff) {
                // 最適なサイズを更新
                optimalSize = size;
                // 最小の縦幅の差を更新
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : previewSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}

