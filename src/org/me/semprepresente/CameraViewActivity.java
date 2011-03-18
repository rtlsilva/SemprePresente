/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.semprepresente;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Bitmap.*;
import android.os.*;
import android.hardware.*;
import android.view.*;
import android.view.View.*;
import java.io.*;

/**
 *
 * @author Ricardo
 */
public class CameraViewActivity extends Activity implements SurfaceHolder.Callback, OnClickListener {

    static final int PHOTO_MODE = 0;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private boolean previewRunning;
    private Context context = this;
    private Camera.PictureCallback picCallback = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] imageData, Camera cam) {
            if(imageData != null) {
                Intent intent =  new Intent();
                storeByteImage(context, imageData, 50, "ImageName");
                camera.startPreview();
                setResult(PHOTO_MODE, intent);
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera_surface);

        surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewRunning) {
            camera.stopPreview();
        }
        Camera.Parameters param = camera.getParameters();
        param.setPreviewSize(width, height);
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
        previewRunning = true;
    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
        camera.stopPreview();
        previewRunning = false;
        camera.release();
    }

    public static boolean storeByteImage(Context context, byte[] imageData, int quality, String expName) {

        File sdImageMainDirectory = new File("/sdcard");
        FileOutputStream fileOutputStream = null;
        String nameFile;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 5;

            Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
            fileOutputStream = new FileOutputStream(sdImageMainDirectory.toString() + "/image.png");
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);

            image.compress(CompressFormat.PNG, quality, bos);

            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    public void onClick(View v) {
        camera.takePicture(null, picCallback, picCallback);
    }
}
