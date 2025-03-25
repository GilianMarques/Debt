package gmarques.debtv3.novo_bsheet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.animation.PathInterpolatorCompat;

import gmarques.debtv3.R;
import gmarques.debtv3.interface_.UIUtils;

@SuppressWarnings("unused")
public class Sheettalogo extends AlertDialog {


    private int alturaDaTela;
    private Activity activity;
    private View dialogView;
    private View usuarioView;
    /*Limites de até onde a view pode ser movida pelo usuario*/
    private float yTopoDaTela;/*qto maior a altura do dialogo menor será seu valor*/
    private float yFundoDaTela;/*geralmente é = ao tamanho da tela, uma ve que a view vem de baixo pra cima*/
    private Interpolator interpolator;
    private View fundoAnimavel;
    private boolean exibindo;
    private int tempoAnimaçao = 300;
    private boolean dispensasAoCliqueDosBotoes = true;
    private boolean cancelavel = true;


    public Sheettalogo(Activity activity) {
        super(activity, R.style.FullScreenDialogStyle);
        this.activity = activity;
        inicializar();

    }


    private void inicializar() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        alturaDaTela = displayMetrics.heightPixels;

        dialogView = this.activity.getLayoutInflater().inflate(R.layout.layout_sheettalogo, null, false);
        fundoAnimavel = dialogView.findViewById(R.id.fundo);
        interpolator = interpolar(0.495, -0.005, 0.485, 1.030);
        dialogView.setVisibility(View.INVISIBLE);
        inicializarViews();
    }


    /**
     * Poe as views no lugar.
     * Insere no  FrameLayout criado em onCreateView (em tempo de execução) a view do dialogo
     * que foi inflada
     */
    @SuppressLint("InflateParams")
    public void inicializarViews() {
        FrameLayout dialogContainer = new FrameLayout(activity);
        dialogContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setView(dialogContainer);
        dialogContainer.addView(dialogView);

    }

    public Sheettalogo botaoPositivo(@NonNull String texto, @Nullable View.OnClickListener callback) {
        dialogView.findViewById(R.id.containerBotoes).setVisibility(View.VISIBLE);
        Button btnPos = dialogView.findViewById(R.id.btnPositivo);
        btnPos.setVisibility(View.VISIBLE);
        btnPos.setText(texto);
        btnPos.setOnClickListener(v -> {
            if (dispensasAoCliqueDosBotoes) {
                dispensarComAnimaçao();
            }
            new Handler().postDelayed(() -> {
                if (callback != null) callback.onClick(v);
            }, tempoAnimaçao);

        });

        /*Caso mude a altura dos botes atualize manualmente este valor para os tres botoes*/
        View container = dialogView.findViewById(R.id.viewContainer);
        if (container.getPaddingBottom() == 0)
            container.setPadding(0, 0, 0, (int) UIUtils.dp(40 + 16/*tamanho dos btn + margins/paddings*/));

        return this;
    }

    public Sheettalogo botaoNegativo(@NonNull String texto, @Nullable View.OnClickListener callback) {
        dialogView.findViewById(R.id.containerBotoes).setVisibility(View.VISIBLE);
        Button btnNegativo = dialogView.findViewById(R.id.btnNegativo);
        btnNegativo.setVisibility(View.VISIBLE);
        btnNegativo.setText(texto);
        btnNegativo.setOnClickListener(v -> {
            if (dispensasAoCliqueDosBotoes) {
                dispensarComAnimaçao();
            }

            new Handler().postDelayed(() -> {
                if (callback != null) callback.onClick(v);
            }, tempoAnimaçao);

        });

        /*Caso mude a altura dos botes atualize manualmente este valor para os tres botoes*/
        View container = dialogView.findViewById(R.id.viewContainer);
        if (container.getPaddingBottom() == 0)
            container.setPadding(0, 0, 0, (int) UIUtils.dp(40 + 16/*tamanho dos btn + margins/paddings*/));

        return this;
    }

    public Sheettalogo botaoNeutro(@NonNull String texto, @Nullable View.OnClickListener callback) {
        dialogView.findViewById(R.id.containerBotoes).setVisibility(View.VISIBLE);
        Button btnNeutro = dialogView.findViewById(R.id.btnNeutro);
        btnNeutro.setVisibility(View.VISIBLE);
        btnNeutro.setText(texto);
        btnNeutro.setOnClickListener(v -> {

            if (dispensasAoCliqueDosBotoes) {
                dispensarComAnimaçao();
            }
            new Handler().postDelayed(() -> {
                if (callback != null) callback.onClick(v);
            }, tempoAnimaçao);

        });

        /*Caso mude a altura dos botes atualize manualmente este valor para os tres botoes*/
        View container = dialogView.findViewById(R.id.viewContainer);
        if (container.getPaddingBottom() == 0)
            container.setPadding(0, 0, 0, (int) UIUtils.dp(40 + 16/*tamanho dos btn + margins/paddings*/));
        return this;
    }

    public Sheettalogo titulo(String texto) {
        dialogView.findViewById(R.id.tituloContainer).setVisibility(View.VISIBLE);
        dialogView.findViewById(R.id.ivIcone).setVisibility(View.INVISIBLE);
        TextView tvTitulo = dialogView.findViewById(R.id.tvTitulo);
        tvTitulo.setText(texto);
        tvTitulo.setVisibility(View.VISIBLE);
        return this;
    }

    public Sheettalogo mensagem(String texto) {
        dialogView.findViewById(R.id.tituloContainer).setVisibility(View.VISIBLE);
        TextView tvMensagem = dialogView.findViewById(R.id.tvMensagem);
        tvMensagem.setText(texto);
        tvMensagem.setVisibility(View.VISIBLE);
        return this;
    }

    public Sheettalogo icone(@DrawableRes int icone) {
        dialogView.findViewById(R.id.tituloContainer).setVisibility(View.VISIBLE);
        ImageView ivIcone = dialogView.findViewById(R.id.ivIcone);
        ivIcone.setImageResource(icone);
        ivIcone.setVisibility(View.VISIBLE);
        return this;
    }

    public Sheettalogo naoDispensarAoCliqueDosBotoes() {
        dispensasAoCliqueDosBotoes = false;
        return this;
    }

    public boolean estaExibindo() {
        return exibindo;
    }

    public void show() {
        super.show();
        exibirComAnimaçao();
    }

    @SuppressWarnings("ConstantConditions")
    public Sheettalogo semFoco() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return this;
    }

    public Sheettalogo contentView(View usuarioView) {
        this.usuarioView = usuarioView;
        ((FrameLayout) dialogView.findViewById(R.id.viewContainer)).addView(usuarioView);

        return this;
    }

    public Sheettalogo onDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        setOnDismissListener(onDismissListener);
        return this;
    }

    public Sheettalogo naoCancelavel() {
        setCancelable(false);
        cancelavel = false;
        return this;
    }

    /**
     * É aqui onde é setado o View.OnTouchListener na view do dialogo para permitir o drag'nDrop e
     * aqui tbm tem o codigo pra deixar a view do dialogo visivel
     */
    private void exibirComAnimaçao() {
        fundoAnimavel.post(() -> {
            exibindo = true;
            ObjectAnimator animator = ObjectAnimator.ofFloat(fundoAnimavel, "y", alturaDaTela, fundoAnimavel.getY());
            animator.setDuration(tempoAnimaçao);
            animator.setInterpolator(interpolator);
            animator.addUpdateListener(animation -> {
                float animVal = (float) animation.getAnimatedValue();
                /*Esse valor geralmente é equivalente ao tamanho ta tela pq o layout vem debaixo pra cima
                 * sempre, ele nao surge do meio da tela pra cima por exemplo*/
                if (yFundoDaTela == 0) {
                    yFundoDaTela = animVal;
                    /*me aproveito desse trecho de codigo pra n ter que definir um listsner na animaçao so pra
                     * poder deixar a view visivel*/
                    dialogView.setVisibility(View.VISIBLE);
                }
                /*Se o dialogo se estende por toda tela, ao fim da animaçao ele tera um Y=0
                 * e é esse valor que será atribuido a variavel yTopoDaTela*/
                yTopoDaTela = animVal;
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    fundoAnimavel.setOnTouchListener(new ArrastarEsoltar(fundoAnimavel));
                    dialogView.findViewById(R.id.parent).setOnClickListener(v -> {
                        if (cancelavel) dispensarComAnimaçao();
                    });
                }
            });

            animator.start();


        });
    }


    @SuppressWarnings("SameParameterValue")
    private Interpolator interpolar(double v, double v1, double v2, double v3) {
        return PathInterpolatorCompat.create((float) v, (float) v1, (float) v2, (float) v3);
    }

    private void exibirAposUsuarioInteragir() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(fundoAnimavel, "y", fundoAnimavel.getY(), yTopoDaTela);
        animator.setDuration(tempoAnimaçao / 2);
        animator.setInterpolator(interpolator);
        animator.start();

    }

    public void dispensarComAnimaçao() {
        exibindo = false;
        ObjectAnimator animator = ObjectAnimator.ofFloat(fundoAnimavel, "y", fundoAnimavel.getY(), alturaDaTela);
        animator.setDuration(tempoAnimaçao);
        animator.setInterpolator(interpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Sheettalogo.super.dismiss();
                super.onAnimationEnd(animation);
            }
        });
        animator.start();

    }

    @Override
    public void dismiss() {
        dispensarComAnimaçao();
    }


    /**
     * É aqui onde acontecem os calculos para permitir a animaçao da view
     */
    private class ArrastarEsoltar implements View.OnTouchListener {
        private View fundo;

        private ArrastarEsoltar(View fundo) {
            this.fundo = fundo;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                aplicarMovimento(event);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                definirSeDeveContinuarExibindoOuDispensarAview();
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                yInicialVariavelDoDedo = event.getY();
            }
            return true;
        }


        /*Essa variavel tem seu valor alterado a cada chamada em aplicarMovimento*/
        private float yInicialVariavelDoDedo;


        private void aplicarMovimento(MotionEvent event) {
            float movimentoYdoDedo = event.getY() - yInicialVariavelDoDedo;
            if ((fundo.getY() + movimentoYdoDedo) < Sheettalogo.this.yFundoDaTela && (fundo.getY() + movimentoYdoDedo) > Sheettalogo.this.yTopoDaTela)
                fundo.setY(fundo.getY() + movimentoYdoDedo);

            yInicialVariavelDoDedo = event.getY();
        }

        // TODO: 14/06/2020 definir uns interpolators
        private void definirSeDeveContinuarExibindoOuDispensarAview() {

            /*Se o movimento y > 1 terço do tamanho do dialogo*/
            if ((fundo.getY() - yTopoDaTela) > (alturaDaTela - yTopoDaTela) / 3)
                dispensarComAnimaçao();
            else exibirAposUsuarioInteragir();
        }


    }

}
