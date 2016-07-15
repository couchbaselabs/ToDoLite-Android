package com.couchbase.todolite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ImageActivity extends AppCompatActivity {
    public static final String INTENT_IMAGE = "image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        byte[] byteArray = getIntent().getByteArrayExtra(INTENT_IMAGE);
        Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        ImageView imageView = (ImageView) findViewById(R.id.image);
        imageView.setImageBitmap(image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
