package com.nawab.swipeableview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.nawab.swipeableview.R;


/**
 * @author Nawab Hussain
 */

public class SwipeableView extends ViewGroup {

    public static final int SWIPE_LEFT = 0;
    public static final int SWIPE_RIGHT = 1;
    private float mLeftSwipeRange;
    private float mRightSwipeRange;
    private final int mSwipeDirection;
    private final ViewDragHelper mDragHelper;
    private final int mTouchSlop;
    //View to be swiped
    private View mSwipeItem;

    private int mHorizontalDragRange;
    private boolean mFirstLayout = true;
    private boolean mHasPassedLeftThreshold;
    private boolean mHasPassedRightThreshold;
    private int mPreviousPosition;
    private boolean mSwipeLeft;
    private boolean mSwipeRight;

    public SwipeableView(Context context) {
        this(context, null);
    }

    public SwipeableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.SwipeableView);
        float mSwipeRange = arr.getFloat(R.styleable.SwipeableView_swipe_range, 0);
        mSwipeDirection = arr.getInteger(R.styleable.SwipeableView_direction, 0);

        checkDirection(mSwipeDirection, mSwipeRange);
        arr.recycle();

        ViewConfiguration vc = ViewConfiguration.get(this.getContext());
        mTouchSlop = vc.getScaledTouchSlop();

        mDragHelper = ViewDragHelper.create(this, new DragHelperCallback());

    }

    private void checkDirection(int mSwipeDirection, float mSwipeRange) {
        mSwipeLeft = mSwipeDirection == SWIPE_LEFT ? true : false;
        if (!mSwipeLeft) {
            mSwipeRight = true;
            mRightSwipeRange = mSwipeRange;
        } else {
            mSwipeRight = false;
            mLeftSwipeRange = mSwipeRange;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
        mPreviousPosition = 0;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
        mPreviousPosition = 0;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h != oldh) {
            mFirstLayout = true;
            mPreviousPosition = 0;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() > 1) {
            throw new IllegalStateException(getContext().getString(R.string.swipeable_more_than_one_child_exception));
        }
        // Measure child
        mSwipeItem = getChildAt(0);

        measureChildWithMargins(mSwipeItem, widthMeasureSpec, 0, heightMeasureSpec, 0);

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mSwipeItem.getMeasuredHeight());

        mSwipeItem.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        final int parentLeft = getPaddingLeft();
        final int parentRight = right - left - getPaddingRight();
        final int parentTop = getPaddingTop();

        if (mFirstLayout) {
            // restore state
            int childLeft = parentLeft;
            int childRight = parentRight;
            mHorizontalDragRange = (int) (getMeasuredWidth() * 0.8);
            mSwipeItem.layout(childLeft, parentTop, childRight, parentTop + mSwipeItem.getMeasuredHeight());
            mFirstLayout = false;
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(SwipeableView.LayoutParams source) {
            super(source);
        }
    }

    // Swipe Methods


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragHelper.processTouchEvent(ev);
        // handle parent scroll behaviour
        if (Math.abs(mSwipeItem.getLeft()) > mTouchSlop) {
            // disable parent scrolling
            ViewParent parent = getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
        } else if (MotionEventCompat.getActionMasked(ev) == MotionEvent.ACTION_UP || MotionEventCompat.getActionMasked(ev) == MotionEvent.ACTION_CANCEL) {
            // enable parent scrolling
            ViewParent parent = getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View view, int i) {
            return view == mSwipeItem;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (left < 0) {
                return child.getLeft() + Math.round(mLeftSwipeRange * dx);
            } else {
                return child.getLeft() + Math.round(mRightSwipeRange * dx);
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mHorizontalDragRange;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_IDLE) {
                if (mSwipeItem.getLeft() == -mHorizontalDragRange) {
//                    Handle Left Swipe
                } else if (mSwipeItem.getLeft() == mHorizontalDragRange) {
//                    Handle Right Swipe
                } else if (mSwipeItem.getLeft() == 0) {
                    // check whether settled from restricted swipe
                    if (mLeftSwipeRange != 1.0f && mHasPassedLeftThreshold) {
                        mHasPassedLeftThreshold = false;
                        // Handle Left Swipe
                    }
                    if (mRightSwipeRange != 1.0f && mHasPassedRightThreshold) {
                        mHasPassedRightThreshold = false;
//                        Handle Right Swipe
                    }
                }
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            handlePositionChange(left);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            // logic for slide behaviour
            if (xvel < 0 && mLeftSwipeRange == 1.0f) {
                // dismiss to left
                mDragHelper.settleCapturedViewAt(-mHorizontalDragRange, releasedChild.getTop());
            } else if (xvel > 0 && mRightSwipeRange == 1.0f) {
                // dismiss to right
                mDragHelper.settleCapturedViewAt(mHorizontalDragRange, releasedChild.getTop());
            } else {
                // not enough velocity to dismiss
                mDragHelper.settleCapturedViewAt(0, releasedChild.getTop());
            }

            invalidate();
        }
    }

    private void handlePositionChange(int newLeft) {
        if (newLeft > 0) {

            float rightRange = mRightSwipeRange;
            if (rightRange != 1.0f && newLeft > Math.round(mHorizontalDragRange * rightRange * 0.75f)) {
                mHasPassedRightThreshold = true;
                mHasPassedLeftThreshold = false;
            }
        } else if (newLeft < 0) {

            float leftRange = mLeftSwipeRange;
            if (leftRange != 1.0f && newLeft < (-mHorizontalDragRange * leftRange * 0.75f)) {
                mHasPassedLeftThreshold = true;
                mHasPassedRightThreshold = false;
            }
        }
        mPreviousPosition = newLeft;
    }

    public void swipeBack() {
        if (mDragHelper.smoothSlideViewTo(mSwipeItem, 0, mSwipeItem.getTop())) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


}
