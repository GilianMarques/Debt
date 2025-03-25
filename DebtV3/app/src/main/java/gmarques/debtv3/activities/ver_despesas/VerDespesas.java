package gmarques.debtv3.activities.ver_despesas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.text.MessageFormat;
import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.activities.add_edit_despesas.AddEditDespesas;
import gmarques.debtv3.databinding.ActivityVerDespesasBinding;
import gmarques.debtv3.gestores.Despesas;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;


public class VerDespesas extends MyActivity {
    private ActivityVerDespesasBinding ui;
    private Mes mesAtual = Meses.mesAtual;
    private DespesasAdapter adapter;
    private boolean atualizarAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_ver_despesas);

        setSupportActionBar(ui.toolbar);
        ActionBar bar = getSupportActionBar();

        bar.setTitle(MessageFormat.format(getString(R.string.DespesasdeX), mesAtual.getNome()));


        new Handler().postDelayed(this::inicializarRecyclerView, tempoDeEspera);
    }

    private void inicializarRecyclerView() {
        adapter = new DespesasAdapter(mesAtual.getDespesas(), this, new DespesasAdapter.Callback() {
            @Override
            public void onClick(int adapterPosition, Despesa despesa, View itemView) {
                exibirDialogoDeDetalhes(adapterPosition, despesa);
            }
        });


        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);

        ui.rv.setLayoutManager(layoutManager);
        ui.rv.setAdapter(adapter);

    }


    private void exibirDialogoDeDetalhes(final int adapterPosition, final Despesa despesa) {

        new DetalhesDespesa(despesa, this, new DetalhesDespesa.Callback() {
            @Override
            public void cliqueEmPagar() {
                despesa.setPaga(!despesa.estaPaga());
                mesAtual.attDespesa(despesa);
                adapter.atualizarItem(despesa, adapterPosition);
                Broadcaster.enviar(Broadcaster.atualizarGraficoDeBarras,Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarFragDespesas);
            }

            @Override
            public void cliqueEmEditar() {
                atualizarAdapter = true;
                startActivity(new Intent(VerDespesas.this, AddEditDespesas.class).putExtra("id", despesa.getId()));
            }

            @Override
            public void cliqueEmRemover() {
                List<Despesa> copias = Despesas.getTodasAsCopiasDaDespesaIncluindoAsRecorrentesEParceladasDestaDataEmDiante(despesa);
                if (copias.size() > 0) confirmarRemocaoDasCopias(despesa, copias);
                mesAtual.removerDespesa(despesa);
                adapter.removerItem(adapterPosition);
                Broadcaster.enviar( Broadcaster.atualizarGraficoDeBarras,Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarFragDespesas);

            }
        });

    }

    private void confirmarRemocaoDasCopias(Despesa despesa, final List<Despesa> copias) {
        new Sheettalogo(this)
                .titulo(getString(R.string.Porfavorconfirme))
                .mensagem(getString(R.string.Deondedesejaremoveradespesa))
                .botaoPositivo(mesAtual.getNome(), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                })
                .botaoNegativo(MessageFormat.format(getString(R.string.Xemdiante), mesAtual.getNome()), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (Despesa despesa1 : copias) Despesas.removerCopias(despesa1);

                    }
                })
              .naoCancelavel()
                .show();
        ;
    }

    @Override
    protected void onResume() {
        if (atualizarAdapter) {
            adapter.atualizar(mesAtual.getDespesas());
            Broadcaster.enviar( Broadcaster.atualizarGraficoDeBarras,Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarFragDespesas);
        }
        atualizarAdapter = false;
        super.onResume();
    }
}
