package gmarques.debtv3.modelos;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.LocalDate;
import org.joda.time.Months;

import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Despesa extends RealmObject implements Sincronizavel {

    public static final long PARCELADA = 9999998;
    public static final long RECORRENTE = 9999999;

    @PrimaryKey
    private long id;
    private long origem;
    private long categoriaId;
    private long mesId;

    private String nome;
    private float valor;
    private boolean pago;
    private String observacoes;
    private long dataDePagamento;
    private long dataEmQueFoiPago;
    /*a data nessa variavel deve apontar até qual
       mes e ano a despesa deve ser  importada*/
    private long ultimaParcela;
    private long primeiraparcela;
    /*UTC pra nao cagar com o sincronismo em fuso-horarios diferentes*/
    private long ultimaAtt;
    private boolean removido;


    //private boolean removidso;

    public Despesa() {

        this.id = GestorId.getId();
        resetarOrigem();
    }

    public long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public long getMesId() {
        return mesId;
    }

    public void setMesId(long mesId) {
        this.mesId = mesId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public long getDataDePagamento() {
        return dataDePagamento;
    }

    public void setDataDePagamento(LocalDate dataDePagamento) {
        this.dataDePagamento = (dataDePagamento.toDate().getTime());
    }


    public boolean estaPaga() {
        return pago;
    }

    public void setPaga(boolean pago) {

        if (pago) setDataEmQueFoiPaga(new LocalDate());
        else setDataEmQueFoiPaga(null);

        this.pago = pago;
    }

    public void setDataEmQueFoiPaga(LocalDate dataEmQueFoiPago) {
        if (dataEmQueFoiPago == null) this.dataEmQueFoiPago = 0;
        else {
         /*a data em que uma despesa ou receita foi paga deve ser em algum
           dia do mes de recebimento da mesma. Se esta for de agosto
            sua data em que foi recebida/paga deve ser entre 1-31 do 8
            caso contrario nao sera contabilizada nas movimentaçoes do mes
            que considera as datas em que as receitas/despesas foram pagas/recebidas*/
            if (getDataDePagamento() == 0) {
                throw new IllegalStateException("E necesario que a data esteja definida no objeto neste ponto ");
            }

            LocalDate dataDePagamento = new LocalDate(getDataDePagamento());

            if (dataEmQueFoiPago.getMonthOfYear() < dataDePagamento.getMonthOfYear()) {
                /*foi paga/recebida com pelo menos um mes de antecedencia entao deve definir esta data para dia 1 do mes de recebimento/pagamento*/
                this.dataEmQueFoiPago = dataDePagamento.withDayOfMonth(1).toDate().getTime();

            } else if (dataEmQueFoiPago.getMonthOfYear() > dataDePagamento.getMonthOfYear()) {
                /*foi paga/recebida com pelo menos um mes de atraso entao deve definir esta data para dia 30-31 do mes de recebimento/pagamento*/
                LocalDate data = dataDePagamento.plusMonths(1).withDayOfMonth(1);
                this.dataEmQueFoiPago = data.minusDays(1).toDate().getTime();
            } else this.dataEmQueFoiPago = (dataEmQueFoiPago.toDate().getTime());
            //////// como o ano das datas nao é considerado nessas verificaçoes é possivel que ainda hajam erros leves nas viradas de ano por exemplo
        }
    }


    public long getDataEmQueFoiPaga() {
        return dataEmQueFoiPago;
    }

    public long getId() {
        return id;
    }


    @Override
    public long getUltimaAtt() {
        return ultimaAtt;
    }

    @Override
    public void setUltimaAtt() {
        ultimaAtt = Data.timeStampUTC();
    }

    @Override
    public boolean estaRemovido() {
        return removido;
    }

    @Override
    public void setRemovido(boolean removido) {
        this.removido = removido;
    }

    @Override
    public long getOrigem() {
        return origem;
    }

    @Override
    public void resetarOrigem() {
        this.origem = GestorId.getIdDoDispositivo();
    }

    @Nullable
    public int[] getParcelas() {
        Log.d(Tag.AppTag, "Despesa.getParcelas: "+primeiraparcela+" "+ultimaParcela);
        if (this.primeiraparcela <= 0 || this.ultimaParcela <= 0) return null;
        LocalDate ultimaParcela = new LocalDate(this.ultimaParcela).withDayOfMonth(1);
        LocalDate primeiraParcela = new LocalDate(this.primeiraparcela).withDayOfMonth(1);
        LocalDate mesAtual = new LocalDate(getDataDePagamento()).withDayOfMonth(1);

        int totalParcelas = Months.monthsBetween(primeiraParcela, ultimaParcela).getMonths();
        int parcelaAtual = Months.monthsBetween(primeiraParcela, mesAtual).getMonths();
        return new int[]{parcelaAtual, totalParcelas};
    }

    public long getUltimaParcela() {
        return ultimaParcela;
    }

    public void setParcelas(LocalDate primeiraparcela, LocalDate ultimaParcela) {
        if (primeiraparcela == null && ultimaParcela == null) {
            this.primeiraparcela = 0;
            this.ultimaParcela = 0;
        } else {
            this.primeiraparcela = (primeiraparcela.toDate().getTime());
            this.ultimaParcela = (ultimaParcela.toDate().getTime());
        }
    }

    @NonNull
    /**
     * Retorna os longs primeiraParcela e ultimaParcela. Para obter as parcelas formatadas (03x12) chame getParcelas()*/
    public long[] getParcelasLong() {
        return new long[]{primeiraparcela, ultimaParcela};
    }

    @Override
    public String toString() {
        return "Despesa{" +
                "id=" + id +
                ", origem=" + origem +
                ", categoriaId=" + categoriaId +
                ", mesId=" + mesId +
                ", nome='" + nome + '\'' +
                ", valor=" + valor +
                ", pago=" + pago +
                ", observacoes='" + observacoes + '\'' +
                ", dataDePagamento=" + new LocalDate(dataDePagamento) +
                ", dataEmQueFoiPago=" + dataEmQueFoiPago +
                ", ultimaParcela=" + ultimaParcela +
                ", primeiraparcela=" + primeiraparcela +
                ", ultimaAtt=" + ultimaAtt +
                ", removido=" + removido +
                '}';
    }
}
