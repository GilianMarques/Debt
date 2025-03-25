package gmarques.debtv3.activities.dashboard;

import static gmarques.debtv3.gestores.Meses.mesAtual;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.List;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.BackupERestauraçao;
import gmarques.debtv3.activities.ExportarRelatorio;
import gmarques.debtv3.activities.NubankActivity;
import gmarques.debtv3.activities.VerContas;
import gmarques.debtv3.activities.add_edit_categorias.VerCategorias;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.sincronismo.ServicoDeSincronismo;

public class MainActivityMenu {
    private Callback callback;
    private MainActivity mainActivity;
    private int delayParaAcao = 350;
    private boolean exibindo;

    private float menuViewWidth;
    private float menuViewHeight;
    private float containerWidth = UIUtils.dp(40);
    private float containerHeight = UIUtils.dp(40);

    private LinearLayout container;
    private ImageView ivMenu;
    private ConstraintLayout menuView;

    private TextView tvVerContas;
    private TextView tvVerCategorias;
    private TextView tvImportarNubank;
    private TextView trocarDeMes;
    private TextView exportarRelatorio;
    private TextView sincronizar;
    private TextView suspenderAnuncios;
    private TextView backup;

    public MainActivityMenu(Callback callback, MainActivity mainActivity) {
        this.callback = callback;
        this.mainActivity = mainActivity;
    }

    public void inicializar(LinearLayout container) {
        this.container = container;
        menuView = container.findViewById(R.id.menuView);
        ivMenu = container.findViewById(R.id.ivMenu);

        menuView.post(() -> {
            inicializarViews();
            aplicarRestriçoes();
            definirListeners();
            calcularTamanhoDaViewDeMenu();
        });
    }

    private void inicializarViews() {

        tvImportarNubank = menuView.findViewById(R.id.tvImportarNubank);
        tvVerContas = menuView.findViewById(R.id.tvVerContas);
        tvVerCategorias = menuView.findViewById(R.id.tvVerCategorias);
        trocarDeMes = menuView.findViewById(R.id.trocarDeMes);
        exportarRelatorio = menuView.findViewById(R.id.exportarRelatorio);
        sincronizar = menuView.findViewById(R.id.sincronizar);
        suspenderAnuncios = menuView.findViewById(R.id.suspenderAnuncios);
        backup = menuView.findViewById(R.id.backup);
    }

    /**
     * Modifica o menu de acordo com algumas condiçoes
     */
    private void aplicarRestriçoes() {

        if (Debt.DESLIGAR_ADS) suspenderAnuncios.setVisibility(View.GONE);
            }

    private void definirListeners() {

        container.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
            }
        });

        tvVerContas.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = () -> mainActivity.startActivity(new Intent(mainActivity, VerContas.class));
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

        tvVerCategorias.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = () -> mainActivity.startActivity(new Intent(mainActivity, VerCategorias.class));
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

        trocarDeMes.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View v) {
                super.onClick(v);

                exibirDispensarMenu();
                /*------------------------------------------------------*/
                final Sheettalogo dialogo = new Sheettalogo(mainActivity);
                final FlexboxLayout flexboxLayout = new FlexboxLayout(mainActivity);
                flexboxLayout.setFlexWrap(FlexWrap.WRAP);
                flexboxLayout.setFlexDirection(FlexDirection.ROW);
                flexboxLayout.setJustifyContent(JustifyContent.FLEX_START);
                flexboxLayout.setLayoutTransition(new LayoutTransition());

                List<Mes> meses = Meses.getTodosOsMeses();
                meses.sort((mes, t1) -> t1.getMes() - mes.getMes());
                meses.sort((mes, t1) -> t1.getAno() - mes.getAno());


                for (final Mes mes : meses) {
                    final View view = mainActivity.getLayoutInflater().inflate(R.layout.layout_mes_troca_rapida_de_mes, null, false);
                    TextView tvNome = view.findViewById(R.id.tvNome);
                    String mesNome = mes.getNome().toUpperCase();
                    if (mesNome.contains("/"))
                        tvNome.setText(mesNome.substring(0, 3).concat("/").concat(mesNome.substring(mesNome.length() - 2)));
                    else tvNome.setText(mesNome.substring(0, 3));

                    tvNome.setOnClickListener(new AnimatedClickListener() {
                        @Override
                        public void onClick(View view) {
                            super.onClick(view);
                            dialogo.dismiss();
                            mainActivity.trocarMesEAtualizarUI(mes);
                        }
                    });
                    flexboxLayout.addView(view);
                    flexboxLayout.setPadding(0, 0, 0, (int) UIUtils.dp(24));
                }


                dialogo.contentView(flexboxLayout)
                        .titulo("Selecione um mês")
                        .icone(R.drawable.vec_info)
                        .show();

                /*------------------------------------------------------*/

            }
        });

        exportarRelatorio.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.startActivity(new Intent(mainActivity, ExportarRelatorio.class));
                    }
                };
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

        sincronizar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = () -> {

                    mainActivity.startForegroundService(new Intent(mainActivity, ServicoDeSincronismo.class));
                    new ServicoDeSincronismo.Callback() {
                        @Override
                        public void feito(boolean sucesso, String mensagem) {
                            if (sucesso) {
                                UIUtils.sucessoToasty(mainActivity.getString(R.string.Sincronismoconcluidopodesernecessarioreiniciar));
                            } else
                                UIUtils.erroToasty(mainActivity.getString(R.string.Sincronismofalhou) + "\n" + mensagem);
                            /*recarrego o objeto mesAtual do DB  assim recarrego os array de receitas e despesas do mes*/
                            mesAtual = Meses.getMes(mesAtual.getMes(), mesAtual.getAno());
                            Broadcaster.atualizarMainActivity();
                        }
                    };
                };

                long dataLocal = Prefs.getLong(ServicoDeSincronismo.ultimaTentativaDeSincronismo, 0);
                // // TODO: 24/01/2021 verificar se deo bloquear ou se posso permitir sincronismo a qualquer hora
                //  if (Data.timeStampUTC() - dataLocal > 60 * 60 * 1000)
                new Handler().postDelayed(runnable, delayParaAcao);
                // else UIUtils.infoToasty("você não pode sincronizar agora, por favor aguarde");
            }
        });

        backup.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = () -> mainActivity.startActivity(new Intent(mainActivity, BackupERestauraçao.class));
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

        tvImportarNubank.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDispensarMenu();
                Runnable runnable = () -> mainActivity.startActivity(new Intent(mainActivity, NubankActivity.class));
                new Handler().postDelayed(runnable, delayParaAcao);
            }
        });

    }

    private void atualizarEstadodasViews() {
    }

    private void calcularTamanhoDaViewDeMenu() {

        Display display = mainActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        menuView.measure(size.x, size.y);

        menuViewWidth = menuView.getMeasuredWidth();
        menuViewHeight = menuView.getMeasuredHeight();

        container.removeView(menuView);


    }

    public void exibirDispensarMenu() {

        if (exibindo) dispensarMenu();
        else mostrarMenu();

        exibindo = !exibindo;
    }

    private int animDur = 200;
    private int animDur2 = 350;

    private void dispensarMenu() {
        callback.menuFechado();
        container.removeAllViews();
        container.addView(ivMenu);


        final ValueAnimator hAnim = ValueAnimator.ofFloat(menuViewHeight, containerHeight);
        hAnim.setInterpolator(new AccelerateInterpolator());
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
        wAnim.setInterpolator(new DecelerateInterpolator());
        wAnim.setDuration(animDur2);
        wAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animVal = (float) animation.getAnimatedValue();
                final ViewGroup.LayoutParams params = container.getLayoutParams();
                params.width = (int) animVal;
                container.setLayoutParams(params);
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
                ivMenu.setAlpha(1 - animVal);
            }
        });

        alphaAnim.start();

    }

    private void mostrarMenu() {
        callback.menuAberto();
        atualizarEstadodasViews();
        container.removeAllViews();
        container.addView(menuView);

        final ValueAnimator hAnim = ValueAnimator.ofFloat(containerHeight, menuViewHeight);
        hAnim.setDuration(animDur2);
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
                ivMenu.setAlpha(animVal);
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
