package gmarques.debtv3.activities.dashboard;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.add_edit_categorias.AddEditCategoria;
import gmarques.debtv3.activities.add_edit_despesas.AddEditDespesas;
import gmarques.debtv3.activities.add_edit_receitas.AddEditReceitas;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;

public class FloatMenu {
    private Callback callback;
    private MainActivity mainActivity;
    private int delayParaAcao = 280;
    private boolean exibindo;

    private float menuViewWidth;
    private float menuViewHeight;
    private float containerWidth = UIUtils.dp(56);
    private float containerHeight = UIUtils.dp(56);

    private LinearLayout container;
    private ImageView ivMais;
    private View menuView;
    private Point tamanhoDaTela;

    public FloatMenu(Callback callback, MainActivity mainActivity) {
        this.callback = callback;
        this.mainActivity = mainActivity;
    }

    public void inicializar(LinearLayout container) {
        this.container = container;
        menuView = container.findViewById(R.id.floatMenuView);
        ivMais = container.findViewById(R.id.ivMais);

        menuView.post(new Runnable() {
            @Override
            public void run() {
                inicializarView();
                calcularTamanhoDaViewDeMenu();
            }
        });
    }

    private void inicializarView() {


        View verDespesas = menuView.findViewById(R.id.llDespesa);
        View verReceitas = menuView.findViewById(R.id.llReceita);
        View gerenciarCategorias = menuView.findViewById(R.id.llCategoria);

        verDespesas.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.startActivity(new Intent(mainActivity, AddEditDespesas.class));
                    }
                };
                new Handler().postDelayed(runnable, delayParaAcao);

            }
        });

        verReceitas.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.startActivity(new Intent(mainActivity, AddEditReceitas.class));
                    }
                };
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

        gerenciarCategorias.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.startActivity(new Intent(mainActivity, AddEditCategoria.class));
                    }
                };
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

        container.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
            }
        });
    }

    private void calcularTamanhoDaViewDeMenu() {

        Display display = mainActivity.getWindowManager().getDefaultDisplay();
        tamanhoDaTela = new Point();
        display.getSize(tamanhoDaTela);

        menuView.measure(tamanhoDaTela.x, tamanhoDaTela.y);

        menuViewWidth = menuView.getMeasuredWidth();
        float widthComPadding = tamanhoDaTela.x - UIUtils.dp(16);
        if (menuViewWidth > widthComPadding) menuViewWidth = widthComPadding;

        menuViewHeight = menuView.getMeasuredHeight();

        container.removeView(menuView);


    }

    public void exibirDispensarMenu() {

        if (exibindo) dispensarMenu();
        else mostrarMenu();

        exibindo = !exibindo;


    }

    private int animDur = 350;

    private void dispensarMenu() {
        callback.menuFechado();
        container.removeAllViews();
        container.addView(ivMais);


        final ValueAnimator hAnim = ValueAnimator.ofFloat(menuViewHeight, containerHeight);
        hAnim.setInterpolator(new FastOutSlowInInterpolator());
        hAnim.setDuration(animDur);
        hAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = container.getLayoutParams();
                params.height = (int) animVal;
                container.setLayoutParams(params);
            }
        });

        hAnim.start();


        final ValueAnimator wAnim = ValueAnimator.ofFloat(menuViewWidth, containerWidth);
        wAnim.setInterpolator(new FastOutSlowInInterpolator());
        wAnim.setDuration(animDur);
        wAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = container.getLayoutParams();
                params.width = (int) animVal;
                container.setLayoutParams(params);
                container.setX((tamanhoDaTela.x - container.getMeasuredWidth())/2);
            }
        });
        wAnim.start();


        final ValueAnimator alphaAnim = ValueAnimator.ofFloat(1, 0);
        alphaAnim.setInterpolator(new FastOutSlowInInterpolator());
        alphaAnim.setDuration(animDur);
        alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                menuView.setAlpha(animVal);
                ivMais.setAlpha(1 - animVal);
            }
        });

        alphaAnim.start();

    }

    private void mostrarMenu() {
        callback.menuAberto();
        container.removeAllViews();
        container.addView(menuView);


        final ValueAnimator hAnim = ValueAnimator.ofFloat(containerHeight, menuViewHeight);
        hAnim.setDuration(animDur);
        hAnim.setInterpolator(new FastOutSlowInInterpolator());
        hAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = container.getLayoutParams();
                params.height = (int) animVal;
                container.setLayoutParams(params);


            }
        });

        hAnim.start();


        final ValueAnimator wAnim = ValueAnimator.ofFloat(containerWidth, menuViewWidth);
        wAnim.setDuration(animDur);
        wAnim.setInterpolator(new FastOutSlowInInterpolator());
        wAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = container.getLayoutParams();
                params.width = (int) animVal;
                container.setLayoutParams(params);
                container.setX((tamanhoDaTela.x - container.getMeasuredWidth())/2);
            }
        });

        wAnim.start();

        final ValueAnimator alphaAnim = ValueAnimator.ofFloat(1, 0);
        alphaAnim.setDuration(animDur * 2);
        alphaAnim.setInterpolator(new FastOutSlowInInterpolator());
        alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                ivMais.setAlpha(animVal);
                menuView.setAlpha(1 - animVal);
            }
        });

        alphaAnim.start();


    }

    public boolean estaAberto() {
        return exibindo;
    }

    public interface Callback {
        void menuAberto();

        void menuFechado();
    }
}
