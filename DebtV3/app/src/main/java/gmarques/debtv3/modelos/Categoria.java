package gmarques.debtv3.modelos;

import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Categoria extends RealmObject implements Sincronizavel {

    @PrimaryKey
    private long id;
    private long origem;
    private String nome;
    private String icone;
    private int cor;
    private long ultimaAtt;
    private boolean removido;

    public Categoria() {
        this.id = GestorId.getId();
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

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }


    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public int getCor() {
        return cor;
    }

    public void setCor(int cor) {
        this.cor = cor;
    }

    @Override
    public String toString() {
        return "Categoria{" +
                "id=" + id +
                ", origem=" + origem +
                ", nome='" + nome + '\'' +
                ", icone='" + icone + '\'' +
                ", cor=" + cor +
                ", ultimaAtt=" + ultimaAtt +
                ", removido=" + removido +
                '}';
    }
}
