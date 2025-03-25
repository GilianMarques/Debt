package gmarques.debtv3.modelos;

import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * essa classe serve para salvar t;odo o dinheiro que se tem na conta de banco do usuário (q n esteja registrado como receita) com ela o
 * usuário pode listar por objetivos os valores da conta e toda vez que entrar ou sair dinheiro o
 * próprio aplicativo subtrai o valor do objetivo com menor prioridade
 * <p>
 * funciona assim, se você tem r$ 1000 na conta e tem três objetivos para fazer com esse dinheiro e quer saber quanto sobra você vai adicionar
 * uma conta vai adicionar os três objetivos nessa conta, cada um com seu valor e vai colocar o com maior prioridade por último na lista de objetivos e o com menor prioridade primeiro o
 * aplicativo vai calcular o valor de cada objetivo e dizer quanto vc tem livre pra gastar sem comprometer seus objetivos e sempre que entrar ou sair dinheiro ele vai subtrair o valor do objetivo com menor prioridade
 * caso esteja sobrando dinheiro na conta esse valor vai ser exibido separadamente e se for retirado o dinheiro da conta e ainda tiver dinheiro sobrando o
 * valor subtraído vai ser desta quantia  que está sobrando
 * <p>
 * Os objetivos não ficam salvos na conta dentro de um array, eles ficam soltos no banco de dados e tem
 * com eles  a id da conta de banco para serem associados pelo aplicativo sempre que necessário. mais ou menos como funciona o
 * conceito de meses e despesas + receitas onde as despesas e receitas não ficam salvas em um array dentro dos meses mas tem a id do mês neles para associaçao
 */
public class ContaBanco extends RealmObject implements Sincronizavel {

    @PrimaryKey
    private  long id;
    private long ultimaAtt;
    private boolean removido;
    private long origem;

    private String nomeConta;
    private float valorConta;
    private  String propritario;


    public ContaBanco() {

        this.id = GestorId.getId();
        resetarOrigem();
        propritario = Usuario.getEmail();
    }

    public void setNomeConta(String nomeConta) {
        this.nomeConta = nomeConta;
    }

    public void setValorConta(float valorConta) {
        this.valorConta = valorConta;
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
        return nomeConta;
    }

    public float getValor() {
        return valorConta;
    }


    public String getPropritario() {
        return propritario;
    }

    public void addObjetivo(Objetivo novoObjetivo) {
        novoObjetivo.setContaId(getId());
        MyRealm.insert(novoObjetivo);
    }

    public void attObjetivo(Objetivo objetivo) {
        MyRealm.insertOrUpdate(objetivo);

    }

    public void removerObjetivo(Objetivo objetivo) {
        MyRealm.remover(objetivo);
    }
}
