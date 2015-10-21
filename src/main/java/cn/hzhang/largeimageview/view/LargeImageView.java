package cn.hzhang.largeimageview.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

import cn.hzhang.largeimageview.event.MoveGestureDetector;

/**
 * Created by hzh on 2015/10/21.
 */
public class LargeImageView extends View
{
    /**
     * BitmapRegionDecoder
     */
    private BitmapRegionDecoder mDecoder;
    private static final BitmapFactory.Options mDecodeOptions = new BitmapFactory.Options();
    static
    {
        mDecodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    private Rect mRect = new Rect();

    //图片的宽高
    private int mImageWidth;
    private int mImageHeight;

    //检测Move
    private MoveGestureDetector mMoveGestureDetector;

    public LargeImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    /**
     * 对外公布的方法
     *
     * @param is
     */
    public void getImageInputStream(InputStream is)
    {
        try
        {
            //初始化mDecoder
            mDecoder = BitmapRegionDecoder.newInstance(is, false);

            //得到图片的宽高
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            mImageWidth = options.outWidth;
            mImageHeight = options.outHeight;

            requestLayout();
            invalidate();
        } catch (IOException e)
        {
            e.printStackTrace();
        }finally
        {
            try
            {
                if(is != null)
                {
                    is.close();
                }
            }catch (Exception e)
            {}
        }
    }

    private void init()
    {
        mMoveGestureDetector = new MoveGestureDetector(getContext(),
                new MoveGestureDetector.SimpleMoveGestureDetector()
        {
            @Override
            public boolean onMove(MoveGestureDetector detector)
            {
                //移动rect
                int movX = (int) detector.getMoveX();
                int movY = (int) detector.getMoveY();

                if(mImageWidth > getWidth())
                {
                    mRect.offset(-movX, 0);
                    checkWidth();
                    invalidate();
                }
                if(mImageHeight > getHeight())
                {
                    mRect.offset(0, -movY);
                    checkHeight();
                    invalidate();
                }
                return true;
            }
        });
    }

    private void checkHeight()
    {
        if(mRect.bottom > mImageHeight)
        {
            mRect.bottom = mImageHeight;
            mRect.top = mRect.bottom - getHeight();
        }
        if(mRect.top < 0)
        {
            mRect.top = 0;
            mRect.bottom = mRect.top + getHeight();
        }
    }

    private void checkWidth()
    {
        if(mRect.right > mImageWidth)
        {
            mRect.right = mImageWidth;
            mRect.left = mImageWidth - getWidth();
        }
        if(mRect.left < 0)
        {
            mRect.left = 0;
            mRect.right = mRect.left + getWidth();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mMoveGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        //初始化mRect，显示图片中间区域
        mRect.left = mImageWidth/2 - width/2;
        mRect.top = mImageHeight/2 - height/2;
        mRect.right = mRect.left + width;
        mRect.bottom = mRect.top + height;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Bitmap bitmap = mDecoder.decodeRegion(mRect, mDecodeOptions);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }
}
