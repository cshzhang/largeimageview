# largeimageview
LargeImageView超大图的显示Demo

#概述
对于加载图片，一般为了尽可能避免OOM都会按照如下做法：
*对于图片显示：根据显示图片的控件大小对图片进行压缩；
*对于图片数量非常多：使用LruCache等缓存机制，将一些图片维持在内存中；

其实对于图片还有一种加载情况，就是单个图片非常巨大且不允许压缩。比如显示：世界地图，清明上河图...
那么对于这种需求该如何实现？
首先不压缩，按照原图尺寸加载，那么屏幕肯定不够大，所以肯定是局部加载，那么肯定用到一个类：
``java
BitmapRegionRecoder
``
其次，既然屏幕显示不完全，就需要添加Move手势检查，让用户可以拖动查看。

#效果图
<img src="xxx.gif" width="320px">

#BitmapRegionRecoder简单使用
BitmapRegionRecoder主要用于显示图片的某一块矩形区域。
BitmapRegionDecoder提供一系列构造方法来初始化该对象，支持传入文件路径，文件描述符，文件的inputstream等。
例如：
``java
BitmapRegionDecoder bitmapRegionDecoder =
  BitmapRegionDecoder.newInstance(inputStream, false);
``

接下来就是显示指定区域的方法：
``java
bitmapRegionDecoder.decodeRegion(rect, options);
``
参数一是一个rect，参数二是BitmapFactory.Options,可以控制inSampleSize,inPreferredConfig等。

#自定义View显示大图
思路：
*提供一个设置图片的入口
*重写onTouchEvent()方法，根据用户移动的手势，去更新显示区域的Rect
*每次更新Rect之后，调用invalidate方法，重写onDraw(),在里面去regionDecoder.decodeRegion(rect, options)实现绘制

上代码：
``java
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
     * 对外公布的方法，设置图片
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
	//拿到最新rect对应的bitmap，进行绘制；
        Bitmap bitmap = mDecoder.decodeRegion(mRect, mDecodeOptions);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }
}
``
根据上述代码
*getImageInputStream里面获取图片的真实高度，初始化mDecoder
*onMeasure里面初始化mRect，大小为view的尺寸，并且显示图片中间区域
*onTouchEvent里面监听Move手势，在监听的回调里面改变Rect参数，以及边界检查，最后invalidate
*onDraw里面拿到最新rect对应的bitmap，进行绘制

OK，上面并不复杂；但是监听Move的方法有点奇怪：
``java
mMoveGestureDetector.onTouchEvent(event);
``
嗯，这里模仿了系统的ScaleGestureDetector编写了MoveGestureDetector

MoveGestureDetector代码如下：
``java
public class MoveGestureDetector
{
    private Context mContext;

    private PointF mPrePointer;
    private PointF mCurPointer;

    private boolean isGestureMoving;
    private MotionEvent mPreMotionEvent;
    private MotionEvent mCurrentMotionEvent;

    public OnMoveGestureListener mListener;

    //记录最终结果返回
    private PointF mDeltaPointer = new PointF();

    public MoveGestureDetector(Context context, OnMoveGestureListener listener)
    {
        this.mContext = context;
        this.mListener = listener;
    }

    public float getMoveX()
    {
        return mDeltaPointer.x;
    }

    public float getMoveY()
    {
        return mDeltaPointer.y;
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if(!isGestureMoving)
        {
            handleStartEvent(event);
        }else
        {
            handleProgressEvent(event);
        }

        return true;
    }

    private void handleProgressEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mListener.onMoveEnd(this);
                resetState();
                break;
            case MotionEvent.ACTION_MOVE:
                updateStateByEvent(event);
                if(mListener.onMove(this))
                {
                    mPreMotionEvent.recycle();
                    mPreMotionEvent = MotionEvent.obtain(event);
                }
                break;
        }
    }

    private void handleStartEvent(MotionEvent event)
    {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                resetState();
                mPreMotionEvent = MotionEvent.obtain(event);
                updateStateByEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                isGestureMoving = mListener.onMoveBegin(this);
                break;
        }
    }

    private void updateStateByEvent(MotionEvent event)
    {
        MotionEvent preEvent = mPreMotionEvent;

        mPrePointer = calculateFocalPointer(preEvent);
        mCurPointer = calculateFocalPointer(event);

        boolean skipThisMoveEvent = preEvent.getPointerCount() != event.getPointerCount();

        //更新deltaX和deltaY
        mDeltaPointer.x = skipThisMoveEvent ? 0 : mCurPointer.x - mPrePointer.x;
        mDeltaPointer.y = skipThisMoveEvent ? 0 : mCurPointer.y - mPrePointer.y;
    }

    private PointF calculateFocalPointer(MotionEvent event)
    {
        int count = event.getPointerCount();
        float x = 0, y = 0;
        for(int i = 0; i < count; i++)
        {
            x += event.getX(i);
            y += event.getY(i);
        }

        x /= count;
        y /= count;
        return new PointF(x, y);
    }

    private void resetState()
    {
        if(mPreMotionEvent != null)
        {
            mPreMotionEvent.recycle();
            mPreMotionEvent = null;
        }
        if(mCurrentMotionEvent != null)
        {
            mCurrentMotionEvent.recycle();
            mCurrentMotionEvent = null;
        }
        isGestureMoving = false;
    }

    public interface OnMoveGestureListener
    {
        public boolean onMoveBegin(MoveGestureDetector detector);
        public boolean onMove(MoveGestureDetector detector);
        public void onMoveEnd(MoveGestureDetector detector);
    }

    public static class SimpleMoveGestureDetector implements OnMoveGestureListener
    {

        @Override
        public boolean onMoveBegin(MoveGestureDetector detector)
        {
            return true;
        }

        @Override
        public boolean onMove(MoveGestureDetector detector)
        {
            return false;
        }

        @Override
        public void onMoveEnd(MoveGestureDetector detector)
        {

        }
    }
}
``
简单分析一下：
*OnMoveGestureListener内部接口以及SimpleMoveGestureDetector内部类都是模仿系统ScaleGestureDetector设计
*构造方法MoveGestureDetector(Context context, OnMoveGestureListener listener)要求用户初始化OnMoveGestureListener并传递进来
*对外公布onTouchEvent()，外部必须调用该方法，并把最新的event传递进来；
*对外公布getMoveX()，getMoveY()，外部可以通过这两个方法，拿到Move时候最新的deltaX和deltaY
*updateStateByEvent(event)根据最新的event，更新mDeltaPointer.x和mDeltaPointer.y
*剩余的方法：handleStartEvent(MotionEvent)；handleProgressEvent(MotionEvent)主要就是记录mPreMotionEvent，调用updateStateByEvent(MotionEvent)等来实现逻辑功能