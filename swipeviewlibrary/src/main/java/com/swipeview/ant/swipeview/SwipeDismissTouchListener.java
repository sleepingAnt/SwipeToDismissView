// THIS IS A BETA! I DON'T RECOMMEND USING IT IN PRODUCTION CODE JUST YET

/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.swipeview.ant.swipeview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * A {@link View.OnTouchListener} that makes any {@link View}
 * dismissable when the user swipes (drags her finger) horizontally across the
 * view.
 * <p/>
 * <p>
 * <em>For {@link android.widget.ListView} list items that don't manage their own touch events
 * (i.e. you're using
 * {@link android.widget.ListView#setOnItemClickListener(android.widget.AdapterView.OnItemClickListener)}
 * or an equivalent listener on {@link android.app.ListActivity}</em>
 * </p>
 * <p/>
 * <p>
 * Example usage:
 * </p>
 * <p/>
 * <pre>
 * view.setOnTouchListener(new SwipeDismissTouchListener(view, null, // Optional
 * 																	// token/cookie
 * 																	// object
 * 		new SwipeDismissTouchListener.OnDismissCallback() {
 * 			public void onDismiss(View view, Object token) {
 * 				parent.removeView(view);
 *            }
 *        }));
 * </pre>
 * <p/>
 * <p>
 * This class Requires API level 12 or later due to use of
 * {@link android.view.ViewPropertyAnimator}.
 * </p>
 */
public class SwipeDismissTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private View mView;
    private OnDismissCallback mCallback;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private float mDownX;
    private boolean mSwiping;
    private Object mToken;
    private VelocityTracker mVelocityTracker;
    private float mTranslationX;

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given view.
     *
     * @param view     The view to make dismissable.
     * @param token    An optional token/cookie object to be passed through to the
     *                 callback.
     * @param callback The callback to trigger when the user has indicated that she
     *                 would like to dismiss this view.
     */
    public SwipeDismissTouchListener(View view, Object token,
                                     OnDismissCallback callback) {
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = view.getContext().getResources()
                .getInteger(android.R.integer.config_shortAnimTime);
        mView = view;
        mToken = token;
        mCallback = callback;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // offset because the view is translated during swipe
        motionEvent.offsetLocation(mTranslationX, 0);

        if (mViewWidth < 2) {
            mViewWidth = mView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // TODO: ensure this is a finger, and set a flag
                mDownX = motionEvent.getRawX();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(motionEvent);
                view.onTouchEvent(motionEvent);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(mVelocityTracker.getXVelocity());
                float velocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > mViewWidth / 2) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= velocityX
                        && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
                    dismiss = true;
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
//                    animate(mView)
                    mView.animate().translationX(dismissRight ? mViewWidth : -mViewWidth)
                            .alpha(0).setDuration(mAnimationTime)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    performDismiss(mView, mAnimationTime);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                } else {
                    // cancel
                    mView.animate().translationX(0).alpha(1)
                            .setDuration(mAnimationTime).setListener(null);
                }
                mVelocityTracker = null;
                mTranslationX = 0;
                mDownX = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                if (Math.abs(deltaX) > mSlop) {
                    mSwiping = true;
                    mView.getParent().requestDisallowInterceptTouchEvent(true);

                    // Cancel view's touch
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex() <<
                                    MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mView.onTouchEvent(cancelEvent);
                }

                if (mSwiping) {
                    mTranslationX = deltaX;
                    mView.setTranslationX(deltaX);
                    // TODO: use an ease-out interpolator or such
                    mView.setAlpha(
                            Math.max(
                                    0f,
                                    Math.min(1f, 1f - 2f * Math.abs(deltaX)
                                            / mViewWidth)));
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void performDismiss(final View mView,long duration) {
        // Animate the dismissed view to zero-height and then fire the dismiss
        // callback.
        // This triggers layout on each animation frame; in the future we may
        // want to do something
        // smarter and more performant.

        final ViewGroup.LayoutParams lp = mView.getLayoutParams();
        final int originalHeight = mView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1)
                .setDuration(duration);

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mCallback.onDismiss(mView, mToken);
                // Reset view presentation
                mView.setAlpha(1f);
                mView.setTranslationX(0);
                lp.height = originalHeight;
                mView.setLayoutParams(lp);
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
     * The callback interface used by {@link SwipeDismissTouchListener} to
     * inform its client about a successful dismissal of the view for which it
     * was created.
     */
    public interface OnDismissCallback {
        /**
         * Called when the user has indicated they she would like to dismiss the
         * view.
         *
         * @param view  The originating {@link View} to be dismissed.
         * @param token The optional token passed to this object's constructor.
         */
        void onDismiss(View view, Object token);
    }
}
