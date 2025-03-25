package gmarques.debtv3.interface_.dialogo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import gmarques.debtv3.R;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;

/**
 * Criado por Gilian Marques
 * Sábado, 03 de Agosto de 2019  as 17:01:06.
 */
 class BottomSheetDialog {
    private final View rootView, animatable, actContent, parent;
    private Activity activity;
    private Callback callback;
    private Dialog mDialog;
    private TextView tvTitle, tvMessage, btnPositive, btnNegative;
    private int dismissTime = 350;
    private boolean focusable = true;
    private View bottonsContainer, titleContainer;
    LinearLayout container;
    ImageView ivIcon;
    // Gestures----------------------------------------------------------**
    private float startY;
    private float originalY;
    private float maxActContainerTop;
    private boolean cancelable = true;
    private DialogInterface.OnDismissListener dismissListener;
    private int customBg;

    // Gestures----------------------------------------------------------**
    public BottomSheetDialog(Activity activity) {
        rootView = activity.getLayoutInflater().inflate(R.layout.layout_sheettalogo, null);
        this.activity = activity;
        tvTitle = rootView.findViewById(R.id.tvTitle);
        tvMessage = rootView.findViewById(R.id.tvMessage);
        btnPositive = rootView.findViewById(R.id.btnPositive);
        btnNegative = rootView.findViewById(R.id.btnNegative);
        animatable = rootView.findViewById(R.id.animatable);
        actContent = activity.findViewById(android.R.id.content);
        parent = rootView.findViewById(R.id.parent);
        bottonsContainer = rootView.findViewById(R.id.bottonsContainer);
        titleContainer = rootView.findViewById(R.id.titleContainer);
        container = rootView.findViewById(R.id.container);
        ivIcon = rootView.findViewById(R.id.ivIcone);
        animatable.setVisibility(View.INVISIBLE);
        parent.setBackgroundColor(ContextCompat.getColor(activity, R.color.brancoA10));
        animatable.setBackground(UIUtils.aplicarTema(animatable.getBackground(), UIUtils.corAttr(android.R.attr.windowBackground)));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void makeGestureSensitive() {


        animatable.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float fingerMovement = (startY - event.getY());
                    float animatableY = animatable.getY();
                    if (animatableY - fingerMovement >= originalY) {
                        animatable.setY(animatableY - fingerMovement);
                        float ny = actContent.getY() - fingerMovement / 10;
                        if (ny <= 0) actContent.setY(ny);
                    }

                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    if (!cancelable) {
                        showByGesture();
                        return true;
                    }

                    float half = animatable.getMeasuredHeight() / 2;
                    if (animatable.getY() > originalY + half)
                        dismissByGesture();
                    else showByGesture();

                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    originalY = animatable.getY();
                    startY = event.getY();
                }

                return true;
            }
        });

    }

    public BottomSheetDialog icone(@DrawableRes int icon) {
        ivIcon.setImageResource(icon);
        ivIcon.setVisibility(View.VISIBLE);
        return this;
    }

    public BottomSheetDialog titulo(String title) {
        tvTitle.setText(title);
        tvTitle.setVisibility(View.VISIBLE);
        return this;
    }

    public BottomSheetDialog mensagem(String message) {
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
        return this;
    }

    public BottomSheetDialog botaoPositivo(String positiveText, View.OnClickListener callback) {
        btnPositive.setText(positiveText);
        btnPositive.setVisibility(View.VISIBLE);

        return this;
    }

    public BottomSheetDialog botaoNegativo(String negativeText, View.OnClickListener callback) {
        btnNegative.setText(negativeText);
        btnNegative.setVisibility(View.VISIBLE);
        return this;
    }

    public BottomSheetDialog setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public BottomSheetDialog naoCancelavel() {
        cancelable = false;
        return this;
    }

    public BottomSheetDialog semFoco() {
        focusable = false;
        parent.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.transparent));
        return this;
    }

    public BottomSheetDialog setCustomBgColor(@ColorInt int color) {
        this.customBg = color;
        animatable.setBackground(UIUtils.aplicarTema(animatable.getBackground(), color));

        return this;
    }

    public BottomSheetDialog contentView(View view, boolean paodebatata) {
        titleContainer.setVisibility(View.GONE);
        container.addView(view);
        return this;
    }

    public BottomSheetDialog contentView(View view) {
        container.addView(view);
        return this;
    }

    public void show() {


        if (btnPositive.getVisibility() == View.VISIBLE) {
            btnPositive.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {super.onClick(view);
                    dismiss();

                    Runnable mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            callback.positive();
                        }
                    };
                    new Handler().postDelayed(mRunnable, dismissTime + 100);
                }
            });
        }

        if (btnNegative.getVisibility() == View.VISIBLE) {
            btnNegative.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {super.onClick(view);
                    dismiss();

                    Runnable mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            callback.negative();
                        }
                    };
                    new Handler().postDelayed(mRunnable, dismissTime + 100);

                }
            });
        }


        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        animatable.measure(size.x, size.y);
        final int height = animatable.getMeasuredHeight();


        animatable.post(new Runnable() {
            @Override
            public void run() {

                //  Interpolator customInterpolator = PathInterpolatorCompat.create(0.200f, 0.825f, 0.000f, 0.1000f);
                //  Interpolator customInterpolator = PathInterpolatorCompat.create(1.000f, -0.600f, 0.000f, 1.650f);


                ValueAnimator valueAnimator = ObjectAnimator.ofFloat(animatable, "y", animatable.getY() + height, animatable.getY());
                valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
                valueAnimator.setDuration(300);
                valueAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        makeGestureSensitive();

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                valueAnimator.start();

                maxActContainerTop = -actContent.getMeasuredHeight() / 40;

                ValueAnimator actValueAnimator = ObjectAnimator.ofFloat(actContent, "y", actContent.getY(), maxActContainerTop);
                actValueAnimator.setInterpolator(new FastOutSlowInInterpolator());
                actValueAnimator.setDuration(300);
                actValueAnimator.start();

                animatable.setVisibility(View.VISIBLE);
            }
        });


        if (focusable) {

                parent.setBackgroundColor(ContextCompat.getColor(activity, R.color.brancoA10));

            AlphaAnimation animation = new AlphaAnimation(0, 1);
            animation.setDuration(300);
            parent.startAnimation(animation);
            parent.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {super.onClick(view);

                    if (cancelable) dismiss();
                }
            });
        }


        mDialog = new Dialog(activity, R.style.FullScreenDialogStyle);
        mDialog.setContentView(rootView);
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (dismissListener != null) dismissListener.onDismiss(dialog);
                dismiss();
            }
        });
        Window window = mDialog.getWindow();

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;

        window.setAttributes(params);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(customBg == 0 ? UIUtils.corAttr(android.R.attr.windowBackground) : customBg);

        if (!focusable) window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !App.darkTheme) {
        // a view dentro do dialog n responde ao toque
        //  window.addFlags(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        //}

        mDialog.setCancelable(cancelable);
        mDialog.show();


    }

    public void dismiss() {

        ValueAnimator actValueAnimator = ObjectAnimator.ofFloat(actContent, "y", actContent.getY(), 0);
        actValueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        actValueAnimator.setDuration(300);
        actValueAnimator.start();


        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(animatable, "y", +animatable.getY(), animatable.getY() + animatable.getMeasuredHeight());
        valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        valueAnimator.setDuration(dismissTime);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mDialog.dismiss();
            }
        });
        valueAnimator.start();

        if (focusable) {
            AlphaAnimation animation = new AlphaAnimation(1, 0);
            animation.setDuration(300);
            animation.setFillAfter(true);
            parent.startAnimation(animation);

        }


    }

    private void dismissByGesture() {

        ValueAnimator actValueAnimator = ObjectAnimator.ofFloat(actContent, "y", actContent.getY(), 0);
        actValueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        actValueAnimator.setDuration(200);
        actValueAnimator.start();


        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(animatable, "y", +animatable.getY(), animatable.getY() + animatable.getMeasuredHeight());
        valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        valueAnimator.setDuration(200);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mDialog.dismiss();
            }
        });
        valueAnimator.start();


        if (focusable) {
            AlphaAnimation animation = new AlphaAnimation(1, 0);
            animation.setDuration(300);
            animation.setFillAfter(true);
            parent.startAnimation(animation);

        }
    }

    private void showByGesture() {
        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(animatable, "y", animatable.getY(), originalY);
        valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        valueAnimator.setDuration(300);
        valueAnimator.start();


        ValueAnimator actValueAnimator = ObjectAnimator.ofFloat(actContent, "y", actContent.getY(), maxActContainerTop
        );
        actValueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        actValueAnimator.setDuration(300);
        actValueAnimator.setStartDelay(valueAnimator.getDuration()/2);
        actValueAnimator.start();
    }

    public BottomSheetDialog setOnDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
        return this;
    }


    public interface Callback {
        void positive();

        void negative();

    }

}
