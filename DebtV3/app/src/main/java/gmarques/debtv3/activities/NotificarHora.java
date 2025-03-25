package gmarques.debtv3.activities;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.LocalDateTime;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import gmarques.debtv3.BuildConfig;
import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityNotificarHoraBinding;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.FBaseNomes;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.outros.Tag;

public class NotificarHora extends MyActivity {
    private ActivityNotificarHoraBinding ui;
    private LocalDateTime dataServidor;
    private Timer timer;
    private boolean enviouRelatiorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*No primeiro boot pode ser que o remote config nao tenha carregado os dados ainda
         * entao nao posso mostrar essa tela atoa pro usuario, meio que caga com a credibilidade*/
        if (Debt.DIFERENÇA_SEG == 0) finish();
        else {

            ui = DataBindingUtil.setContentView(this, R.layout.activity_notificar_hora);
            ui.tvInfo.setText(Html.fromHtml(getString(R.string.a_data_do_seu_dispositivo_pode_estar_errada), Html.FROM_HTML_MODE_COMPACT));
            ui.tvDataDispositivo.setText("");
            ui.tvDataServidor.setText("");
        }
    }

    @Override
    protected void onResume() {
        verificarDataNoServidor();
        super.onResume();
    }

    private void verificarDataNoServidor() {
        new FirebaseImpl().getDataConfiavel(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long stamp = (Long) dataSnapshot.getValue();

                /*se por algum motivo a data do servidor for nula, devo mostrar uma data errada mesmo
                   que de mentira só pra notificar o usuario dos riscos de ficar com a data errada*/

                if (stamp == null) dataServidor = new LocalDateTime().plusSeconds(40);
                else {
                    dataServidor = new LocalDateTime(stamp);
                    if (!enviouRelatiorio) eviarRelatorio();
                }
                atualizarUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                UIUtils.dialogo(NotificarHora.this, "", getString(R.string.Naofoipossivelrecuperaradatadoservidor));
            }
        });

    }

    private void eviarRelatorio() {
        enviouRelatiorio = true;

        LocalDateTime dataDispositivo = new LocalDateTime();

        HashMap<String, Object> dados = new HashMap<>();

        String sDataServidor = dataServidor.toString("dd/MM/YYYY HH:mm:ss:SSS");
        String sDataDispositivo = dataDispositivo.toString("dd/MM/YYYY HH:mm:ss:SSS");

        dados.put("versaoDoApp", BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")");
        dados.put("   dataServidor", sDataServidor);
        dados.put("dataDispositivo", sDataDispositivo);
        dados.put("diferençaEmSegundos", ((dataServidor.toDate().getTime() - dataDispositivo.toDate().getTime())) / 1000);
        /*Usar "/" nos caminhos do firebase cria sub diretorios do mesmo jeito que o explorer do windows enta troco as "/" por "-"*/
        FirebaseDatabase.getInstance().getReference(FBaseNomes.verificaçaoDeData).child(sDataServidor.replace("/", "-")).setValue(dados);
    }

    private void atualizarUI() {
        if (timer != null) timer.cancel();

        if (dataServidor.toDate().getTime() - new LocalDateTime().toDate().getTime() < (Debt.DIFERENÇA_SEG * 1000)
                && new LocalDateTime().toDate().getTime() - dataServidor.toDate().getTime() < (Debt.DIFERENÇA_SEG * 1000)) {
            /*relogio do usuario ta X minutos atrasado*/
            /*relogio do usuario ta X minutos adiantado*/
            finish();
            return;
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    dataServidor = dataServidor.plusSeconds(1);
                    ui.tvDataServidor.setText(dataServidor.plusSeconds(1).toString("dd/MM/YYYY, HH:mm:ss"));
                    ui.tvDataDispositivo.setText(new LocalDateTime().toString("dd/MM/YYYY, HH:mm:ss"));
                    Log.d(Tag.AppTag, "NotificarHora.atualizarUI: " + (dataServidor.toDate().getTime() - new LocalDateTime().toDate().getTime()) + "  " + Debt.DIFERENÇA_SEG);
                });
            }
        }, 0, 1000);


    }
}