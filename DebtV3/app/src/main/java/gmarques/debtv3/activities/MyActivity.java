package gmarques.debtv3.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.Timer;
import java.util.TimerTask;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.dashboard.MainActivity;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.outros.Tag;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public abstract class MyActivity extends AppCompatActivity {
    private static float ultimoToqueY;
    private static float ultimoToqueX;
    private View content;
    private boolean devoAplicarCircularReveal = true;
    protected long tempoDeEspera=800;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        devoAplicarCircularReveal = devoAplicarCircularReveal();

        if (devoAplicarCircularReveal)
            overridePendingTransition(0, R.anim.fade_out);
        else overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        super.onCreate(savedInstanceState);

        Debt.binder.get().setActivity(this);

        content = findViewById(android.R.id.content);
        if (devoAplicarCircularReveal) content.setVisibility(View.INVISIBLE);


        content.post(() -> {
            if (devoAplicarCircularReveal) {
                final View windowParent = findViewById(R.id.windowParent);
                final View parent = findViewById(R.id.parent);
                windowParent.setVisibility(View.INVISIBLE);
                parent.setVisibility(View.INVISIBLE);

                iniciarCircularReveal(windowParent, parent);
            }

            verificarModoDeTestes();
        });


    }

    /**
     * Preciso garantir de me lembrar que estou ou nao em modo de testes
     */
    private void verificarModoDeTestes() {
        if (Debt.MODO_DE_TESTES) {
            View v = findViewById(R.id.parent);
            if (v != null) {
                v.setBackgroundColor(ContextCompat.getColor(MyActivity.this, R.color.modo_de_testes));
            }
            UIUtils.infoToasty("Em modo de testes");
        }
    }

    private void iniciarCircularReveal(View windowParent, final View parent) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        parent.setY(displayMetrics.heightPixels);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(parent, "y", displayMetrics.heightPixels, 0);
        objectAnimator.setDuration(500);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.setStartDelay(250);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                parent.setVisibility(View.VISIBLE);
                super.onAnimationStart(animation);
            }
        });
        objectAnimator.start();


        int startRadius = 0;
        int endRadius = (int) Math.hypot(windowParent.getWidth(), windowParent.getHeight());
        Animator anim = ViewAnimationUtils.createCircularReveal(windowParent, (int) ultimoToqueX, (int) ultimoToqueY, startRadius, endRadius);
        anim.setDuration(500);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.start();
        windowParent.setVisibility(View.VISIBLE);
        content.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onResume() {
        Log.d(Tag.AppTag, this.getClass().getSimpleName() + ".onResume: ");

        Debt.binder.get().setAppMinimizado(false);
        if (Usuario.getUsuario() != null) {
            /*Se desinstalar o app e instalar de novo, sem ter feito loggof
            * esse trecho causava exception por isso verifico*/
            Debt.binder.get().liveSinc.inicializar();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(Tag.AppTag, this.getClass().getSimpleName() + ".onPause: ");

        Debt.binder.get().setAppMinimizado(true);
        if (devoAplicarCircularReveal)
            overridePendingTransition(0, R.anim.fade_out);
        else overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        verificarSeAppPermaneceMinimizado();

        super.onPause();
    }

    private void verificarSeAppPermaneceMinimizado() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cancel();
                if (Debt.binder.get().estaAppMinimizado()) {
                    appEstaMinimizado();
                }
            }
        }, 3000);

    }

    private void appEstaMinimizado() {
        Log.d(Tag.AppTag, getClass().getSimpleName() + ".appEstaMinimizado: ");
        Debt.binder.get().liveSinc.pararDeOuvir();
    }

    /**
     * Em algumas activities é melhor aplicar uma animaçao de fadein-out nas transiçoes pois
     * melhora o entendimento do fluxo, e a estetica do app.
     *
     * @return b
     */
    @SuppressWarnings("rawtypes")
    private boolean devoAplicarCircularReveal() {

        Class[] acts = new Class[]{
                MainActivity.class
                , Login.class
                , TermosDeUso.class
                , Falha.class
        };

        for (Class aClass : acts)
            if (this.getClass().getName().equals(aClass.getName())) return false;

        return true;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    /**
     * listener global de toques, todas as activities que herdam dessa classe reportam eventos de clique
     * por to-do o layout a esse metodo
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        ultimoToqueX = ev.getX();
        ultimoToqueY = ev.getY();
        return super.dispatchTouchEvent(ev);
    }


}
