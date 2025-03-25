package gmarques.debtv3.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Locale;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.dashboard.MainActivity;
import gmarques.debtv3.databinding.ActivityTermosDeUsoBinding;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.RemoteConfig;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.C;

public class TermosDeUso extends MyActivity {
    private ActivityTermosDeUsoBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_termos_de_uso);

        ui.webView.setVisibility(View.GONE);
        ui.cbAceitar.setVisibility(View.GONE);

        inhicializarWebView();
        inicializarCheckBox();
        salvarDataDaUltimaAtualizaçaoDosTermosDeUso();


    }

    /**
     * Verifica junto ao remote config a data da última atualização dos termos de uso e salva nas preferencias.
     * Uma vez que esta tela é exibida usuário ela exibe o site com a versão mais recente dos termos, logo salvo nas preferências
     * que esta versão dos termos foi exibida para o usuário e que não há necessidade de re-exibila posteriormente ao
     * usuario como se esta fosse uma atualizaçao
     */
    private void salvarDataDaUltimaAtualizaçaoDosTermosDeUso() {
        new RemoteConfig.RConfigListener(this) {
            @Override
            protected void configuraçoesCarregadas(boolean sucesso, FirebaseRemoteConfig remoteConfig) {
                if (sucesso) {
                    long ultimaAttFb = remoteConfig.getLong(RemoteConfig.ultimaAttDosTermosDeUso);
                    Log.d(Tag.AppTag, "TermosDeUso.configuraçoesCarregadas :" + ultimaAttFb);
                    Prefs.putLong(C.ultimaAtualizaçaoDosTermos, ultimaAttFb);
                }
            }
        };
    }

    private void notificarSobreAtualizaçaoDosTermos() {

        String msg = getString(R.string.Ostermosdeusoepoliticadeprivacidadeforamatualizadosemx);
        UIUtils.dialogo(this, "", String.format(Locale.getDefault(), msg, FormatUtils.formatarData(Prefs.getLong(C.ultimaAtualizaçaoDosTermos, 0), true)));
    }

    private void inicializarCheckBox() {
        ui.cbAceitar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) notificaroUsuarioeIniciarApp();
        });
    }

    private void notificaroUsuarioeIniciarApp() {
        AlertDialog d = new AlertDialog.Builder(TermosDeUso.this)
                .setTitle("")
                .setMessage(R.string.OStermosseraoexibidosparavocesemprequeforematualizados)
                .create();

        d.setOnDismissListener(dialog -> {
            Prefs.putBoolean(C.usuarioAceitouOsTermosDeUso, true);
            startActivity(new Intent(getApplicationContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finishAffinity();
        });

        Window window = d.getWindow();
        if (window != null)
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        d.show();
    }

    private void inhicializarWebView() {
        ui.webView.loadUrl("https://brfeedbackus.wixsite.com/termos-de-uso");
        ui.webView.getSettings().setUseWideViewPort(true);
        ui.webView.getSettings().setLoadWithOverviewMode(true);

        ui.webView.getSettings().setSupportZoom(true);
        ui.webView.getSettings().setBuiltInZoomControls(true);
        ui.webView.getSettings().setDisplayZoomControls(false);

        ui.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ui.progressBar.setVisibility(View.GONE);
                ui.splash.setVisibility(View.GONE);
                ui.webView.setVisibility(View.VISIBLE);
                ui.cbAceitar.setVisibility(View.VISIBLE);

                if ("atualizaçao".equals(getIntent().getAction()))
                    notificarSobreAtualizaçaoDosTermos();

            }

        });

    }
}