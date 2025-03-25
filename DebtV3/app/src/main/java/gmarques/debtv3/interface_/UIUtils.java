package gmarques.debtv3.interface_;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Html;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.math.BigDecimal;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;
import gmarques.debtv3.Debt;
import gmarques.debtv3.R;

/**
 * Criado por Gilian Marques
 * Sexta-feira, 19 de Julho de 2019  as 23:22:35.
 */
public class UIUtils {

    /***
     *  0 interaçao
     *  1 sucesso
     *  2 erro
     * */
    public static void vibrar(int tipo) {
        Vibrator v = (Vibrator) Debt.binder.get().getSystemService(Context.VIBRATOR_SERVICE);
        if (tipo == 0)
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_TICK));
        else if (tipo == 1)
            v.vibrate(VibrationEffect.createWaveform(new long[]{25, 25, 25, 25, 25, 25}, VibrationEffect.DEFAULT_AMPLITUDE));
        else if (tipo == 2)
            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.EFFECT_HEAVY_CLICK));
    }


    public static int corAttr(int reference) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = Debt.binder.get().activity().getTheme();
        theme.resolveAttribute(reference, typedValue, true);
        return typedValue.data;
    }

    public static int cor(int reference) {
        return ContextCompat.getColor(Debt.binder.get(), reference);
    }

    @ColorInt
    public static int corComTransaparencia(int cor, float factor) {
        cor = corAttr(cor);
        cor = mudarTransaprencia(cor, factor);
        return cor;
    }

    /**
     * @param color  c
     * @param factor the higher the transparent color will be (0.9 almost invisible)
     * @return x
     */
    @ColorInt
    public static int mudarTransaprencia(int color, float factor) {

        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb(1 - alpha, red, green, blue);

    }

    public static void mudarCor(final TextView btnPaydOut, int from, int to) {
        ValueAnimator animator = ValueAnimator.ofArgb(from, to);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                btnPaydOut.setTextColor((int) valueAnimator.getAnimatedValue());
            }
        });
        animator.start();

    }

    /**
     * @param px amount to convert
     * @return the value received in dp
     */
    public static float dp(final float px) {
        return new BigDecimal(Debt.binder.get().getResources().getDimension(R.dimen.dp_base)).multiply(new BigDecimal(px)).floatValue();
    }

    /**
     * @param px amount to convert
     * @return the value received in sp
     */
    public static float sp(final float px) {
        return new BigDecimal(Debt.binder.get().getResources().getDimension(R.dimen.sp_base)).multiply(new BigDecimal(px)).floatValue();
    }

    public static Drawable aplicarTema(Drawable image, int cor) {
        if (image == null) return null;
        image.mutate();
        PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP);
        image.setColorFilter(porterDuffColorFilter);
        return image;

    }

    public static Drawable aplicarTema(@DrawableRes int image, @ColorInt int cor) {
        Drawable draw = ContextCompat.getDrawable(Debt.binder.get(), image);
        return aplicarTema(draw, cor);

    }

    public static void mostrarTeclado(View view) {

        new Handler().postDelayed(() -> Debt.binder.get().runOnUiThread(() -> {
            MotionEvent motionEventDown = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100, MotionEvent.ACTION_DOWN, view.getMeasuredWidth(), view.getMeasuredHeight() / 2, 0);
            view.dispatchTouchEvent(motionEventDown);
            motionEventDown.recycle();

            MotionEvent motionEventUP = MotionEvent.obtain(SystemClock.uptimeMillis() + 200, SystemClock.uptimeMillis() + 300, MotionEvent.ACTION_UP, view.getMeasuredWidth(), view.getMeasuredHeight() / 2, 0);
            view.dispatchTouchEvent(motionEventUP);
            motionEventUP.recycle();
        }), 300);}


    public static void esconderTeclado() {

        InputMethodManager imm = (InputMethodManager) Debt.binder.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(Debt.binder.get().activity().findViewById(android.R.id.content).getWindowToken(),
                    0);
        }
    }


    /**
     * @param color  to manipulate
     * @param factor 1.0f nothing changes <1.0f darker >1.0f lighter
     * @return manipulated color
     */
    public static int manipularCor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

    public static ArrayList<Integer> getCores() {
        ArrayList<Integer> colors = new ArrayList<>();
        Context context = Debt.binder.get().getApplicationContext();

        colors.add(ContextCompat.getColor(context, R.color.flat_color_1));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_2));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_3));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_4));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_5));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_6));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_7));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_8));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_9));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_10));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_11));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_12));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_13));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_14));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_15));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_16));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_17));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_18));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_19));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_21));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_22));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_23));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_24));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_25));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_26));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_27));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_28));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_29));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_30));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_31));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_32));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_33));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_34));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_35));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_36));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_37));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_38));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_39));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_40));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_41));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_42));
        colors.add(ContextCompat.getColor(context, R.color.flat_color_43));

        return colors;
    }


    public static void sucessoToasty(String message) {
        Toasty.success(Debt.binder.get(), message, Toast.LENGTH_LONG, true).show();
        vibrar(1);
    }

    public static void erroToasty(String message) {
        Toasty.error(Debt.binder.get(), message, Toast.LENGTH_LONG, true).show();
        vibrar(2);
    }

    public static void avisoToasty(String message) {
        Toasty.warning(Debt.binder.get(), message, Toast.LENGTH_LONG, true).show();
    }


    public static void erroNoFormulario(View alvo) {
        erroToasty(Debt.binder.get().getString(R.string.Verifiqueainformacaoinserida));
        vibrar(2);
        YoYo.with(Techniques.Shake).duration(800).interpolate(new FastOutLinearInInterpolator()).playOn(alvo);

    }

    public static void infoToasty(String message) {
        Toasty.info(Debt.binder.get(), message, Toast.LENGTH_LONG, true).show();

    }

    public static void dialogo(Activity activity, String titulo, String msg) {
        AlertDialog d = new AlertDialog.Builder(activity)
                .setTitle(titulo)
                .setMessage(Html.fromHtml(msg, Html.FROM_HTML_MODE_COMPACT))
                .create();
        Window window = d.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }
        d.show();

    }
}
