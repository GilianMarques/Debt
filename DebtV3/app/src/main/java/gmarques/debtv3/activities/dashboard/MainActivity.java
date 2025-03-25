package gmarques.debtv3.activities.dashboard;

import static gmarques.debtv3.gestores.Meses.mesAtual;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.animation.AnticipateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pixplicity.easyprefs.library.Prefs;
import com.squareup.picasso.Picasso;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;

import gmarques.debtv3.BuildConfig;
import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.Falha;
import gmarques.debtv3.activities.Login;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.activities.NotificarHora;
import gmarques.debtv3.activities.TermosDeUso;
import gmarques.debtv3.activities.perfil.Perfil;
import gmarques.debtv3.broadcast_e_servicos.GestorDeAlarmes;
import gmarques.debtv3.databinding.ActivityMainBinding;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.nuvem.RemoteConfig;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.C;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class MainActivity extends MyActivity {
    private ActivityMainBinding ui;

    private ValueAnimator cornerAnimation;
    private ValueAnimator statusBarColorAnimatior;
    private ValueAnimator paddingAnimator;
    private ValueAnimator fotoDePerfilEMenuY;
    private FloatMenu floatMenu;
    private MainActivityMenu mMainActivityMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_main);
        ui.splash.setVisibility(View.VISIBLE);
        final Runnable inicializar = () -> {
            açoesFeitasNaTelaSplash();
            inicializarTrocaDeMes();
            inicializarViewQueFechaOsMenus();
            inicializarFloatMenu();
            inicializarMenu();
            inicializarFotoDePerfil();
            adicionarFragmentos();
            inicializarAnimaçoes();
            inicializarAppBar();
            inicializarRemoteConfig();

            new Handler().postDelayed(() -> ui.splash.setVisibility(View.GONE), 1000);

            //   startActivity(new Intent(MainActivity.this, VerContas.class));

        };

        final Runnable preInicializar = () -> {
            if (verificarSeHouveErroEAbrirTelaDeFalha()) {
                finish();
                return;
            }

            Usuario.estaLogado(logado -> {
                Log.d(Tag.AppTag, "MainActivity.onCreate: Usuario logado? " + logado);
                if (logado) {
                    if (!Prefs.getBoolean(C.usuarioAceitouOsTermosDeUso, false)) {
                        startActivity(new Intent(getApplicationContext(), TermosDeUso.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        finishAffinity();
                    } else inicializar.run();
                } else {
                    startActivity(new Intent(getApplicationContext(), Login.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    finishAffinity();
                }
            });

        };

        new Handler().postDelayed(preInicializar, 1000);

    }

    private void inicializarRemoteConfig() {
        new RemoteConfig.RConfigListener(this) {
            @Override
            protected void configuraçoesCarregadas(boolean sucesso, FirebaseRemoteConfig remoteConfig) {
                verificarBloqueioDoApp(remoteConfig);
                notificarSobreAtualizaçaoDisponivel(remoteConfig);
                verificarAtualizaçaoDosTermosDeUso(remoteConfig);
            }
        };
    }

    private void verificarAtualizaçaoDosTermosDeUso(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        long ultimaAttFb = mFirebaseRemoteConfig.getLong(RemoteConfig.ultimaAttDosTermosDeUso);
        Log.d(Tag.AppTag, "MainActivity.verificarAtualizaçaoDosTermosDeUso: " + ultimaAttFb);
        if (ultimaAttFb > 0) {
            long ultimaAttLocal = Prefs.getLong(C.ultimaAtualizaçaoDosTermos, 0);
            /*uso != ao inves de > pq se eu colocar uma data errada no console do fb e um dispositivo salvar essa data nas preferencias,
            pode nao ser possivel exibir as atualizaçoes dos termos ate que o usuario limpe os dados do app*/
            if (ultimaAttFb != ultimaAttLocal) {
                Prefs.putLong(C.ultimaAtualizaçaoDosTermos, ultimaAttFb);
                Prefs.putBoolean(C.usuarioAceitouOsTermosDeUso, false);
                startActivity(new Intent(Debt.binder.get().getApplicationContext(), TermosDeUso.class).setAction("atualizaçao").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finishAffinity();
            }

        }
    }

    private void verificarBloqueioDoApp(final FirebaseRemoteConfig mFirebaseRemoteConfig) {
        /* se bloquearVersoesAbaixoDe = 3 versoes de 2 para baixo serao bloqueadas obrigando o usuario a atualizar o app*/

        final int bloquearVersoesAbaixoDe = (int) mFirebaseRemoteConfig.getLong(RemoteConfig.bloquearVersoesAbaixoDe);

        if (BuildConfig.VERSION_CODE >= bloquearVersoesAbaixoDe) return;


        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.Atualizeoapp));
        alertDialog.setMessage(getString(R.string.Estaversaodoidesativadapelo));
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Irparaplaystore), (dialog, which) -> {

            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            if (BuildConfig.VERSION_CODE < bloquearVersoesAbaixoDe)
                verificarBloqueioDoApp(mFirebaseRemoteConfig);
        });

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }

        alertDialog.show();

    }

    private void notificarSobreAtualizaçaoDisponivel(final FirebaseRemoteConfig mFirebaseRemoteConfig) {
        /* se versaoAtiva=5, versoes de de 4 para baixo vao notificar sobre atualizaçao*/

        int versaoAtiva = (int) mFirebaseRemoteConfig.getLong(RemoteConfig.versaoAtiva);

        if (BuildConfig.VERSION_CODE == versaoAtiva || BuildConfig.DEBUG) return;

        /*QQer versao que nao seja igual a versao ativa deve exibir dialogo de atualizaçao*/

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.Atualizeoapp));
        alertDialog.setMessage(getString(R.string.Umanovaversaoestadisponivel));
        alertDialog.setCancelable(true);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Irparaplaystore), (dialog, which) -> {

            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }

        });

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }

        alertDialog.show();

    }

    /**
     * Verifica nas preferencias se o app encerrou com um erro na sua ultima execuçao, caso nao retorna false
     * e ao app é inicalizado normalmente. Caso sim, abre a tela de falhas em uma nova tarefa para que o erro nao
     * a impessa de ser aberta, e fecha a mainActivity. Da tela de falhas o usuario pode
     * enviar o relatorio e apos isso o app é aberto por la.
     */
    private boolean verificarSeHouveErroEAbrirTelaDeFalha() {
        if (Prefs.getString("trace", null) != null) {
            startActivity(new Intent(getApplicationContext(), Falha.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        return false;
    }

    private void inicializarAnimaçoes() {
        cornerAnimation = ValueAnimator.ofFloat((int) UIUtils.dp(90), (int) UIUtils.dp(0));
        cornerAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float cornerRadius = (float) animation.getAnimatedValue();
                GradientDrawable drawable = (GradientDrawable) ui.nested.getBackground();
                GradientDrawable drawable2 = (GradientDrawable) ui.cltb.getBackground();
                drawable.setCornerRadii(new float[]{cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, 0f, 0f});
                drawable2.setCornerRadii(new float[]{0f, 0f, 0f, 0f, cornerRadius, cornerRadius, 0f, 0f});
            }
        });


        fotoDePerfilEMenuY = ValueAnimator.ofFloat(ui.ivFotoDePerfil.getY(), -ui.ivFotoDePerfil.getMeasuredHeight());
        fotoDePerfilEMenuY.setInterpolator(PathInterpolatorCompat.create(1.000f, 0.000f, 1.000f, 1.030f));
        fotoDePerfilEMenuY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float y = (float) animation.getAnimatedValue();
                ui.ivFotoDePerfil.setY(y);
                ui.menuContainer.setY(y);

            }
        });

        paddingAnimator = ValueAnimator.ofInt((int) UIUtils.dp(24), (int) UIUtils.dp(8));
        paddingAnimator.setInterpolator(new AnticipateInterpolator());
        paddingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ui.nested.setPadding(ui.nested.getPaddingLeft(), (int) animation.getAnimatedValue(), ui.nested.getPaddingRight(), ui.nested.getPaddingBottom());
            }
        });


        int targetColor = UIUtils.corAttr(R.attr.appWindowBackground);
        statusBarColorAnimatior = ValueAnimator.ofArgb(getWindow().getStatusBarColor(), targetColor);
        statusBarColorAnimatior.setInterpolator(PathInterpolatorCompat.create(1.000f, 0.000f, 1.000f, 1.030f));
        statusBarColorAnimatior.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                getWindow().setStatusBarColor((Integer) animation.getAnimatedValue());
            }
        });


    }

    private void inicializarAppBar() {
        final View decorView = getWindow().getDecorView();
        final int decorViewFlags = decorView.getSystemUiVisibility();

        ui.appbar.post(new Runnable() {
            @Override
            public void run() {

                final int maxScroll = ui.appbar.getMeasuredHeight();

                ui.appbar.addOnOffsetChangedListener(new AppBarLayout.BaseOnOffsetChangedListener() {
                    @Override
                    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                        verticalOffset = -1 * verticalOffset;

                        float fracao = new BigDecimal(verticalOffset).divide(new BigDecimal(maxScroll), 6, BigDecimal.ROUND_UP).multiply(new BigDecimal(100)).floatValue();

                        paddingAnimator.setCurrentFraction(fracao / 100);
                        cornerAnimation.setCurrentFraction(fracao / 100);
                        fotoDePerfilEMenuY.setCurrentFraction(fracao / 100);
                        statusBarColorAnimatior.setCurrentFraction(fracao / 100);

                        if (fracao < 60) {
                            decorView.setSystemUiVisibility(decorViewFlags);
                        } else {
                            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        }

                    }
                });

            }
        });
    }

    public void atualizarBalançoDoDia(float val) {


        String texto = getString(R.string.Balancododia);
        String valor = FormatUtils.emReal(val);

        ui.tvBalancoDia.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
        Spannable span = new SpannableString(valor + "\n" + texto);
        span.setSpan(new RelativeSizeSpan(0.35f), valor.length(), valor.length() + texto.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        ui.tvBalancoDia.setText(span);

        if (val == -999) ui.tvBalancoDia.setText("");
    }

    private void açoesFeitasNaTelaSplash() {

        /*primiro boot*/
        if (Prefs.getBoolean(C.primeiroBoot, true)) {
            Categorias.addPrimeirasCategorias();

            GestorDeAlarmes alarmes = new GestorDeAlarmes(this);
            if (!alarmes.estaoAsNotificaçoesLigadas()) {
                alarmes.ligarNotificaçoes();
                alarmes.alternarAutoSinc(true);
            }

         /*    dou 7 dias sem anuncios para novos usuarios (7 é o numero padrao no momento em que escrevo)
            simulando que ele tenha assistido o anuncio premiado para suspender os anuncios*/
            if (!BuildConfig.DEBUG)
                Prefs.putLong(C.dataDoAnuncioPremiado, new LocalDate().toDate().getTime());

            Prefs.edit().putBoolean(C.primeiroBoot, false).commit();
        }


        /*a cada 7 dias ou enquanto o usuário não enviar seus dados públicos o código abaixo será executado*/
        if (Days.daysBetween(new LocalDate(Prefs.getLong(C.dadosPublicosEnviados, 0)), new LocalDate()).getDays() > 7) {
            Usuario.enviarDadosPublicos();
        }
        /* Verifico se a data esta certa */
        new FirebaseImpl().getDataConfiavel(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long stamp = (Long) dataSnapshot.getValue();
                if (stamp == null) {
                    Log.d(Tag.AppTag, "MainActivity.onDataChange: Nao foi possivel verificar a data");
                } else {

                    if (new LocalDateTime(stamp).toDate().getTime() - new LocalDateTime().toDate().getTime() > (Debt.DIFERENÇA_SEG * 1000)
                            || new LocalDateTime().toDate().getTime() - new LocalDateTime(stamp).toDate().getTime() > (Debt.DIFERENÇA_SEG * 1000)) {

                        /*relogio do usuario ta X minutos atrasado*/
                        /*relogio do usuario ta X minutos adiantado*/

                        startActivity(new Intent(MainActivity.this, NotificarHora.class));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void inicializarViewQueFechaOsMenus() {
        ui.fecharMenusView.setOnClickListener(v -> {
            if (floatMenu.estaAberto()) floatMenu.exibirDispensarMenu();
            if (mMainActivityMenu.estaAberto()) mMainActivityMenu.exibirDispensarMenu();
        });
    }

    private void inicializarFotoDePerfil() {
        Picasso.get().load(Usuario.getFotoDePerfil()).transform(new CropCircleTransformation()).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ui.ivFotoDePerfil);
        ui.ivFotoDePerfil.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                startActivity(new Intent(MainActivity.this, Perfil.class));
            }
        });

    }

    private void inicializarMenu() {
        mMainActivityMenu = new MainActivityMenu(new MainActivityMenu.Callback() {
            @Override
            public void menuAberto() {
                if (floatMenu.estaAberto()) floatMenu.exibirDispensarMenu();
                ui.fecharMenusView.setElevation(4);
            }

            @Override
            public void menuFechado() {
                ui.fecharMenusView.setElevation(0);

            }
        }, this);
        mMainActivityMenu.inicializar(ui.menuContainer);

    }

    private void inicializarFloatMenu() {
        floatMenu = new FloatMenu(new FloatMenu.Callback() {
            @Override
            public void menuAberto() {
                if (mMainActivityMenu.estaAberto()) mMainActivityMenu.exibirDispensarMenu();
                ui.fecharMenusView.setElevation(4);
            }

            @Override
            public void menuFechado() {
                ui.fecharMenusView.setElevation(0);
            }
        }, this);
        floatMenu.inicializar(ui.addMenuContainer);
    }

    private void adicionarFragmentos() {
        FragmentManager fManager = getSupportFragmentManager();



        /*---------------------------------------------------*/
        SinalizadorDeSincronismo sSinc = (SinalizadorDeSincronismo) fManager.findFragmentByTag(SinalizadorDeSincronismo.class.getSimpleName());

        if (sSinc == null) {
            sSinc = new SinalizadorDeSincronismo();
            fManager.beginTransaction().add(R.id.frag0, sSinc, SinalizadorDeSincronismo.class.getSimpleName()).commitAllowingStateLoss();
        }

        /*---------------------------------------------------*/
        Atividade atividade = (Atividade) fManager.findFragmentByTag(Atividade.class.getSimpleName());

        if (atividade == null) {
            atividade = new Atividade();
            fManager.beginTransaction().add(R.id.frag1, atividade, Atividade.class.getSimpleName()).commitAllowingStateLoss();
        }

        /*---------------------------------------------------*/
        VariacaoDaReceita variacaoDaReceita = (VariacaoDaReceita) fManager.findFragmentByTag(VariacaoDaReceita.class.getSimpleName());

        if (variacaoDaReceita == null) {
            variacaoDaReceita = new VariacaoDaReceita();
            fManager.beginTransaction().add(R.id.frag2, variacaoDaReceita, VariacaoDaReceita.class.getSimpleName()).commitAllowingStateLoss();
        }


        /*---------------------------------------------------*/
        DadosSobreOMes dadosSobreOMes = (DadosSobreOMes) fManager.findFragmentByTag(DadosSobreOMes.class.getSimpleName());

        if (dadosSobreOMes == null) {
            dadosSobreOMes = new DadosSobreOMes();
            fManager.beginTransaction().add(R.id.frag3, dadosSobreOMes, DadosSobreOMes.class.getSimpleName()).commitAllowingStateLoss();
        }

        /*---------------------------------------------------*/
        ProxDespesas mpDespesas = (ProxDespesas) fManager.findFragmentByTag(ProxDespesas.class.getSimpleName());

        if (mpDespesas == null) {
            mpDespesas = new ProxDespesas();
            fManager.beginTransaction().add(R.id.frag4, mpDespesas, ProxDespesas.class.getSimpleName()).commitAllowingStateLoss();
        }

        /*---------------------------------------------------*/
        ContasFrag contasFrag = (ContasFrag) fManager.findFragmentByTag(ContasFrag.class.getSimpleName());

        if (contasFrag == null) {
            contasFrag = new ContasFrag();
            fManager.beginTransaction().add(R.id.frag5, contasFrag, ContasFrag.class.getSimpleName()).commitAllowingStateLoss();
        }
        /*---------------------------------------------------*/
        NotasFrag notasFrag = (NotasFrag) fManager.findFragmentByTag(NotasFrag.class.getSimpleName());

        if (notasFrag == null) {
            notasFrag = new NotasFrag();
            fManager.beginTransaction().add(R.id.frag6, notasFrag, NotasFrag.class.getSimpleName()).commitAllowingStateLoss();
        }


    }

    private void inicializarTrocaDeMes() {
        /*trocar de mes mto rapido causa nullpointerex*/
        final LocalDate dataAtual = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);

        Mes mesAnterior = Meses.getMes(dataAtual.minusMonths(1).withDayOfMonth(1));
        Mes mesSeguinte = Meses.getMes(dataAtual.plusMonths(1).withDayOfMonth(1));

        ui.tvMesAtual.setText(mesAtual.getNome());

        if (mesSeguinte != null) ui.tvMesSeguinte.setText(mesSeguinte.getNome());
        else ui.tvMesSeguinte.setText("");

        if (mesAnterior != null) ui.tvMesAnterior.setText(mesAnterior.getNome());
        else ui.tvMesAnterior.setText("");

        ui.tvMesSeguinte.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                trocarMesEAtualizarUI(Meses.getMes(dataAtual.plusMonths(1).withDayOfMonth(1)));
            }
        });


        ui.tvMesAnterior.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                trocarMesEAtualizarUI(Meses.getMes(dataAtual.minusMonths(1).withDayOfMonth(1)));
            }
        });
    }

    void trocarMesEAtualizarUI(Mes novoMesAtual) {
        Meses.setMesAtual(novoMesAtual);
        inicializarTrocaDeMes();
        Broadcaster.atualizarMainActivity();
    }

    @Override
    public void onBackPressed() {
        if (floatMenu != null && floatMenu.estaAberto()) floatMenu.exibirDispensarMenu();
        else if (mMainActivityMenu != null && mMainActivityMenu.estaAberto())
            mMainActivityMenu.exibirDispensarMenu();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Debt.binder.get().liveSinc.pararDeOuvir();
        super.onDestroy();
    }

    private void addDespesasEreceitasParaTestes() {
        ArrayList<Categoria> categorias = Categorias.getCategorias();
        LocalDate dataDespesa = new LocalDate(2020, 6, 1);
        LocalDate dataReceita = new LocalDate(2020, 6, 1);
        for (int i = 1; i < 20; i++) {
            Despesa despesa = new Despesa();
            despesa.setNome("despesa #" + i);
            despesa.setValor(getRandom(45, 150));
            despesa.setPaga(new Random().nextBoolean());
            despesa.setDataDePagamento(dataDespesa);
            despesa.setCategoriaId(categorias.get(new Random().nextInt(categorias.size())).getId());

            mesAtual.addDespesa(despesa);

            dataDespesa = dataDespesa.plusDays(2);

            if (i > 17) {
                Receita receita = new Receita();
                receita.setNome("Receita #" + i);
                receita.setValor(getRandom(400, 1200));
                receita.setDataDeRecebimento(dataReceita);
                receita.setRecebida(new Random().nextBoolean());
                mesAtual.addReceita(receita);
                dataReceita = dataReceita.plusDays(2);
            }
        }


    }

    private float getRandom(int piso, int teto) {
        float valor = new Random().nextInt((teto + 1));
        if (valor < piso) return getRandom(piso, teto);
        else return valor;
    }

}
