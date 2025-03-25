package gmarques.debtv3.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityNubankBinding;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.notificacoes_nubank.ReceberNotificacoesNubank;
import gmarques.debtv3.utilitarios.C;

public class NubankActivity extends MyActivity {
    private ActivityNubankBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_nubank);
        setSupportActionBar(ui.toolbar);
        verificarAcessoAsNotificacoesEencaminharUsuarioSeNecessario();

        new Handler().postDelayed(() -> {
            inicializarCampoNomeDespesa();
            inicializarCampoData();
            inicializarBotaoRevogar();
        }, tempoDeEspera);
    }

    private void inicializarBotaoRevogar() {
        ui.btnRevogar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 1);
            }
        });
    }

    private void verificarAcessoAsNotificacoesEencaminharUsuarioSeNecessario() {
        if (!appTemAcessoAsNotificacoes()) {
            UIUtils.infoToasty(getString(R.string.Permitaqueoappacesseasnotificacoes));
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 1);
        }
    }

    private void inicializarCampoNomeDespesa() {
        ui.edtNomeDespesa.setText(Prefs.getString(C.nomeDespesaNubank, ""));
    }
    private void inicializarCampoData() {
        ui.edtData.setText(Prefs.getInt(C.dataDespesaNubank, 10)+"");
        UIUtils.mostrarTeclado(ui.edtData);
    }

    @Override
    protected void onStop() {
        //salva as alteraçoes automaticamente
        Prefs.putString(C.nomeDespesaNubank, ui.edtNomeDespesa.getText().toString());
        Prefs.putInt(C.dataDespesaNubank, Integer.parseInt(ui.edtData.getText().toString()));
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (appTemAcessoAsNotificacoes()) {
                UIUtils.sucessoToasty(getString(R.string.Vamosteajudaramanterafatura));
            } else UIUtils.infoToasty(getString(R.string.Naopodemosmaisajudaramanter));

        }
    }

    private boolean appTemAcessoAsNotificacoes() {
        String enabledNotificationListeners = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabledNotificationListeners.contains(ReceberNotificacoesNubank.class.getName());
    }
}