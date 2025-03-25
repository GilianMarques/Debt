package gmarques.debtv3.modelos;

import org.joda.time.LocalDate;

import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Receita extends RealmObject implements Sincronizavel {

    public static final long AUTOIMPORTADA = 9999998;
    public static final long RECORRENTE = 9999999;

    @PrimaryKey
    private long id;
    private long origem;
    private String nome;
    private long mesId;
    private boolean recebido;
    private long dataDeRecebimento;
    private long dataEmQueFoiRecebido;

    private float valor;
    /*a data nessas variaveis deve apontar até qual
     mes e ano a receita deve ser  importada*/
    private long autoImportarPrimeira;
    private long autoImportarUltima;
    private String observacoes;
    private long ultimaAtt;
    private boolean removido;


    public Receita() {
        this.id = GestorId.getId();
        resetarOrigem();
    }

    public long getId() {
        return id;
    }

    @Override
    public String getNome() {
        return nome;
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


    public void setNome(String nome) {
        this.nome = nome;
    }

    public long getMesId() {
        return mesId;
    }

    public void setMesId(long mesId) {
        this.mesId = mesId;
    }

    public boolean estaRecebido() {
        return recebido;
    }

    public void setRecebida(boolean recebido) {
        if (recebido) setDataEmQueFoiRecebida(new LocalDate());
        else setDataEmQueFoiRecebida(null);
        this.recebido = recebido;
    }

    public void setDataEmQueFoiRecebida(LocalDate dataEmQueFoiRecebida) {
        if (dataEmQueFoiRecebida == null) this.dataEmQueFoiRecebido = 0;
        else {
            /*a data em que uma despesa ou receita foi paga deve ser em algum
           dia do mes de recebimento da mesma. Se esta for de agosto
            sua data em que foi recebida/paga deve ser entre 1-31 do 8
            caso contrario nao sera contabilizada nas movimentaçoes do mes
            que considera as datas em que as receitas/despesas foram pagas/recebidas*/
            if (getDataDeRecebimento() == 0) {
                throw new IllegalStateException("E necesario que a data esteja definida no objeto neste ponto ");
            }

            LocalDate dataDeRecebimento = new LocalDate(getDataDeRecebimento());

            if (dataEmQueFoiRecebida.getMonthOfYear() < dataDeRecebimento.getMonthOfYear()) {
                /*foi paga/recebida com pelo menos um mes de antecedencia entao deve definir esta data para dia 1 do mes de recebimento/pagamento*/
                this.dataEmQueFoiRecebido = dataDeRecebimento.withDayOfMonth(1).toDate().getTime();

            } else if (dataEmQueFoiRecebida.getMonthOfYear() > dataDeRecebimento.getMonthOfYear()) {
                /*foi paga/recebida com pelo menos um mes de atraso entao deve definir esta data para dia 30-31 do mes de recebimento/pagamento*/
                LocalDate data = dataDeRecebimento.plusMonths(1).withDayOfMonth(1);
                this.dataEmQueFoiRecebido = data.minusDays(1).toDate().getTime();

            } else this.dataEmQueFoiRecebido = (dataEmQueFoiRecebida.toDate().getTime());
//////// como o ano das datas nao é considerado nessas verificaçoes é possivel que ainda hajam erros leves na virada de ano por exemplo


        }
    }

    public long getDataEmQueFoiRecebida() {
        return dataEmQueFoiRecebido;
    }

    public long getDataDeRecebimento() {
        return dataDeRecebimento;
    }

    public void setDataDeRecebimento(LocalDate dataDeRecebimento) {
        this.dataDeRecebimento = (dataDeRecebimento.toDate().getTime());


    }

    public long getAutoImportarPrimeira() {
        return autoImportarPrimeira;
    }

    public void setAutoImportarPrimeira(LocalDate autoImportarPrimeira) {
        if (autoImportarPrimeira == null) this.autoImportarPrimeira = 0;
        else this.autoImportarPrimeira = (autoImportarPrimeira.toDate().getTime());
    }

    public long getAutoImportarUltima() {
        return autoImportarUltima;
    }

    public void setAutoImportarUltima(LocalDate autoImportarUltima) {
        if (autoImportarUltima == null) this.autoImportarUltima = 0;
        else this.autoImportarUltima = (autoImportarUltima.toDate().getTime());
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    @Override
    public String toString() {
        return "Receita{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", mesId=" + mesId +
                ", recebido=" + recebido +
                ", dataDeRecebimento=" + dataDeRecebimento +
                ", valor=" + valor +
                ", importarAte=" + autoImportarPrimeira +
                ", observacoes='" + observacoes + '\'' +
                ", ultimaAtt=" + ultimaAtt +
                ", removido=" + removido +
                '}';
    }


}
