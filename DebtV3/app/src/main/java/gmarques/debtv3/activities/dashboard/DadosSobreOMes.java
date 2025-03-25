package gmarques.debtv3.activities.dashboard;

import static gmarques.debtv3.gestores.Meses.mesAtual;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil;

import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.math.RoundingMode;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.FragmentDadosSobreOMesBinding;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyFragment;
import io.realm.RealmList;


public class DadosSobreOMes extends MyFragment {
    private FragmentDadosSobreOMesBinding ui;

    private Mes mesPassado;
    private RealmList<Despesa> despesas;
    private RealmList<Receita> receitas;
    private float receitaTotal;
    private float despesaTotal;
    private float receitaTotalMesPassado;
    private float despesaTotalMesPassado;

    public DadosSobreOMes() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_dados_sobre_o_mes, container, false);
        return ui.getRoot();
    }


    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return Broadcaster.atualizarCardsDeDados;
    }

    @Override
    protected void inicializar() {

        if (mesAtual.getReceitas().size() == 0 || mesAtual.getDespesas().size() == 0) {
            ui.getRoot().setVisibility(View.GONE);
            return;
        } else ui.getRoot().setVisibility(View.VISIBLE);

        carregarDadosBase();
        carregarReceitaDisponivel();
        carregarDespesaTotal();
        carregarReceitaTotal();
        carregarDespesasEmAberto();
        carregarReceitaRecebida();


    }

    private void carregarDadosBase() {

        LocalDate localDate = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1).minusMonths(1);
        mesPassado = Meses.getMes(localDate.getMonthOfYear(), localDate.getYear());

        receitas = mesAtual.getReceitas();
        despesas = mesAtual.getDespesas();

        receitaTotal = 0f;
        despesaTotal = 0f;

        receitaTotalMesPassado = 0f;
        despesaTotalMesPassado = 0f;

        for (Receita receita : receitas)
            receitaTotal = new BigDecimal(receitaTotal).add(new BigDecimal(receita.getValor())).floatValue();

        for (Despesa despesa : despesas)
            despesaTotal = new BigDecimal(despesaTotal).add(new BigDecimal(despesa.getValor())).floatValue();

        if (mesPassado != null) {

            RealmList<Receita> receitas = mesPassado.getReceitas();
            RealmList<Despesa> despesas = mesPassado.getDespesas();

            for (Receita receita : receitas)
                receitaTotalMesPassado = new BigDecimal(receitaTotalMesPassado).add(new BigDecimal(receita.getValor())).floatValue();

            for (Despesa despesa : despesas)
                despesaTotalMesPassado = new BigDecimal(despesaTotalMesPassado).add(new BigDecimal(despesa.getValor())).floatValue();

            if (receitaTotalMesPassado == 0) receitaTotalMesPassado = 1f;
            if (despesaTotalMesPassado == 0) despesaTotalMesPassado = 1f;

        }
    }

    private void carregarReceitaDisponivel() {
        View view = ui.receitaDisponivel;

        ImageView ivIcone = view.findViewById(R.id.ivIcone);
        ImageView ivPorcento = view.findViewById(R.id.ivPorcento);
        TextView tvNome = view.findViewById(R.id.tvNome);
        TextView tvPorcento = view.findViewById(R.id.tvPorcento);
        TextView tvValor = view.findViewById(R.id.tvValor);

        ivIcone.setImageResource(R.drawable.vec_receita);

        float receitaDisponivel = new BigDecimal(receitaTotal).subtract(new BigDecimal(despesaTotal)).floatValue();

        tvValor.setTextColor(UIUtils.corAttr(receitaDisponivel < 0 ? android.R.attr.colorAccent : android.R.attr.colorPrimary));
        tvValor.setText(FormatUtils.emReal(receitaDisponivel));


    }

    private void carregarDespesaTotal() {
        View view = ui.despesaTotal;

        ImageView ivIcone = view.findViewById(R.id.ivIcone);
        ImageView ivPorcento = view.findViewById(R.id.ivPorcento);
        TextView tvNome = view.findViewById(R.id.tvNome);
        TextView tvPorcento = view.findViewById(R.id.tvPorcento);
        TextView tvValor = view.findViewById(R.id.tvValor);

        ivIcone.setImageResource(R.drawable.vec_despesa);
        tvValor.setText(FormatUtils.emReal(despesaTotal));
        tvValor.setTextColor(UIUtils.corAttr(android.R.attr.colorPrimary));

        tvNome.setText(getString(R.string.Despesatotal));
        ivPorcento.setImageResource(0);
        tvPorcento.setText("");


        if (mesPassado != null) {

            if (despesaTotal > despesaTotalMesPassado) {

                float diferença = new BigDecimal(despesaTotal).subtract(new BigDecimal(despesaTotalMesPassado)).floatValue();
                ivPorcento.setImageResource(R.drawable.vec_seta_cima_ruim);
                BigDecimal porcentagem = new BigDecimal(diferença).divide(new BigDecimal(despesaTotalMesPassado), 2, RoundingMode.UP).multiply(new BigDecimal(100));
                tvPorcento.setText(FormatUtils.formatarPorcentagem(porcentagem));

            } else if (despesaTotal < despesaTotalMesPassado) {

                float diferença = new BigDecimal(despesaTotalMesPassado).subtract(new BigDecimal(despesaTotal)).floatValue();
                ivPorcento.setImageResource(R.drawable.vec_seta_baixo_bom);
                BigDecimal porcentagem = new BigDecimal(diferença).divide(new BigDecimal(despesaTotalMesPassado), 2, RoundingMode.UP).multiply(new BigDecimal(100));
                tvPorcento.setText(FormatUtils.formatarPorcentagem(porcentagem));

            }


        }


    }

    private void carregarReceitaTotal() {
        View view = ui.receitaTotal;

        ImageView ivIcone = view.findViewById(R.id.ivIcone);
        ImageView ivPorcento = view.findViewById(R.id.ivPorcento);
        TextView tvNome = view.findViewById(R.id.tvNome);
        TextView tvPorcento = view.findViewById(R.id.tvPorcento);
        TextView tvValor = view.findViewById(R.id.tvValor);

        ivIcone.setImageResource(R.drawable.vec_receita);
        tvValor.setText(FormatUtils.emReal(receitaTotal));
        tvNome.setText(getString(R.string.Receitatotal));


        ivPorcento.setImageResource(0);
        tvPorcento.setText("");

        if (mesPassado != null) {

            float diferença;
            BigDecimal porcentagem;

            if (receitaTotal > receitaTotalMesPassado) {

                diferença = new BigDecimal(receitaTotal).subtract(new BigDecimal(receitaTotalMesPassado)).floatValue();
                ivPorcento.setImageResource(R.drawable.vec_seta_cima_bom);
                porcentagem = new BigDecimal(diferença).divide(new BigDecimal(receitaTotalMesPassado), 2, RoundingMode.UP).multiply(new BigDecimal(100));
                tvPorcento.setText(FormatUtils.formatarPorcentagem(porcentagem));

            } else if (receitaTotal < receitaTotalMesPassado) {

                diferença = new BigDecimal(receitaTotalMesPassado).subtract(new BigDecimal(receitaTotal)).floatValue();
                ivPorcento.setImageResource(R.drawable.vec_seta_baixo_ruim);
                porcentagem = new BigDecimal(diferença).divide(new BigDecimal(receitaTotalMesPassado), 2, RoundingMode.UP).multiply(new BigDecimal(100));
                tvPorcento.setText(FormatUtils.formatarPorcentagem(porcentagem));

            }


        }

    }

    private void carregarDespesasEmAberto() {
        View view = ui.despesasEmAberto;

        ImageView ivIcone = view.findViewById(R.id.ivIcone);
        ImageView ivPorcento = view.findViewById(R.id.ivPorcento);
        TextView tvNome = view.findViewById(R.id.tvNome);
        TextView tvPorcento = view.findViewById(R.id.tvPorcento);
        TextView tvValor = view.findViewById(R.id.tvValor);
        float despesasEmAberto = 0f;
        for (Despesa despesa : despesas)
            if (!despesa.estaPaga())
                despesasEmAberto = new BigDecimal(despesasEmAberto).add(new BigDecimal(despesa.getValor())).floatValue();

        ivIcone.setImageResource(R.drawable.vec_despesa);
        tvValor.setText(FormatUtils.emReal(despesasEmAberto));
        tvNome.setText(getString(R.string.Despesasemaberto));

    }

    private void carregarReceitaRecebida() {
        View view = ui.receitaEmPosse;

        ImageView ivIcone = view.findViewById(R.id.ivIcone);
        ImageView ivPorcento = view.findViewById(R.id.ivPorcento);
        TextView tvNome = view.findViewById(R.id.tvNome);
        TextView tvPorcento = view.findViewById(R.id.tvPorcento);
        TextView tvValor = view.findViewById(R.id.tvValor);

        float receitaEmPosse = 0f;
        for (Receita receita : receitas)
            if (receita.estaRecebido())
                receitaEmPosse = new BigDecimal(receitaEmPosse).add(new BigDecimal(receita.getValor())).floatValue();

        ivIcone.setImageResource(R.drawable.vec_receita);
        tvValor.setText(FormatUtils.emReal(receitaEmPosse));
        tvNome.setText(getString(R.string.Reeceitarecebida));

    }
}
