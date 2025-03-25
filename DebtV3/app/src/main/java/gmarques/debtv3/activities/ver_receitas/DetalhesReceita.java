package gmarques.debtv3.activities.ver_receitas;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.charts.LineChart;

import org.joda.time.LocalDate;

import gmarques.debtv3.R;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.novo_bsheet.Sheettalogo;


/**
 * Criado por Gilian Marques
 * Sábado, 05 de Outubro de 2019  as 14:42:37.
 */
public class DetalhesReceita {

    private final View view;
    private final Sheettalogo dialogo;
    Receita receita;
    private Activity mActivity;
    private Callback callback;

    public DetalhesReceita(Receita receita, Activity mActivity, Callback callback) {
        this.receita = receita;
        this.mActivity = mActivity;
        this.callback = callback;

        view = mActivity.getLayoutInflater().inflate(R.layout.layout_detalhes_despesa, null);
        dialogo = new Sheettalogo((FragmentActivity) mActivity).contentView(view);

        inicializarUI();
        inicializarBotoes();
        finalizarCargaEExibir();
    }

    private void finalizarCargaEExibir() {

        if (callback == null) {
            //if there's no callback, no action buttons will be shown
            view.findViewById(R.id.actionsContainer).setVisibility(View.GONE);
        }

        new GraficoDeLinhaComInfo((LineChart) view.findViewById(R.id.lineChart), receita);

        dialogo.show();

    }

    private void inicializarBotoes() {


        view.findViewById(R.id.ivPagar).setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                dialogo.dismiss();
                callback.cliqueEmPagar();
            }
        });
        view.findViewById(R.id.ivEditar).setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                dialogo.dismiss();
                callback.cliqueEmEditar();
            }
        });
        view.findViewById(R.id.ivRemover).setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                dialogo.dismiss();

                final Sheettalogo dialogo2 = new Sheettalogo((FragmentActivity) mActivity)
                        .titulo(mActivity.getString(R.string.Porfavorconfirme))
                        .mensagem(mActivity.getString(R.string.Desejamesmoremoverestareceita))
                        .icone(R.drawable.vec_info)
                        .botaoPositivo(mActivity.getString(R.string.Cancelar), null)
                        .botaoNegativo(mActivity.getString(R.string.Remover), v -> callback.cliqueEmRemover());

                Runnable mRunnable = dialogo2::show;
                new Handler().postDelayed(mRunnable, 300);
            }
        });
    }

    private void inicializarUI() {
        TextView tvName, tvValue, tvDate, tvComments, tvPaid, tvInstallments;
        ImageView ivCat;
        tvName = view.findViewById(R.id.tvName);
        tvValue = view.findViewById(R.id.tvValor);
        tvDate = view.findViewById(R.id.tvDataPgto);
        tvComments = view.findViewById(R.id.tvObservacoes);
        tvPaid = view.findViewById(R.id.tvPago);
        tvInstallments = view.findViewById(R.id.tvParcelas);
        ivCat = view.findViewById(R.id.ivCategoria);

        //  ivCat.setBackground(UIUtils.aplicarTema(ivCat.getBackground(), UIUtils.corComTransaparencia(categoria.getCor(), 0.85f)));
        ivCat.setImageResource(R.drawable.vec_receita);

        tvName.setText(receita.getNome());
        tvValue.setText(FormatUtils.emReal(receita.getValor()));
        tvDate.setText(FormatUtils.formatarData(receita.getDataDeRecebimento(), true));

        if (receita.getObservacoes() == null || receita.getObservacoes().isEmpty())
            tvComments.setVisibility(View.GONE);
        else tvComments.setText(receita.getObservacoes());

        tvPaid.setText(receita.estaRecebido() ? mActivity.getString(R.string.Recebida) : mActivity.getString(R.string.Pendente));


        if (receita.getAutoImportarPrimeira() > 0) {
            tvInstallments.setText(FormatUtils.formatarDataEmPeriodo(new LocalDate(receita.getAutoImportarPrimeira()), new LocalDate(receita.getAutoImportarUltima())));
        } else tvInstallments.setVisibility(View.GONE);


    }


    public interface Callback {

        void cliqueEmPagar();

        void cliqueEmEditar();

        void cliqueEmRemover();
    }
}
