package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;


public class ImageProcessThread extends HandlerThread {

    private static final int MESSAGE_CONVERT = 0;

    private static final int PERCENT = 100;

    private static final int PARTS_COUNT = 50;

    private static final int PART_SIZE = PERCENT / PARTS_COUNT;



    private Handler mMainHandler;

    private Handler mBackgroundHandler;

    private Callback mCallback;



    public ImageProcessThread(String name) {

        super(name);

    }



    public void setCallback(Callback callback) {

        mCallback = callback;


    }



    @SuppressLint("HandlerLeak")  // находимся в хендлер треде, утечки не будет

    @Override

    protected void onLooperPrepared() {

        //создаем хендлер, связанный с мейнтредом

        mMainHandler = new Handler(Looper.getMainLooper());



        //также создаем хендлер, связанный с текущим тредом

        mBackgroundHandler = new Handler() {

            //и указываем ему, что делать в случае получения сообщения с нашим what значением

            // это будет выполнено в фоновом потоке

            @Override

            public void handleMessage(Message msg) {

                switch (msg.what) {

                    case MESSAGE_CONVERT: {

                        //выдергиваем битмапу и процессим ее

                        Bitmap bitmap = (Bitmap) msg.obj;

                        processBitmap(bitmap);



                    }

                }

            }

        };

    }



    private void processBitmap(final Bitmap bitmap) {

        //бессмысленно-беспощадная долгая операция

        //здесь я выдергиваю пиксели из битмапы

        int h = bitmap.getHeight();

        int w = bitmap.getWidth();

        int[] pixels = new int[h * w];

        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);



        // java - лайфхак - костыль - использую final массив с одной ячейкой, в которой буду менять процент.

        // в функциональные интерфейсы мы можем передавать только финальные или эффективно финальные значения

        // но процент у меня меняется, и я обхожу это требование

        final int[] progress = new int[1];



        //потом прохожусь по каждому пикселю и сдвигаю хекс значения

        //красный на зеленый, зеленый на синий, синий на красный

        for (int i = 0; i < h * w; i++) {

            String hex = String.format("#%06X", (0xFFFFFF & pixels[i]));

            String R = hex.substring(1, 3);

            String G = hex.substring(3, 5);

            String B = hex.substring(5);

            String mess = B + R + G;

            pixels[i] = Integer.parseInt(mess, 16);



            //здесь логика показа процента готовности обработки изображения

            //показываем PARTS_COUNT (50) кусочков в прогресс баре

            int part = w * h / PARTS_COUNT;



            if (i % part == 0) {

                progress[0] = i / part * PART_SIZE; // <- костыльная магия

                //постим в мейнтред через мейнхендлер

                mMainHandler.post(new Runnable() {

                    @Override

                    public void run() {

                        // этот код уже выполняется в главном потоке

                        mCallback.sendProgress(progress[0]);

                    }

                });

            }

        }



        //создаем битмапу из пикселей с уже смещенным цветом

        final Bitmap result = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.RGB_565);

        //постим в мейнтред

        mMainHandler.post(new Runnable() {

            @Override

            public void run() {

                //этот код так же исполнится в мейнтреде

                mCallback.onCompleted(result);

            }

        });



    }



    //этот метод будет вызываться  из главного потока

    public void performOperation(Bitmap inputData) {

        // создаем Message от BackgroundHandler'а, зааписываем в него Bitmap и отправляем в очередь

        mBackgroundHandler

                .obtainMessage(MESSAGE_CONVERT, inputData)

                .sendToTarget();

        // созданное сообщение попадает в очередь фонового потока,

        // так как хендлер связан с лупером фонового потока

        // сейчас вызовется метод handleMessage(),

        // который переопределен выше, в методе onLooperPrepared



    }




    public interface Callback {

        void sendProgress(int progress);

        void onCompleted(Bitmap bitmap);

    }

}
