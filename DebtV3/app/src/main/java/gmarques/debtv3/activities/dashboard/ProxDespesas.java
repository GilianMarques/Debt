package gmarques.debtv3.activities.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import org.joda.time.LocalDate;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.activities.add_edit_despesas.AddEditDespesas;
import gmarques.debtv3.activities.ver_despesas.DetalhesDespesa;
import gmarques.debtv3.databinding.FragmentProxDespesasBinding;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.gestores.Despesas;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyFragment;
import io.realm.RealmList;

import static gmarques.debtv3.gestores.Meses.mesAtual;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProxDespesas extends MyFragment {
    private FragmentProxDespesasBinding ui;

    private RealmList<Despesa> despesasEmAberto = new RealmList<>();
    private MyActivity activity;

    public ProxDespesas() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_prox_despesas, container, false);
        return ui.getRoot();
    }

    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return Broadcaster.atualizarFragDespesas;
    }

    @Override
    protected void inicializar() {
        mesAtual = mesAtual;
        activity = (MyActivity) getActivity();

        carregarDespesas();
        if (despesasEmAberto.size() == 0) {
            ui.getRoot().setVisibility(View.GONE);
        } else ui.getRoot().setVisibility(View.VISIBLE);
        carregarViews();
    }

    private void carregarViews() {

        ui.container.removeAllViews();

        for (final Despesa despesa : despesasEmAberto) {

            Categoria categoria = Categorias.getCategoria(despesa.getCategoriaId());

            View view = getLayoutInflater().inflate(R.layout.layout_chip_despesa, null);

            ImageView iv = view.findViewById(R.id.ivIcone);
            TextView tvNome = view.findViewById(R.id.tvNome);

            iv.setImageDrawable(UIUtils.aplicarTema(Categorias.getIntIcone(categoria.getIcone()), UIUtils.corAttr(android.R.attr.colorPrimary)));
            tvNome.setText(despesa.getNome());

            view.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    exibirDialogoDeDetalhes(despesa);
                }
            });

            ui.container.addView(view);
        }
    }

    private void exibirDialogoDeDetalhes(final Despesa despesa) {

        new DetalhesDespesa(despesa, activity, new DetalhesDespesa.Callback() {
            @Override
            public void cliqueEmPagar() {
                despesa.setPaga(!despesa.estaPaga());
                mesAtual.attDespesa(despesa);
                Broadcaster.enviar(Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarFragDespesas);
            }

            @Override
            public void cliqueEmEditar() {
                startActivity(new Intent(ProxDespesas.this.activity, AddEditDespesas.class).putExtra("id", despesa.getId()));
            }

            @Override
            public void cliqueEmRemover() {
                List<Despesa> copias = Despesas.getTodasAsCopiasDaDespesaIncluindoAsRecorrentesEParceladasDestaDataEmDiante(despesa);
                if (copias.size() > 0) confirmarRemocaoDasCopias(despesa, copias);
                mesAtual.removerDespesa(despesa);
                inicializar();
                Broadcaster.enviar(Broadcaster.atualizarGraficoDeBarras, Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarFragDespesas);
            }
        });

    }


    private void confirmarRemocaoDasCopias(Despesa despesa, final List<Despesa> copias) {
        new Sheettalogo(activity)
                .titulo(getString(R.string.Porfavorconfirme))
                .mensagem(getString(R.string.Deondedesejaremoveradespesa))
                .botaoPositivo(mesAtual.getNome(), v -> {

                })
                .botaoNegativo(MessageFormat.format(getString(R.string.Xemdiante), mesAtual.getNome()), v -> {
                    for (Despesa despesa1 : copias) Despesas.removerCopias(despesa1);
                })
                .naoCancelavel()
                .show();
        ;
    }

    private void carregarDespesas() {

        despesasEmAberto.clear();
        RealmList<Despesa> despesas = mesAtual.getDespesas();
        /*despesas que vencem de dois dias pra cá incluindo em atraso  (se hj é 25, contem despesas dos dias ...23,24, 25,26,27)*/
        LocalDate proxDoisDias = new LocalDate().plusDays(3);

        for (Despesa despesa : despesas) {
            if (!despesa.estaPaga() && despesa.getDataDePagamento() < proxDoisDias.toDate().getTime())
                despesasEmAberto.add(despesa);
        }
        Collections.sort(despesasEmAberto, (o1, o2) -> (int) (o1.getDataDePagamento() - o2.getDataDePagamento()));
    }
}
