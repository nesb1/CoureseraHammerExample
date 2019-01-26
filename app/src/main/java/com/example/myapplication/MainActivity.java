package com.example.myapplication;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity implements ImageProcessThread.Callback{

    //весь код ниже выполняется в мейнтреде

    private ImageView mDoge;

    private ProgressBar mProgressBar;

    private ImageProcessThread mImageProcessThread;

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final Button performBtn = findViewById(R.id.btn_perform);

        mDoge = findViewById(R.id.iv_doge);

        mProgressBar = findViewById(R.id.progress);

        //создаем новый экземпляр фонового потока потока

        mImageProcessThread = new ImageProcessThread("Background ");

        //запускаем поток и инициализируем Looper

        mImageProcessThread.start();

        mImageProcessThread.getLooper();  // -> вызовется onLooperPrepared()

        mImageProcessThread.setCallback(this);

        //теперь в фоновом потоке будет крутиться лупер и ждать задач



        performBtn.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {

                //выдергиваем Bitmap из ImageView и скармливаем в наш поток,

                //просто вызывая метод на нем

                BitmapDrawable drawable = (BitmapDrawable) mDoge.getDrawable();

                mImageProcessThread.performOperation(drawable.getBitmap());

            }

        });

    }

    // методы колбека из интерфейса

    @Override

    public void sendProgress(int progress) {

        mProgressBar.setProgress(progress);

    }


    public void onCompleted(Bitmap bitmap) {

        mDoge.setImageBitmap(bitmap);

    }

    @Override

    protected void onDestroy() {

        //гасим фоновый поток, не мусорим

        mImageProcessThread.quit();

        super.onDestroy();

    }

}
