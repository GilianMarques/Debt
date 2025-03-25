package gmarques.debtv3.activities.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.FragmentContasBinding;
import gmarques.debtv3.especificos.CalculadorMonetario;
import gmarques.debtv3.gestores.ContaBancos;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyFragment;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.outros.UIRunnable;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContasFrag extends MyFragment {
    FragmentContasBinding ui;


    public ContasFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_contas, container, false);
        return ui.getRoot();
    }

    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return Broadcaster.atualizarFragContas;
    }

    @Override
    protected void inicializar() {
        carregarContaBancos();
    }

    private void carregarContaBancos() {

        final ArrayList<ContaBanco> contas = ContaBancos.getContas();

        if (contas.size() == 0) ui.getRoot().setVisibility(View.GONE);
        else ui.getRoot().setVisibility(View.VISIBLE);

        new Thread(new UIRunnable() {

            private final ArrayList<View> views = new ArrayList<>();

            @Override
            public void workerThread() {
                for (ContaBanco conta : contas) {
                    View vContaBanco = carregarViewDaConta(conta);
                    views.add(vContaBanco);
                }

            }

            @Override
            public void uiThread() {
                ui.container.removeAllViews();
                for (View view : views) {
                    ui.container.addView(view);
                }
            }
        }).start();


    }

    private View carregarViewDaConta(final ContaBanco conta) {
        View contaView = getLayoutInflater().inflate(R.layout.layout_contas, null);


        ImageView ivMenu = contaView.findViewById(R.id.ivMenu);

        TextView tvNomeConta = contaView.findViewById(R.id.tvNomeConta);
        TextView tvValorContaLivre = contaView.findViewById(R.id.tvValorContaLivre);
        TextView tvValorContaTotal = contaView.findViewById(R.id.tvValorContaTotal);

        tvValorContaTotal.setOnClickListener(v -> atualizarValorConta(conta));

        ivMenu.setVisibility(View.GONE);
        // oculto o container de objetivos, eles nao seram eibidos aqui
        contaView.findViewById(R.id.container).setVisibility(View.GONE);


        BigDecimal valorObjetivos = new BigDecimal("0");

        for (Objetivo objetivo : ContaBancos.getObjetivos(conta))
            if (!objetivo.estaConcluido())
                valorObjetivos = valorObjetivos.add(BigDecimal.valueOf(objetivo.getValor()));

        float valorContaLivre = BigDecimal.valueOf(conta.getValor()).subtract(valorObjetivos).floatValue();

        tvNomeConta.setText(conta.getNome());
        tvValorContaTotal.setText(FormatUtils.emReal(conta.getValor()));
        tvValorContaLivre.setText(FormatUtils.emReal(valorContaLivre));

        if (valorContaLivre > 0) tvValorContaLivre.setTextColor(UIUtils.cor(R.color.flat_color_11));
        else tvValorContaLivre.setTextColor(UIUtils.cor(R.color.flat_color_12));

        return contaView;
    }


    private void atualizarValorConta(ContaBanco conta) {


        View vAttObjetivo = getLayoutInflater().inflate(R.layout.layout_add_edit_conta, null);

        final EditText edtTitulo = vAttObjetivo.findViewById(R.id.edtNome);
        final EditText edtValor = vAttObjetivo.findViewById(R.id.edtValor);

        edtTitulo.setVisibility(View.GONE);
        edtValor.setHint(FormatUtils.emReal(conta.getValor()));
        UIUtils.mostrarTeclado(edtValor);

        new CalculadorMonetario(edtValor, (valor, valorFormatado, editText) -> {
            editText.setText(valorFormatado);
            conta.setValorConta(valor);
        });
        Sheettalogo dialogo = new Sheettalogo(getActivity());
        dialogo.contentView(vAttObjetivo)
                .titulo(getString(R.string.Digiteonovovalordaconta))
                .icone(R.drawable.vec_conta_banco)
                .botaoPositivo(getString(R.string.Concluir), v -> {
                    if (conta.getValor() <= 0) conta.setValorConta(1);
                    MyRealm.insertOrUpdate(conta);
                    Broadcaster.enviar(Broadcaster.atualizarFragContas);
                })
                .botaoNegativo(getString(R.string.Cancelar), v -> {

                })
                .show();


    }


}
