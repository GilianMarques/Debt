package gmarques.debtv3.modelos;

import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Nota extends RealmObject implements Sincronizavel {

    private String nome;
    private long origem;
    private String dados;
    private long ultimaAtt;
    private boolean removido;
    @PrimaryKey
    private long id;
    private long mesId;
    private boolean concluido;


    public Nota() {
        this.id = GestorId.getId();
        resetarOrigem();

    }

    public Nota(String nome, String dados,boolean concluido) {
        this.nome = nome;
        this.dados = dados;
        this.concluido = concluido;
        this.id = GestorId.getId();
        resetarOrigem();

    }


    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setDados(String dados) {
        this.dados = dados;
    }

    public String getDados() {
        return dados;
    }

    public boolean estaConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }

    public long getMesId() {
        return mesId;
    }

    public void setMesId(long mesId) {
        this.mesId = mesId;
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
        return nome;
    }

    @Override
    public String toString() {
        return "Nota{" +
                "nome='" + nome + '\'' +
                ", origem=" + origem +
                ", dados='" + dados + '\'' +
                ", ultimaAtt=" + ultimaAtt +
                ", removido=" + removido +
                ", id=" + id +
                ", mesId=" + mesId +
                ", concluido=" + concluido +
                '}';
    }
}
