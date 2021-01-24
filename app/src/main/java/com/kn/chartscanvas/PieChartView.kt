package com.kn.chartscanvas

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class PieChartView : ViewGroup {
    private val mData: MutableList<Item> = ArrayList()
    private var mTotal = 0.0f
    private var mPieBounds = RectF()
    private var mPiePaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mShadowPaint: Paint? = null
    private var mShowText = false
    private var mTextX = 0.0f
    private var mTextY = 0.0f
    private var mTextWidth = 0.0f
    private var mTextHeight = 0.0f
    private var mTextPos = TEXTPOS_LEFT
    private var mHighlightStrength = 1.15f
    private var mPointerRadius = 2.0f
    private var mPointerX = 0f
    private var mPointerY = 0f

    /**
     * Returns the current rotation of the pie graphic.
     *
     * @return The current pie rotation, in degrees.
     */
    var pieRotation = 0
        private set
    private var mCurrentItemChangedListener: OnCurrentItemChangedListener? = null
    private var mTextColor = 0
    private var mPieView: PieView? = null
    private val mScroller: Scroller? = null
    private val mScrollAnimator: ValueAnimator? = null
    private val mDetector: GestureDetector? = null
    private var mPointerView: PointerView? = null

    // The angle at which we measure the current item. This is
    // where the pointer points.
    private var mCurrentItemAngle = 0

    // the index of the current item.
    private var mCurrentItem = 0
    private var mAutoCenterInSlice = false
    private val mAutoCenterAnimator: ObjectAnimator? = null
    private var mShadowBounds = RectF()

    interface OnCurrentItemChangedListener {
        fun OnCurrentItemChanged(source: PieChartView?, currentItem: Int)
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val a: TypedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.PieChartView,
                0, 0
        )
        try {
            // Retrieve the values from the TypedArray and store into
            // fields of this class.
            //
            // The R.styleable.PieChart_* constants represent the index for
            // each custom attribute in the R.styleable.PieChart array.
            mShowText = a.getBoolean(R.styleable.PieChartView__isShowText, false)
            mTextY = a.getDimension(R.styleable.PieChartView__textY, 0.0f)
            mTextWidth = a.getDimension(R.styleable.PieChartView__textWidth, 0.0f)
            mTextHeight = a.getDimension(R.styleable.PieChartView__textHeight, 0.0f)
            mTextPos = a.getInteger(R.styleable.PieChartView__textPos, 0)
            mTextColor = a.getColor(R.styleable.PieChartView__textColor, 0xffffff)
            mHighlightStrength = a.getFloat(R.styleable.PieChartView__highlightStrength, 1.0f)
            pieRotation = a.getInt(R.styleable.PieChartView__pieRotation, 0)
            mPointerRadius = a.getDimension(R.styleable.PieChartView__pointerRadius, 2.0f)
            mAutoCenterInSlice = a.getBoolean(R.styleable.PieChartView__autoCenterInSlice, false)
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle()
        }
        init()
    }

    fun resetChart() {
        mData.clear()
        mTotal = 0f
        showText = false
        onDataChanged()
        invalidate()
    }

    /**
     * Returns true if the text label should be visible.
     *
     * @return True if the text label should be visible, false otherwise.
     */
    /**
     * Controls whether the text label is visible or not. Setting this property to
     * false allows the pie chart graphic to take up the entire visible area of
     * the control.
     *
     * @param showText true if the text label should be visible, false otherwise
     */
    var showText: Boolean
        get() = mShowText
        set(showText) {
            mShowText = showText
            invalidate()
        }

    /**
     * Returns the Y position of the label text, in pixels.
     *
     * @return The Y position of the label text, in pixels.
     */
    /**
     * Set the Y position of the label text, in pixels.
     *
     * @param textY the Y position of the label text, in pixels.
     */
    var textY: Float
        get() = mTextY
        set(textY) {
            mTextY = textY
            invalidate()
        }

    /**
     * Returns the width reserved for label text, in pixels.
     *
     * @return The width reserved for label text, in pixels.
     */
    /**
     * Set the width of the area reserved for label text. This width is constant; it does not
     * change based on the actual width of the label as the label text changes.
     *
     * @param textWidth The width reserved for label text, in pixels.
     */
    var textWidth: Float
        get() = mTextWidth
        set(textWidth) {
            mTextWidth = textWidth
            invalidate()
        }

    /**
     * Returns the height of the label font, in pixels.
     *
     * @return The height of the label font, in pixels.
     */
    /**
     * Set the height of the label font, in pixels.
     *
     * @param textHeight The height of the label font, in pixels.
     */
    var textHeight: Float
        get() = mTextHeight
        set(textHeight) {
            mTextHeight = textHeight
            invalidate()
        }

    /**
     * Returns a value that specifies whether the label text is to the right
     * or the left of the pie chart graphic.
     *
     * @return One of TEXTPOS_LEFT or TEXTPOS_RIGHT.
     */
    /**
     * Set a value that specifies whether the label text is to the right
     * or the left of the pie chart graphic.
     *
     * @param textPos TEXTPOS_LEFT to draw the text to the left of the graphic,
     * or TEXTPOS_RIGHT to draw the text to the right of the graphic.
     */
    var textPos: Int
        get() = mTextPos
        set(textPos) {
            require(!(textPos != TEXTPOS_LEFT && textPos != TEXTPOS_RIGHT)) { "TextPos must be one of TEXTPOS_LEFT or TEXTPOS_RIGHT" }
            mTextPos = textPos
            invalidate()
        }

    /**
     * Returns the strength of the highlighting applied to each pie segment.
     *
     * @return The highlight strength.
     */
    var highlightStrength: Float
        get() = mHighlightStrength
        set(highlightStrength) {
            require(highlightStrength >= 0.0f) { "highlight strength cannot be negative" }
            mHighlightStrength = highlightStrength
            invalidate()
        }

    /**
     * Returns the radius of the filled circle that is drawn at the tip of the current-item
     * pointer.
     *
     * @return The radius of the pointer tip, in pixels.
     */
    /**
     * Set the radius of the filled circle that is drawn at the tip of the current-item
     * pointer.
     *
     * @param pointerRadius The radius of the pointer tip, in pixels.
     */
    var pointerRadius: Float
        get() = mPointerRadius
        set(pointerRadius) {
            mPointerRadius = pointerRadius
            invalidate()
        }

    /**
     * Set the current rotation of the pie graphic. Setting this value may change
     * the current item.
     *
     * @param rotation The current pie rotation, in degrees.
     */
    //    public void setPieRotation(int rotation) {
    //        rotation = (rotation % 360 + 360) % 360;
    //        mPieRotation = rotation;
    //        mPieView.rotateTo(rotation);
    //
    //        calcCurrentItem();
    //    }
    /**
     * Returns the index of the currently selected data item.
     *
     * @return The zero-based index of the currently selected data item.
     */
    /**
     * Set the currently selected item. Calling this function will set the current selection
     * and rotate the pie to bring it into view.
     *
     * @param currentItem The zero-based index of the item to select.
     */
    var currentItem: Int
        get() = mCurrentItem
        set(currentItem) {
            setCurrentItem(currentItem, true)
        }

    /**
     * Set the current item by index. Optionally, scroll the current item into view. This version
     * is for internal use--the scrollIntoView option is always true for external callers.
     *
     * @param currentItem    The index of the current item.
     * @param scrollIntoView True if the pie should rotate until the current item is centered.
     * False otherwise. If this parameter is false, the pie rotation
     * will not change.
     */
    private fun setCurrentItem(currentItem: Int, scrollIntoView: Boolean) {
        mCurrentItem = currentItem
        if (mCurrentItemChangedListener != null) {
            mCurrentItemChangedListener!!.OnCurrentItemChanged(this, currentItem)
        }
        if (scrollIntoView) {
            centerOnCurrentItem()
        }
        invalidate()
    }

    /**
     * Register a callback to be invoked when the currently selected item changes.
     *
     * @param listener Can be null.
     * The current item changed listener to attach to this view.
     */
    fun setOnCurrentItemChangedListener(listener: OnCurrentItemChangedListener?) {
        mCurrentItemChangedListener = listener
    }

    fun addItem(item: Item): Int {

        // Calculate the highlight color. Saturate at 0xff to make sure that high values
        // don't result in aliasing.
        item.highlight = Color.argb(
                0xff,
                Math.min(
                        (mHighlightStrength * Color.red(item.color).toFloat()).toInt(),
                        0xff
                ),
                Math.min(
                        (mHighlightStrength * Color.green(item.color).toFloat()).toInt(),
                        0xff
                ),
                Math.min(
                        (mHighlightStrength * Color.blue(item.color).toFloat()).toInt(),
                        0xff
                )
        )
        mTotal += item.value
        mData.add(item)
        onDataChanged()
        return mData.size - 1
    }

    //    @Override
    //    public boolean onTouchEvent(MotionEvent event) {
    //        // Let the GestureDetector interpret this event
    //        boolean result = mDetector.onTouchEvent(event);
    //
    //        // If the GestureDetector doesn't want this event, do some custom processing.
    //        // This code just tries to detect when the user is done scrolling by looking
    //        // for ACTION_UP events.
    //        if (!result) {
    //            if (event.getAction() == MotionEvent.ACTION_UP) {
    //                // User is done scrolling, it's now safe to do things like autocenter
    //                stopScrolling();
    //                result = true;
    //            }
    //        }
    //        return result;
    //    }
    override fun onLayout(
            changed: Boolean,
            l: Int,
            t: Int,
            r: Int,
            b: Int
    ) {
        // Do nothing. Do not call the superclass method--that would start a layout pass
        // on this view's children. PieChart lays out its children in onSizeChanged().
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the shadow
        canvas.drawOval(mShadowBounds, mShadowPaint!!)
        // If the API level is less than 11, we can't rely on the view animation system to
        // do the scrolling animation. Need to tick it here and call postInvalidate() until the scrolling is done.
//        if (Build.VERSION.SDK_INT < 11) {
//            //tickScrollAnimation();
//            if (!mScroller.isFinished()) {
//                postInvalidate();
//            }
//        }
    }

    //
    // Measurement functions. This example uses a simple heuristic: it assumes that
    // the pie chart should be at least as wide as its label.
    //
    override fun getSuggestedMinimumWidth(): Int {
        return mTextWidth.toInt() * 2
    }

    override fun getSuggestedMinimumHeight(): Int {
        return mTextWidth.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Try for a width based on our minimum
        val minw = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec))

        // Whatever the width ends up being, ask for a height that would let the pie
        // get as big as it can
        val minh = w - mTextWidth.toInt() + paddingBottom + paddingTop
        val h = Math.min(MeasureSpec.getSize(heightMeasureSpec), minh)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        //
        // Set dimensions for text, pie chart, etc
        //
        // Account for padding
        var xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()

        // Account for the label
        if (mShowText) xpad += mTextWidth
        val ww = w.toFloat() - xpad
        val hh = h.toFloat() - ypad

        // Figure out how big we can make the pie.
        val diameter = Math.min(ww, hh)
        mPieBounds = RectF(
                0.0f,
                0.0f,
                diameter,
                diameter
        )
        mPieBounds.offsetTo(paddingLeft.toFloat(), paddingTop.toFloat())
        mPointerY = mTextY - mTextHeight / 2.0f
        var pointerOffset = mPieBounds.centerY() - mPointerY

        // Make adjustments based on text position
        if (mTextPos == TEXTPOS_LEFT) {
            mTextPaint?.textAlign = Paint.Align.RIGHT
            if (mShowText) mPieBounds.offset(mTextWidth, 0.0f)
            mTextX = mPieBounds.left
            Log.d("mtext", "mtextX: $mTextX")
            if (pointerOffset < 0) {
                pointerOffset = -pointerOffset
                mCurrentItemAngle = 225
            } else {
                mCurrentItemAngle = 135
            }
            mPointerX = mPieBounds.centerX() - pointerOffset
        } else {
            mTextPaint?.textAlign = Paint.Align.LEFT
            mTextX = mPieBounds.right
            Log.d("mtext", "mtextX: $mTextX")
            if (pointerOffset < 0) {
                pointerOffset = -pointerOffset
                mCurrentItemAngle = 315
            } else {
                mCurrentItemAngle = 45
            }
            mPointerX = mPieBounds.centerX() + pointerOffset
        }
        mShadowBounds = RectF(
                mPieBounds.left + 10,
                mPieBounds.bottom + 10,
                mPieBounds.right - 10,
                mPieBounds.bottom + 20
        )

        // Lay out the child view that actually draws the pie.
        mPieView?.layout(
                mPieBounds.left.toInt(),
                mPieBounds.top.toInt(),
                mPieBounds.right.toInt(),
                mPieBounds.bottom.toInt()
        )
        mPieView?.setPivot(mPieBounds.width() / 2, mPieBounds.height() / 2)
        mPointerView?.layout(0, 0, w, h)
        onDataChanged()
    }

    /**
     * Calculate which pie slice is under the pointer, and set the current item
     * field accordingly.
     */
    private fun calcCurrentItem() {
        val pointerAngle = (mCurrentItemAngle + 360 + pieRotation) % 360
        for (i in mData.indices) {
            val it = mData[i]
            if (it.startAngle <= pointerAngle && pointerAngle <= it.endAngle) {
                if (i != mCurrentItem) {
                    setCurrentItem(i, false)
                }
                break
            }
        }
    }

    /**
     * Do all of the recalculations needed when the data array changes.
     */
    private fun onDataChanged() {
        // When the data changes, we have to recalculate
        // all of the angles.
        // ve nguojc chieu kim dong ho
//        int currentAngle = 0;
//        float sumAngle = 0.0f;
//        for (Item it : mData) {
//            it.setmStartAngle(currentAngle);
//            //it.setmEndAngle((int) ((float) currentAngle + it.getmValue() * 360.0f / mTotal));
//            it.setmEndAngle((int) ((float) currentAngle + it.getmValue() * 360.0f / mTotal));
//            currentAngle = it.getmEndAngle();
//            float swipeAngle = currentAngle - it.getmStartAngle();
//            sumAngle+=swipeAngle;
//            Log.d("sum angle", ""+sumAngle);
//            //calculate exceed angle if the last item
//            if(it == mData.get(mData.size()-1)){
//                float exceedAngle = 360 - sumAngle;
//                it.setmEndAngle((int) (currentAngle+exceedAngle));
//                Log.d("exceed",""+exceedAngle);
//                sumAngle = 0;
//            }

        //test
        var currentAngle = 360f
        var sumAngle = 0.0f
        for (it in mData) {
            //it.setEndAngle(currentAngle)
            it.endAngle = (currentAngle.toInt())
            it.startAngle = (currentAngle - it.value * 360.0f / mTotal).toInt()
            Log.d(
                    "current angle", "currentAngle: " + currentAngle +
                    " label: " + it.label + " curr - it.getm " +
                    ((currentAngle - it.value * 360.0f / mTotal).toString() +
                            " value: " + it.value + " total: " + mTotal)
            )
            Log.d(
                    "angle",
                    "start: " + it.startAngle.toString() + " end: " + it.endAngle
                            .toString() + " label: " + it.label
            )
            currentAngle = it.startAngle.toFloat()
            val swipeAngle: Float = (it.endAngle - currentAngle)
            sumAngle += swipeAngle
            Log.d("sum angle", "" + sumAngle)
            //calculate exceed angle if the last item
//            if (it == mData.get(mData.size() - 1)) {
//                float exceedAngle = 360-sumAngle;
//                it.setmEndAngle((int) (currentAngle + exceedAngle));
//                Log.d("exceed", "" + exceedAngle +" endAngle: "+it.getmEndAngle());
//                sumAngle = 0;
//            }
            //test
            Log.d(
                    "angle",
                    "start: " + it.startAngle.toString() + " end: " + it.endAngle
                            .toString() + " label: " + it.label
            )
            it.shader = SweepGradient(
                    mPieBounds.width() / 2.0f,
                    mPieBounds.height() / 2.0f, intArrayOf(
                    it.highlight,
                    it.highlight,
                    it.color,
                    it.color
            ), floatArrayOf(
                    0f,
                    (360 - it.endAngle).toFloat() / 360.0f,
                    (360 - it.startAngle).toFloat() / 360.0f,
                    1.0f
            )
            )
        }
        calcCurrentItem()
        onScrollFinished()
    }

    /**
     * Initialize the control. This code is in a separate method so that it can be
     * called from both constructors.
     */
    private fun init() {
        // Force the background to software rendering because otherwise the Blur
        // filter won't work.
        setLayerToSW(this)

        // Set up the paint for the label text
        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint?.color = mTextColor
        if (mTextHeight == 0f) {
            mTextHeight = mTextPaint?.textSize!!
        } else {
            mTextPaint?.textSize = mTextHeight
        }

        // Set up the paint for the pie slices
        mPiePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPiePaint?.style = Paint.Style.FILL
        mPiePaint?.textSize = mTextHeight

        // Set up the paint for the shadow
        mShadowPaint = Paint(0)
        mShadowPaint?.color = Color.GRAY
        mShadowPaint?.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)

        // Add a child view to draw the pie. Putting this in a child view
        // makes it possible to draw it on a separate hardware layer that rotates
        // independently
        mPieView = PieView(context)
        addView(mPieView)
        //mPieView.rotateTo(mPieRotation);

        // The pointer doesn't need hardware acceleration, but in order to show up
        // in front of the pie it also needs to be on a separate view.
        mPointerView = PointerView(context)
        addView(mPointerView)
        resetChart()
        // Set up an animator to animate the PieRotation property. This is used to
        // correct the pie's orientation after the user lets go of it.
//        if (Build.VERSION.SDK_INT >= 11) {
//            mAutoCenterAnimator = ObjectAnimator.ofInt(PieChart.this, "PieRotation", 0);
//
//            // Add a listener to hook the onAnimationEnd event so that we can do
//            // some cleanup when the pie stops moving.
//            mAutoCenterAnimator.addListener(new Animator.AnimatorListener() {
//                public void onAnimationStart(Animator animator) {
//                }
//
//                public void onAnimationEnd(Animator animator) {
//                    mPieView.decelerate();
//                }
//
//                public void onAnimationCancel(Animator animator) {
//                }
//
//                public void onAnimationRepeat(Animator animator) {
//                }
//            });
//        }


        // Create a Scroller to handle the fling gesture.
//        if (Build.VERSION.SDK_INT < 11) {
//            mScroller = new Scroller(getContext());
//        } else {
//            mScroller = new Scroller(getContext(), null, true);
//        }
//        // The scroller doesn't have any built-in animation functions--it just supplies
//        // values when we ask it to. So we have to have a way to call it every frame
//        // until the fling ends. This code (ab)uses a ValueAnimator object to generate
//        // a callback on every animation frame. We don't use the animated value at all.
//        if (Build.VERSION.SDK_INT >= 11) {
//            mScrollAnimator = ValueAnimator.ofFloat(0, 1);
//            mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                    tickScrollAnimation();
//                }
//            });
//        }

        // Create a gesture detector to handle onTouch messages
        // mDetector = new GestureDetector(PieChart.this.getContext(), new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled,
        // you can't scroll for a bit, pause, then scroll some more (the pause is interpreted
        // as a long press, apparently)
        //mDetector.setIsLongpressEnabled(false);


        // In edit mode it's nice to have some demo data, so add that here.
        if (this.isInEditMode) {
            showText = true
            val res: Resources = resources
            addItem(Item("test", 14f, res.getColor(R.color.piechart_2)))
            addItem(Item("test", 14f, res.getColor(R.color.piechart_3)))
            addItem(Item("test", 71f, res.getColor(R.color.piechart_4)))
            //addItem(new Item("sdfs", 4, res.getColor(R.color.piechart_5)));
        }
    }

    //    private void tickScrollAnimation() {
    //        if (!mScroller.isFinished()) {
    //            mScroller.computeScrollOffset();
    //            setPieRotation(mScroller.getCurrY());
    //        } else {
    //            if (Build.VERSION.SDK_INT >= 11) {
    //                mScrollAnimator.cancel();
    //            }
    //            onScrollFinished();
    //        }
    //    }
    private fun setLayerToSW(v: View) {
        if (!v.isInEditMode && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private fun setLayerToHW(v: View) {
        if (!v.isInEditMode && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    /**
     * Force a stop to all pie motion. Called when the user taps during a fling.
     */
    private fun stopScrolling() {
        mScroller?.forceFinished(true)
        if (Build.VERSION.SDK_INT >= 11) {
            mAutoCenterAnimator?.cancel()
        }
        onScrollFinished()
    }

    /**
     * Called when the user finishes a scroll action.
     */
    private fun onScrollFinished() {
        if (mAutoCenterInSlice) {
            centerOnCurrentItem()
        } else {
            mPieView?.decelerate()
        }
    }

    /**
     * Kicks off an animation that will result in the pointer being centered in the
     * pie slice of the currently selected item.
     */
    private fun centerOnCurrentItem() {
        val current = mData[currentItem]

        var targetAngle: Int =
                current.startAngle + (current.endAngle - current.startAngle) / 2

        targetAngle -= mCurrentItemAngle
        if (targetAngle < 90 && pieRotation > 180) targetAngle += 360
        if (Build.VERSION.SDK_INT >= 11) {
            // Fancy animated version
            mAutoCenterAnimator?.setIntValues(targetAngle)
            mAutoCenterAnimator?.setDuration(AUTOCENTER_ANIM_DURATION.toLong())
                    ?.start()
        } else {
            // Dull non-animated version
            //mPieView.rotateTo(targetAngle);
        }
    }

    /**
     * Internal child class that draws the pie chart onto a separate hardware layer
     * when necessary.
     */
    private inner class PieView(context: Context?) : View(context) {
        // Used for SDK < 11
        private val mRotation = 0f
        private val mTransform: Matrix = Matrix()
        private val mPivot = PointF()
        var labelPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * Enable hardware acceleration (consumes memory)
         */
        fun accelerate() {
            setLayerToHW(this)
        }

        /**
         * Disable hardware acceleration (releases memory)
         */
        fun decelerate() {
            setLayerToSW(this)
        }

        var labelRect: Rect = Rect()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (Build.VERSION.SDK_INT < 11) {
                mTransform.set(canvas.getMatrix())
                mTransform.preRotate(mRotation, mPivot.x, mPivot.y)
                canvas.setMatrix(mTransform)
            }
            canvas.rotate(-90.0f, (width / 2).toFloat(), (height / 2).toFloat())
            if (mData.isEmpty()) {
                canvas.drawArc(
                        mBounds!!,
                        0f,
                        360f,
                        true, mPiePaint!!
                )
            }
            //ve nguoc chieu kim dong ho
//            for (Item it : mData) {
//                mPiePaint.setShader(it.getmShader());
//                canvas.drawArc(mBounds,
//                        360 - it.getmEndAngle(),
//                        it.getmEndAngle() - it.getmStartAngle(),
//                        true, mPiePaint);
//                mPiePaint.getTextBounds(it.getmLabel(), 0, it.getmLabel().length(), labelRect);
//                if(getShowText()){
//                    drawPercentText(canvas, it);
//                }
//
//            }
            for (it in mData) {
                mPiePaint?.setShader(it.shader)
                canvas.drawArc(
                        mBounds!!,
                        360f - it.startAngle,
                        (it.startAngle - it.endAngle).toFloat(),
                        true, mPiePaint!!
                )
                //                Log.d("draw arc", "start Angle: "+(360 - it.getmEndAngle()+
//                                " sweep: "+(it.getmEndAngle() - it.getmStartAngle())+
//                                " end"+it.getmEndAngle()));
                mPiePaint?.getTextBounds(it.label, 0, it.label.length, labelRect)
                if (showText) {
                    drawPercentText(canvas, it)
                }
            }


//            for(int i = mData.size()-1;i>=0;i--){
//                mPiePaint.setShader(mData.get(i).getmShader());
//                canvas.drawArc(mBounds,
//                        (360 - mData.get(i).getmEndAngle()),
//                        mData.get(i).getmEndAngle() - mData.get(i).getmStartAngle(),
//                        true, mPiePaint);
//                Log.d("draw arc", "start Angle: "+(360 - mData.get(i).getmEndAngle()+
//                                " sweep: "+(mData.get(i).getmEndAngle() - mData.get(i).getmStartAngle())+
//                                " end"+mData.get(i).getmEndAngle())+"label: "+mData.get(i).getmLabel());
//                mPiePaint.getTextBounds(mData.get(i).getmLabel(), 0, mData.get(i).getmLabel().length(), labelRect);
//                if (getShowText()) {
//                    drawPercentText(canvas, mData.get(i));
//                }
//            }
//            canvas.rotate(360-(mData.get(mData.size()-1).getmEndAngle()-mData.get(mData.size()-1).getmStartAngle()),
//                    getWidth() / 2, getHeight() / 2);
            //canvas.rotate(-90.0f, getWidth() / 2, getHeight() / 2);
            //Log.d("canvas rotate", ""+(360-(mData.get(mData.size()-1).getmEndAngle()-mData.get(mData.size()-1).getmStartAngle())));
        }

        /////////////////////////////////////////////////////////////

        /*aa
       private void drawPercentText(Canvas canvas, Item it) {
           float pieRadius = getWidth() / 2;
           float angel = (it.getStartAngle() + it.getEndAngle()) / 2;

           int sth = -1;
           if (angel % 360 > 180 && angel % 360 < 360) {
               sth = 1;
           }
           float x = (float) (getHeight() / 2 + Math.cos(Math.toRadians(-angel)) * (pieRadius / 2));
           float y = (float) (getHeight() / 2 + sth * Math.abs(Math.sin(Math.toRadians(-angel))) * (pieRadius / 2));
           canvas.save();
           canvas.rotate(90f, x, y);
           canvas.drawText(getPercentStr(it.getEndAngle() - it.getStartAngle()), x, y, labelPaint);
           canvas.restore();
       }
       private String getPercentStr(float sweepDegree) {
           float percent = sweepDegree / 360 * 100;
           if (percent == 0) return "";
           else
               return String.valueOf((int) percent) + "%";
       }
       aa*/

        private fun drawPercentText(canvas: Canvas, it: Item) {
            val pieRadius = width / 2.toFloat()
            val angel: Float = ((it.startAngle + it.endAngle) / 2).toFloat()
            var sth = -1
            if (angel % 360 > 180 && angel % 360 < 360) {
                sth = 1
            }
            //Log.d("angle", "start: " + (it.getmStartAngle()) + "end: " + it.getmEndAngle() + " angle: " + angel + " sth: " + sth);
            val x = (height / 2 + cos(Math.toRadians(-angel.toDouble())) * (pieRadius / 2)).toFloat()
            val y = (height / 2 + sth * abs(sin(Math.toRadians(-angel.toDouble()))) * (pieRadius / 2)).toFloat()
            canvas.save()
            canvas.rotate(90f, x, y)
            //canvas.drawText(getPercentStr((it.endAngle - it.startAngle).toFloat()).toString(), x, y, labelPaint)
            canvas.drawText(getPercentStr((it.endAngle - it.startAngle).toFloat()).toString(), x, y, labelPaint)
            canvas.restore()
        }

        private fun getPercentStr(sweepDegree: Float): String? {
            val percent = sweepDegree / 360 * 100
            if (percent == 0f) return ""
            else {
                //return String.valueOf((int) percent) + "%"
                return  "%d%%".format(percent.roundToInt())
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            mBounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        }

        var mBounds: RectF? = null

        //        public void rotateTo(float pieRotation) {
        //            mRotation = pieRotation;
        //            if (Build.VERSION.SDK_INT >= 11) {
        //                setRotation(pieRotation);
        //            } else {
        //                invalidate();
        //            }
        //        }
        fun setPivot(x: Float, y: Float) {
            mPivot.x = x
            mPivot.y = y
            if (Build.VERSION.SDK_INT >= 11) {
                pivotX = x
                pivotY = y
            } else {
                invalidate()
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            val height = width //calculated
            val width = measuredWidth //parent width
            setMeasuredDimension(width, height)
        }
        /**
         * Construct a PieView
         *
         * @param context
         */
        init {
            labelPaint.color = Color.WHITE
            labelPaint.textSize = 60f
        }
    }

    /**
     * View that draws the pointer on top of the pie chart
     */
    private inner class PointerView
    /**
     * Construct a PointerView object
     *
     * @param context
     */
    (context: Context?) : View(context) {
        override fun onDraw(canvas: Canvas) {
            canvas.drawLine(mTextX, mPointerY, mPointerX, mPointerY, mTextPaint!!)
            canvas.drawCircle(mPointerX, mPointerY, mPointerRadius, mTextPaint!!)
        }
    }

    /**
     * Extends [GestureDetector.SimpleOnGestureListener] to provide custom gesture
     * processing.
     */
    //    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    //        @Override
    //        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    //            // Set the pie rotation directly.
    //            float scrollTheta = vectorToScalarScroll(
    //                    distanceX,
    //                    distanceY,
    //                    e2.getX() - mPieBounds.centerX(),
    //                    e2.getY() - mPieBounds.centerY());
    //            setPieRotation(getPieRotation() - (int) scrollTheta / FLING_VELOCITY_DOWNSCALE);
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    //            // Set up the Scroller for a fling
    //            float scrollTheta = vectorToScalarScroll(
    //                    velocityX,
    //                    velocityY,
    //                    e2.getX() - mPieBounds.centerX(),
    //                    e2.getY() - mPieBounds.centerY());
    //            mScroller.fling(
    //                    0,
    //                    (int) getPieRotation(),
    //                    0,
    //                    (int) scrollTheta / FLING_VELOCITY_DOWNSCALE,
    //                    0,
    //                    0,
    //                    Integer.MIN_VALUE,
    //                    Integer.MAX_VALUE);
    //
    //            // Start the animator and tell it to animate for the expected duration of the fling.
    //            if (Build.VERSION.SDK_INT >= 11) {
    //                mScrollAnimator.setDuration(mScroller.getDuration());
    //                mScrollAnimator.start();
    //            }
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean onDown(MotionEvent e) {
    //            // The user is interacting with the pie, so we want to turn on acceleration
    //            // so that the interaction is smooth.
    //            mPieView.accelerate();
    //            if (isAnimationRunning()) {
    //                stopScrolling();
    //            }
    //            return true;
    //        }
    //    }
    //
    //    private boolean isAnimationRunning() {
    //        return !mScroller.isFinished() || (Build.VERSION.SDK_INT >= 11 && mAutoCenterAnimator.isRunning());
    //    }
    //
    //    /**
    //     * Helper method for translating (x,y) scroll vectors into scalar rotation of the pie.
    //     *
    //     * @param dx The x component of the current scroll vector.
    //     * @param dy The y component of the current scroll vector.
    //     * @param x  The x position of the current touch, relative to the pie center.
    //     * @param y  The y position of the current touch, relative to the pie center.
    //     * @return The scalar representing the change in angular position for this scroll.
    //     */
    //    private static float vectorToScalarScroll(float dx, float dy, float x, float y) {
    //        // get the length of the vector
    //        float l = (float) Math.sqrt(dx * dx + dy * dy);
    //
    //        // decide if the scalar should be negative or positive by finding
    //        // the dot product of the vector perpendicular to (x,y).
    //        float crossX = -y;
    //        float crossY = x;
    //
    //        float dot = (crossX * dx + crossY * dy);
    //        float sign = Math.signum(dot);
    //
    //        return l * sign;
    //    }
    companion object {
        /**
         * Draw text to the left of the pie chart
         */
        const val TEXTPOS_LEFT = 0
        /**
         * Draw text to the right of the pie chart
         */
        const val TEXTPOS_RIGHT = 1
        const val FLING_VELOCITY_DOWNSCALE = 4
        const val AUTOCENTER_ANIM_DURATION = 250
        private const val D_TO_R = 0.0174532925f
    }
}