package gmarques.debtv3.modelos;

import android.util.Log;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.outros.Tag;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class Mes extends RealmObject {
    private int mes;
    private int ano;
    @PrimaryKey
    private int id; /*equivale a mes+ano*/
    @Ignore
    private RealmList<Despesa> arrayDespesas = new RealmList<>();
    @Ignore
    private RealmList<Receita> arrayReceitas = new RealmList<>();

    public Mes(int mes, int ano) {
        this.mes = mes;
        this.ano = ano;
        this.id = Integer.parseInt(mes + "" + ano);
    }

    public Mes() {
        /*Criado para usar com o realm*/
    }

    public int getMes() {
        return mes;
    }

    public int getAno() {
        return ano;
    }

    public long getId() {

        return id;
    }


    public String getNome() {
        Log.d(Tag.AppTag, "Mes.getNome: "+mes+"  "+ano);
        String nomeMes = new DateFormatSymbols().getMonths()[mes - 1];

        Calendar mcalendar = Calendar.getInstance();
        int anoAtual = mcalendar.get(Calendar.YEAR);

        if (anoAtual != getAno()) nomeMes = nomeMes + "/" + this.ano;
        return nomeMes;
    }

    public Despesa getDespesa(long id) {

        for (Despesa despesa : getDespesas()) {
            if (despesa.getId() == id) return despesa;
        }

        return null;
    }

    public RealmList<Despesa> getDespesas() {
        if (arrayDespesas.size() == 0) {
            Realm realm = Realm.getDefaultInstance();
            RealmResults<Despesa> rDesp = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("mesId", getId()).findAll();
            arrayDespesas.addAll(realm.copyFromRealm(rDesp));
            realm.close();
        }


        RealmList<Despesa> despesasCopia = new RealmList<Despesa>();
        despesasCopia.addAll(this.arrayDespesas);


        Collections.sort(despesasCopia, (o1, o2) -> (int) (o1.getDataDePagamento() - o2.getDataDePagamento()));

        Collections.sort(despesasCopia, (o1, o2) -> (int) (Boolean.compare(o1.estaPaga(), o2.estaPaga())));
        return despesasCopia;
    }

    public void addDespesa(Despesa despesa) {
        despesa.setMesId(getId());
        arrayDespesas.add(despesa);
        MyRealm.insert(despesa);
    }

    public void attDespesa(Despesa despesa) {
        RealmList<Despesa> despesas = getDespesas();

        Despesa despesaAlvo = null;

        for (Despesa despesaDaVez : despesas)
            if (despesaDaVez.getId() == despesa.getId()) {
                despesaAlvo = despesaDaVez;
                break;
            }


        if (despesaAlvo == null)
            throw new RuntimeException("Despesa " + despesa.getNome() + " com mesID = " + despesa.getMesId() + " não encontrada no array de despesas do mes " + getNome() + " de id " + getId());
        else {
            if (despesa.estaRemovido()) arrayDespesas.remove(despesaAlvo);
            else arrayDespesas.set(arrayDespesas.indexOf(despesaAlvo), despesa);
            MyRealm.insertOrUpdate(despesa);
        }

    }

    /**
     * Marca a despesa como removida e delega ao metodo attDespesa a funçao de busca-la no array e remove-la
     * de la, atualizando-a tambem no realm. Para  remvover despesas em sequencia de um ou mais meses
     * chame o metodo removerCopias na classe Despesas.
     */
    public void removerDespesa(Despesa despesa) {
        despesa.setRemovido(true);
        attDespesa(despesa);
    }

    public Receita getReceita(long id) {

        for (Receita receita : getReceitas()) {
            if (receita.getId() == id) return receita;
        }

        return null;
    }

    public RealmList<Receita> getReceitas() {
        if (arrayReceitas.size() == 0) {
            Realm realm = Realm.getDefaultInstance();
            RealmResults<Receita> rDesp = realm.where(Receita.class).equalTo("removido", false).and().equalTo("mesId", getId()).findAll();
            arrayReceitas.addAll(realm.copyFromRealm(rDesp));
            realm.close();
        }


        RealmList<Receita> receitasCopia = new RealmList<Receita>();
        receitasCopia.addAll(this.arrayReceitas);

        Collections.sort(receitasCopia, new Comparator<Receita>() {
            @Override
            public int compare(Receita o1, Receita o2) {
                return (int) (o1.getDataDeRecebimento() - o2.getDataDeRecebimento());
            }
        });

        Collections.sort(receitasCopia, new Comparator<Receita>() {
            @Override
            public int compare(Receita o1, Receita o2) {
                return (int) (Boolean.compare(o1.estaRecebido(), o2.estaRecebido()));
            }
        });

        return receitasCopia;
    }

    public void addReceita(Receita receita) {
        receita.setMesId(getId());
        arrayReceitas.add(receita);
        MyRealm.insert(receita);
    }

    public void attReceita(Receita receita) {
        RealmList<Receita> receitas = getReceitas();

        Receita receitaAlvo = null;

        for (Receita receitaDaVez : receitas)
            if (receitaDaVez.getId() == receita.getId()) {
                receitaAlvo = receitaDaVez;
                break;
            }


        if (receitaAlvo == null)
            throw new RuntimeException("Receita " + receita.getNome() + " com mesID = " + receita.getMesId() + " não encontrada no array de receitas do mes " + getNome() + " de id " + getId());
        else {
            if (receita.estaRemovido()) arrayReceitas.remove(receitaAlvo);
            else arrayReceitas.set(arrayReceitas.indexOf(receitaAlvo), receita);
            MyRealm.insertOrUpdate(receita);
        }

    }

    /**
     * Marca a eeceita como removida e delega ao metodo attReceita a funçao de busca-la no array e remove-la
     * de la, atualizando-a tambem no realm. Para  remvover receitas em sequencia de um ou mais meses
     * chame o metodo removerCopias na classe Receitas
     */
    public void removerReceita(Receita receita) {
        receita.setRemovido(true);
        attReceita(receita);
    }

    public ArrayList<Nota> getNotas() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<Nota> rNotas = realm.where(Nota.class)
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", getId())
                .findAll();
        List<Nota> notas = realm.copyFromRealm(rNotas);
        realm.close();

        return (ArrayList<Nota>) notas;
    }

    public void addNota(Nota nota) {
        nota.setMesId(getId());
        MyRealm.insert(nota);
    }

    public void attNota(Nota nota) {
        MyRealm.insertOrUpdate(nota);
    }

    public void removerNota(Nota nota) {
        MyRealm.remover(nota);
    }


}
