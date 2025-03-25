package gmarques.debtv3.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.pixplicity.easyprefs.library.Prefs;

import java.text.MessageFormat;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.dashboard.MainActivity;
import gmarques.debtv3.databinding.ActivityFalhaBinding;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.FirebaseImpl;

public class Falha extends MyActivity {
    private ActivityFalhaBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ui = DataBindingUtil.setContentView(this, R.layout.activity_falha);


        inicializarObjetosEUI();
    }

    private void inicializarObjetosEUI() {
        String falha = Prefs.getString("trace", null);
        Prefs.putString("trace", null);

        falha = "Conte-nos qualquer detalhe que possa ajudar na resolução do problema\n\n\n" + falha + "\n\n\n\n\n\n\n\n\n\n";


        final String finalFalha = falha;
        new Handler().postDelayed(() -> {
            ui.edtFalha.requestFocus();
            UIUtils.mostrarTeclado(getCurrentFocus());
            ui.edtFalha.setText(finalFalha);
            ui.edtFalha.setSelection(0, 68);
        }, 1000);

        ui.tvEnviar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                enviarRelatorio(ui.edtFalha.getText().toString());
            }
        });
    }

    private void enviarRelatorio(String relatorio) {
        UIUtils.infoToasty(getString(R.string.Enviandorelatorio));
        new FirebaseImpl().enviarRelatorioDeErro(relatorio, (sucesso, msg) -> {
            if (sucesso) {
                UIUtils.sucessoToasty(getString(R.string.Obrigado));
                finalizar();

            } else UIUtils.erroToasty(MessageFormat.format("Envio falhou... {0}", msg));
        });

    }

    @Override
    public void onBackPressed() {
        finalizar();
    }

    /**
     * Deixa a mainActivity aparecer primeir depois se finaliza, a mainActivity nao aparece por baixo
     * pq ele é inicializad em uma nova tarefa
     *
     * */
    private void finalizar() {
        UIUtils.esconderTeclado();
        startActivity(new Intent(getApplicationContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        new Handler().postDelayed(this::finishAffinity, 3000);
    }
}