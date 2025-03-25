package gmarques.debtv3.activities.relatorio_do_dia;


import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.broadcast_e_servicos.RelatorioDoDia;
import gmarques.debtv3.databinding.ActivityRelatorioDoDiaBinding;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;

import static gmarques.debtv3.gestores.Meses.mesAtual;


public class RelatorioDoDiaAct extends MyActivity {
    private ActivityRelatorioDoDiaBinding ui;
    private ObjetosAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_relatorio_do_dia);
        setSupportActionBar(ui.toolbar);
        ActionBar actBar = getSupportActionBar();
        assert actBar != null;
        actBar.setTitle(getString(R.string.Relatoriododia));
        inicializarRecyclerView();

    }

    private void inicializarRecyclerView() {
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);

        ui.rv.setLayoutManager(layoutManager);

      adapter = new ObjetosAdapter(new RelatorioDoDia().getPendencias(), new ObjetosAdapter.Callback() {
            @Override
            public void onClick(Sincronizavel sObj, int adapterPosition) {
                if (sObj instanceof Despesa) {
                    ((Despesa) sObj).setPaga(!((Despesa) sObj).estaPaga());
                    mesAtual.attDespesa(((Despesa) sObj));
                } else if (sObj instanceof Receita) {
                    ((Receita) sObj).setRecebida(!((Receita) sObj).estaRecebido());
                    mesAtual.attReceita(((Receita) sObj));
                }
                adapter.notifyDataSetChanged();
            }
        });
        ui.rv.setAdapter(adapter);
    }


}
