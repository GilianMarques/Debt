package gmarques.debtv3.outros;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import gmarques.debtv3.outros.Tag;

public class ViewTransicionador {
    private int maxWidth;
    private int maxHeight;
    private View alvo;
    private float viewX;
    private float viewY;
    private float viewW;
    private float viewH;
    private boolean expandido;
    private long animDur = 800;

    public ViewTransicionador(final View alvo, final Activity activity) {
        this.alvo = alvo;


        alvo.post(new Runnable() {
            @Override
            public void run() {
                viewW = alvo.getMeasuredWidth();
                viewH = alvo.getMeasuredHeight();

                int[] pos = new int[2];
                alvo.getLocationOnScreen(pos);

                viewX = pos[0];
                viewY = pos[1];


                DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                ViewGroup.MarginLayoutParams mParams = (ViewGroup.MarginLayoutParams) alvo.getLayoutParams();

                maxWidth = displayMetrics.widthPixels + mParams.getMarginEnd() + mParams.getMarginStart();
                maxHeight = displayMetrics.heightPixels - mParams.topMargin - mParams.bottomMargin;

                print(viewX, viewY, viewW, viewH);
            }
        });


    }

    private void print(float viewX, float viewY, float viewW, float viewH) {
        Log.d(Tag.AppTag, "print() called with: viewX = [" + viewX + "], viewY = [" + viewY + "], viewW = [" + viewW + "], viewH = [" + viewH + "]");
    }

    public void expendir() {
        expandido = true;


        final ValueAnimator wAnim = ValueAnimator.ofFloat(viewW, maxWidth);
        wAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = alvo.getLayoutParams();
                params.width = (int) animVal;
                alvo.setLayoutParams(params);
            }
        });


        final ValueAnimator xAnim = ValueAnimator.ofFloat(viewX, 0);
        xAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                alvo.setX(animVal);
            }
        });

        definirDuracaoInterpoladorEIniciar(wAnim, xAnim);
    }

    private void definirDuracaoInterpoladorEIniciar(@NonNull ValueAnimator... anims) {

        for (ValueAnimator animator : anims) {
            animator.setDuration(animDur);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
        }
    }

    public void retrair() {
        expandido = false;


        final ValueAnimator wAnim = ValueAnimator.ofFloat(maxWidth, viewW);
        wAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = alvo.getLayoutParams();
                params.width = (int) animVal;
                alvo.setLayoutParams(params);
            }
        });


        final ValueAnimator xAnim = ValueAnimator.ofFloat(0, viewX);
        xAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                alvo.setX(animVal);
            }
        });

        definirDuracaoInterpoladorEIniciar(wAnim, xAnim);

    }

    public boolean expandido() {
        return expandido;
    }
}
