package com.example.makephotosketch;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";

    ImageView imageView;
    Bitmap initial, grayScaled, inverted, blurred, finalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageViewID);

        initial = BitmapFactory.decodeResource(getResources(), R.drawable.initial_1);
        new makeSketch().execute();
    }

    class makeSketch extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            grayScaled = getGrayScaledBitmap(initial);
            inverted = getInvertedBitmap(grayScaled);
            blurred = getBlurredBitmap(inverted);
            finalBitmap = getFinalSketch(blurred, grayScaled);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "onPostExecute: Done" );
            imageView.setImageBitmap(finalBitmap);
        }
    }

    private Bitmap getFinalSketch(Bitmap source, Bitmap layer) {

        Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

        IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
        base.copyPixelsToBuffer(buffBase);
        buffBase.rewind();

        IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
        blend.copyPixelsToBuffer(buffBlend);
        buffBlend.rewind();

        IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
        buffOut.rewind();

        while (buffOut.position() < buffOut.limit()) {

            int filterInt = buffBlend.get();
            int srcInt = buffBase.get();

            int redValueFilter = Color.red(filterInt);
            int greenValueFilter = Color.green(filterInt);
            int blueValueFilter = Color.blue(filterInt);

            int redValueSrc = Color.red(srcInt);
            int greenValueSrc = Color.green(srcInt);
            int blueValueSrc = Color.blue(srcInt);

            int redValueFinal = colordodge(redValueFilter, redValueSrc);
            int greenValueFinal = colordodge(greenValueFilter, greenValueSrc);
            int blueValueFinal = colordodge(blueValueFilter, blueValueSrc);


            int pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal);


            buffOut.put(pixel);
        }

        buffOut.rewind();

        base.copyPixelsFromBuffer(buffOut);
        blend.recycle();

        return base;
    }

    private int colordodge(int filter, int grayScalValue) {
        float image = (float)grayScalValue;
        float mask = (float)filter;
        return ((int) ((image == 255) ? image:Math.min(255, (((long)mask << 8 ) / (255 - image)))));
    }

    private Bitmap getBlurredBitmap(Bitmap inverted) {

        Bitmap photo = inverted.copy(Bitmap.Config.ARGB_8888, true);

        RenderScript renderScript = RenderScript.create(this);
        Allocation input = Allocation.createFromBitmap(renderScript, inverted, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(renderScript, input.getType());
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        scriptIntrinsicBlur.setRadius(9.5f);
        scriptIntrinsicBlur.setInput(input);
        scriptIntrinsicBlur.forEach(output);
        output.copyTo(photo);
        return photo;
    }

    private Bitmap getInvertedBitmap(Bitmap grayScaled) {
        int height = grayScaled.getHeight();
        int width = grayScaled.getWidth();

        ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                -1, 0, 0, 0, 255,
                0, -1, 0, 0, 255,
                0, 0, -1, 0, 255,
                0, 0, 0, 1, 0});
        ColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(colorFilter);

        Bitmap invertedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(invertedBitmap);
        canvas.drawBitmap(grayScaled,0,0,paint);
        return invertedBitmap;
    }

    private Bitmap getGrayScaledBitmap(Bitmap initial) {
        int height = initial.getHeight();
        int width = initial.getWidth();

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);

        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(colorMatrixColorFilter);

        Bitmap grayScaledBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(grayScaledBitmap);
        canvas.drawBitmap(initial, 0, 0, paint);
        return grayScaledBitmap;
    }
}
