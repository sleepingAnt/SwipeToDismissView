package com.swipeview.ant.swipeview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * TODO: document your custom view class.
 */
public class SwipeView extends FrameLayout {
    private long animationDuration = 200;
    private int currentRemovingItemPosition = -1;

    public SwipeView(Context context) {
        super(context);
    }

    public SwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * set the animation duration.
     *
     * @param duration the animation duration
     */
    public void setAnimationDuration(long duration) {
        animationDuration = duration;
    }

    /**
     * add a swipe item
     *
     * @param view custom view
     */
    public void addItem(View view, boolean canSwipe, final OnSwipeViewCallback callback) {
        addView(view, 0);
        if (canSwipe)
            view.setOnTouchListener(new SwipeDismissTouchListener(view, null, new SwipeDismissTouchListener.OnDismissCallback() {
                @Override
                public void onDismiss(View view, Object token) {
                    removeView(view);
                    callback.onSwiped();
                }
            }));
    }

    /**
     * remove the top item.
     */
    public void removeTopItem(boolean withAnimation) {
        if (getChildCount() == 0) return;
        if (currentRemovingItemPosition == -1 || currentRemovingItemPosition > getChildCount() - 1) {
            currentRemovingItemPosition = getChildCount() - 1;
        } else if (currentRemovingItemPosition <= getChildCount() - 1) {
            currentRemovingItemPosition--;
        }
        if (currentRemovingItemPosition < 0) return;
        Log.i("position", currentRemovingItemPosition + "");

        if (withAnimation) {
            final View childAt = getChildAt(currentRemovingItemPosition);
            childAt.animate()
                    .translationX(-getChildAt(0).getWidth())
                    .alpha(0).setDuration(animationDuration).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    performDismiss(childAt);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        } else
            removeViewAt(getChildCount() - 1);
    }

    /**
     * Animate the dismissed view to zero-height and then fire the dismiss
     * callback.
     * This triggers layout on each animation frame; in the future we may
     * want to do something
     * smarter and more performant.
     *
     * @param mView the view to performDismiss.
     */
    public void performDismiss(final View mView) {

        final ViewGroup.LayoutParams lp = mView.getLayoutParams();
        final int originalHeight = mView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1)
                .setDuration(animationDuration);

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset view presentation
                mView.setAlpha(1f);
                mView.setTranslationX(0);
                lp.height = originalHeight;
                mView.setLayoutParams(lp);
                SwipeView.this.removeView(mView);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                mView.setLayoutParams(lp);
            }
        });
        animator.start();
    }

    /**
     * The callback interface used by {@link SwipeView} to
     * inform its client about a successful dismissal of the view for which it
     * was created.
     */
    public interface OnSwipeViewCallback {
        /**
         * Called when the user has indicated they she would like to dismiss the
         * view.
         */
        void onSwiped();
    }
}
