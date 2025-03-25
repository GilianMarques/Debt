package gmarques.debtv3.outros;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.Debt;
import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.C;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.Realm;
import io.realm.RealmObject;

public class MyRealm {


    public static void insert(RealmObject rObj) {

        long ultimaAtualizacaoDBLocal = Data.timeStampUTC();

        if (rObj instanceof Sincronizavel) {

            /*objeto nao é uma atualizaçao baixada da nuvem pelo livesinc, é local, enviado aqui pelo usuario*/
            if (((Sincronizavel) rObj).getOrigem() == GestorId.getIdDoDispositivo()) {
                ((Sincronizavel) rObj).setUltimaAtt();
                Debt.binder.get().liveSinc.addObjeto(rObj, ultimaAtualizacaoDBLocal);
                Prefs.putLong(C.ultimaAtualizacaoDBLocal, ultimaAtualizacaoDBLocal);
            } else {
                /*Objeto veio da nuvem, dessa vez nao devo -modificar/fazer upload- mas reseto a origem para
                 * qua as atualizaçoes feitas aqui sejam enviadas para nuvem*/
                ((Sincronizavel) rObj).resetarOrigem();
            }

        }

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.insert(rObj);
        realm.commitTransaction();
        realm.close();

    }

    public static void insertOrUpdate(RealmObject rObj) {
        long ultimaAtualizacaoDBLocal = Data.timeStampUTC();

        if (rObj instanceof Sincronizavel) {

            /*objeto nao é uma atualizaçao baixada da nuvem pelo livesinc, é local, enviado aqui pelo usuario*/
            if (((Sincronizavel) rObj).getOrigem() == GestorId.getIdDoDispositivo()) {
                ((Sincronizavel) rObj).setUltimaAtt();
                Debt.binder.get().liveSinc.attObjeto(rObj, ultimaAtualizacaoDBLocal);
                Prefs.putLong(C.ultimaAtualizacaoDBLocal, ultimaAtualizacaoDBLocal);
            } else {
                /*Objeto veio da nuvem, dessa vez nao devo -modificar/fazer upload- mas reseto a origem para
                 * qua as atualizaçoes feitas aqui sejam enviadas para nuvem*/
                ((Sincronizavel) rObj).resetarOrigem();
            }

        }

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.insertOrUpdate(rObj);
        realm.commitTransaction();
        realm.close();
    }

    public static void remover(Sincronizavel sinronizavel) {
        sinronizavel.setRemovido(true);
        insertOrUpdate((RealmObject) sinronizavel);
    }


    public static void removerPermanentemente(RealmObject rObj) {

        Realm realm = Realm.getDefaultInstance();


        if (rObj instanceof Despesa)
            rObj = realm.where(Despesa.class).equalTo("id", ((Despesa) rObj).getId()).findFirst();

        else if (rObj instanceof Receita)
            rObj = realm.where(Receita.class).equalTo("id", ((Receita) rObj).getId()).findFirst();

        else if (rObj instanceof Categoria)
            rObj = realm.where(Categoria.class).equalTo("id", ((Categoria) rObj).getId()).findFirst();

        else if (rObj instanceof Nota)
            rObj = realm.where(Nota.class).equalTo("id", ((Nota) rObj).getId()).findFirst();

        else if (rObj instanceof ContaBanco)
            rObj = realm.where(ContaBanco.class).equalTo("id", ((ContaBanco) rObj).getId()).findFirst();

        else if (rObj instanceof Objetivo)
            rObj = realm.where(Objetivo.class).equalTo("id", ((Objetivo) rObj).getId()).findFirst();
        else
            throw new RuntimeException("" + rObj.getClass().getName() + " nao tem codigo fonte correspondente");


        realm.beginTransaction();
        assert rObj != null;
        rObj.deleteFromRealm();
        realm.commitTransaction();
        realm.close();
    }
}
