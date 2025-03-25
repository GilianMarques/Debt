package gmarques.debtv3.activities.ver_receitas;

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
import gmarques.debtv3.activities.add_edit_receitas.AddEditReceitas;
import gmarques.debtv3.databinding.ActivityVerReceitasBinding;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.gestores.Receitas;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;


public class VerReceitas extends MyActivity {
    private ActivityVerReceitasBinding ui;
    private Mes mesAtual = Meses.mesAtual;
    private ReceitasAdapter adapter;
    private boolean atualizarAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_ver_receitas);

        setSupportActionBar(ui.toolbar);
        ActionBar bar = getSupportActionBar();

        bar.setTitle(MessageFormat.format(getString(R.string.ReceitasdeX), mesAtual.getNome()));


        new Handler().postDelayed(this::inicializarRecyclerView, tempoDeEspera);
    }

    private void inicializarRecyclerView() {
        adapter = new ReceitasAdapter(mesAtual.getReceitas(), this, new ReceitasAdapter.Callback() {
            @Override
            public void onClick(int adapterPosition, Receita receita, View itemView) {
                exibirDialogoDeDetalhes(adapterPosition, receita);
            }
        });


        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);

        ui.rv.setLayoutManager(layoutManager);
        ui.rv.setAdapter(adapter);

    }


    private void exibirDialogoDeDetalhes(final int adapterPosition, final Receita receita) {

        new DetalhesReceita(receita, this, new DetalhesReceita.Callback() {
            @Override
            public void cliqueEmPagar() {
                receita.setRecebida(!receita.estaRecebido());
                mesAtual.attReceita(receita);
                adapter.atualizarItem(receita, adapterPosition);
                Broadcaster.enviar(Broadcaster.atualizarGraficoDeBarras, Broadcaster.atualizarCardsDeDados);
            }

            @Override
            public void cliqueEmEditar() {
                atualizarAdapter = true;
                startActivity(new Intent(VerReceitas.this, AddEditReceitas.class).putExtra("id", receita.getId()));
            }

            @Override
            public void cliqueEmRemover() {
                List<Receita> copias = Receitas.getTodasAsCopiasDaReceitaIncluindoAsRecorrentesEParceladasDestaDataEmDiante(receita);
                if (copias.size() > 0) confirmarRemocaoDasCopias(receita, copias);
                mesAtual.removerReceita(receita);
                adapter.removerItem(adapterPosition);
                Broadcaster.enviar(Broadcaster.atualizarGraficoDeBarras, Broadcaster.atualizarCardsDeDados);
            }
        });

    }

    private void confirmarRemocaoDasCopias(Receita receita, final List<Receita> copias) {
        new Sheettalogo(this)
                .titulo(getString(R.string.Porfavorconfirme))
                .mensagem(getString(R.string.Deondedesejaremoverareceita))
                .botaoPositivo(mesAtual.getNome(), v -> {

                })
                .botaoNegativo(MessageFormat.format(getString(R.string.Xemdiante), mesAtual.getNome()), v -> {
                    for (Receita receita1 : copias) Receitas.removerCopias(receita1);

                }).naoCancelavel()
                .show();
        ;
    }

    @Override
    protected void onResume() {
        if (atualizarAdapter) {
            adapter.atualizar(mesAtual.getReceitas());
            Broadcaster.enviar(Broadcaster.atualizarGraficoDeBarras,Broadcaster.atualizarCardsDeDados);
        }
        atualizarAdapter = false;
        super.onResume();
    }
}
