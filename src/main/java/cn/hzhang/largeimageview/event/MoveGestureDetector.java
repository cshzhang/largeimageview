package cn.hzhang.largeimageview.event;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;

/**
 * Created by hzh on 2015/10/21.
 */
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
