package cn.hzhang.largeimageview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;

import cn.hzhang.largeimageview.view.LargeImageView;

public class MainActivity extends AppCompatActivity
{
    private LargeImageView mLargeImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLargeImageView = (LargeImageView) findViewById(R.id.id_large);
        try
        {
            InputStream is = getAssets().open("qm.jpg");
            mLargeImageView.getImageInputStream(is);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
