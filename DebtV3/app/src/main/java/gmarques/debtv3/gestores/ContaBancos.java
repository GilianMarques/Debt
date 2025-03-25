package gmarques.debtv3.gestores;

import java.util.ArrayList;

import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.outros.MyRealm;
import io.realm.Realm;
import io.realm.RealmResults;

public class ContaBancos {
    public static ArrayList<ContaBanco> getContas() {

        Realm realm = Realm.getDefaultInstance();

        RealmResults<ContaBanco> rContas = realm.where(ContaBanco.class).equalTo("removido", false).findAll();
        ArrayList<ContaBanco> contas = new ArrayList<>(realm.copyFromRealm(rContas));
        realm.close();

        return contas;
    }

    public static ArrayList<Objetivo> getObjetivos(ContaBanco conta) {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<Objetivo> rObjetivos = realm.where(Objetivo.class).equalTo("removido", false).and().equalTo("contaId",conta.getId()).findAll();
        ArrayList<Objetivo> objetivos = new ArrayList<>(realm.copyFromRealm(rObjetivos));
        realm.close();

        return objetivos;

    }

    public static void addConta(ContaBanco novaConta) {
        MyRealm.insert(novaConta);
    }

    public static void attConta(ContaBanco conta) {
        MyRealm.insertOrUpdate(conta);

    }

    public static void removerContaBanco(ContaBanco conta) {
        MyRealm.remover(conta);
    }
}
