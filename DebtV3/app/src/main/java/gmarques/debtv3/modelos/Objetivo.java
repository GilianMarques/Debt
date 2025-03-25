package gmarques.debtv3.modelos;

import org.joda.time.LocalDate;

import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * essa classe deve ser usada junto com a classe @{@link ContaBanco}.
 * É nesta classe onde o usuário vai definir o nome e o valor dos objetivos  para o dinheiro  que tem na conta do banco
 */
public class Objetivo extends RealmObject implements Sincronizavel {


    private long contaId;
    @PrimaryKey
    private long id;
    private long ultimaAtt;
    private boolean removido;
    private long origem;

    private long dataDeCriacao;
    private long dataDeConclusao;
    private boolean concluido;
    private String nomeObjetivo;
    private float valorObjetivo;


    public Objetivo() {

        this.id = GestorId.getId();
        this.dataDeCriacao = new LocalDate().toDate().getTime();
        resetarOrigem();
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


    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getNome() {
        return nomeObjetivo;
    }

    public float getValor() {
        return valorObjetivo;
    }

    public boolean estaConcluido() {
        return concluido;
    }

    public long getDataDeCriacao() {
        return dataDeCriacao;
    }

    public long getDataDeConclusao() {
        return dataDeConclusao;
    }

    public long getContaId() {
        return contaId;
    }

    public void setContaId(long contaId) {
        this.contaId = contaId;
    }


    public void setConcluido(boolean concluido) {
        this.concluido = concluido;

        if (this.concluido) this.dataDeConclusao = new LocalDate().toDate().getTime();
        else this.dataDeConclusao = 0;
    }


    public void setNomeObjetivo(String nomeObjetivo) {
        this.nomeObjetivo = nomeObjetivo;
    }


    public void setValor(float valorObjetivo) {
        this.valorObjetivo = valorObjetivo;
    }
}
