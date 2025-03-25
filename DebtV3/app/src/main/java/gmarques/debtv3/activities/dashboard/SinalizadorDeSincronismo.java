 package gmarques.debtv3.activities.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;

import com.pixplicity.easyprefs.library.Prefs;

import org.jetbrains.annotations.NotNull;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.databinding.FragmentSinSincronismoBinding;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyFragment;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.ServicoDeSincronismo;
import gmarques.debtv3.utilitarios.C;

import static gmarques.debtv3.gestores.Meses.mesAtual;


public class SinalizadorDeSincronismo extends MyFragment {
    private FragmentSinSincronismoBinding ui;
    private MyActivity activity;

    public SinalizadorDeSincronismo() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_sin_sincronismo, container, false);
        activity = (MyActivity) getActivity();
        return ui.getRoot();
    }


    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return "null";
    }

    @Override
    protected void inicializar() {

        ui.parent.setVisibility(View.GONE);
        new FirebaseImpl().getDataUltimaAtt(data -> {

            ui.parent.setVisibility(View.VISIBLE);

            long dataLocal = Prefs.getLong(C.ultimaAtualizacaoDBLocal, 0);

            if (data == -9876) notificarErroRecebendoDadosDaNuvem();
            else if (dataLocal < data) notificarSobreNovosDados();
            else ui.parent.setVisibility(View.GONE);

            Log.d(Tag.AppTag, "SinalizadorDeSincronismo.inicializar: " + dataLocal + " : " + data + "  " + (dataLocal < data));
        });
    }

    private void notificarSobreNovosDados() {
        Log.d(Tag.AppTag, "SinalizadorDeSincronismo.notificarSobreNovosDados: ");
        ui.tvInfo.setText("Há novos dados na nuvem");
        ui.ivIcone.setImageResource(R.drawable.vec_notificacao_sinc);
        ui.parent.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                ui.parent.setVisibility(View.GONE);
                sincronizar();
            }
        });
    }


    private void notificarErroRecebendoDadosDaNuvem() {
        ui.tvInfo.setText("Houve um erro ao checar os dados em nuvem.");
        ui.tvInfo.setTextColor(UIUtils.cor(R.color.colorAccent));
        ui.ivIcone.setImageResource(R.drawable.vec_notificacao_sinc_erro);

        new Handler().postDelayed(() -> ui.parent.setVisibility(View.GONE), 5000);

    }

    private void sincronizar() {

        activity.startForegroundService(new Intent(activity, ServicoDeSincronismo.class));
        new ServicoDeSincronismo.Callback() {
            @Override
            public void feito(boolean sucesso, String mensagem) {
                /*recarrego o objeto mesAtual do DB assim recarrego os array de receitas e  despesas do mes */
                mesAtual = Meses.getMes(mesAtual.getMes(), mesAtual.getAno());
                Broadcaster.atualizarMainActivity();

                if (sucesso)
                    UIUtils.sucessoToasty(activity.getString(R.string.Sincronismoconcluidopodesernecessarioreiniciar));
                else
                    UIUtils.erroToasty(activity.getString(R.string.Sincronismofalhou) + "\n" + mensagem);


            }
        };


    }


}
