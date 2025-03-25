package gmarques.debtv3.sincronismo;

import androidx.annotation.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import gmarques.debtv3.Debt;
import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.R;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.sincronismo.api.SincAdapter;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class SincAdapterImpl implements SincAdapter.Callback {

    private SincAdapter sincAdapter;
    private FirebaseImpl firebase;
    private SincAdapter.UICallback callback;
    private ArrayList<Sincronizavel> dadosDaNuvem;

    //servem para o controle do sincronismo
    private int opNuvem;
    private boolean opFalhou;
    private String causaDaFalha;

    public SincAdapterImpl(SincAdapter.UICallback callback) {
        this.callback = callback;
        firebase = new FirebaseImpl();
        sincAdapter = new SincAdapter(this);

    }

    public void executar() {
        carregarDadosDaNuvem();
    }

    private void carregarDadosDaNuvem() {
        callback.status("inicializando","Baixando dados da nuvem");

        firebase.getDados((dados, msg) -> {
            dadosDaNuvem = dados;
            if (dados != null) sincAdapter.executar();
            else callback.feito(false, msg);
            assert dados != null;

        });
    }

    @NonNull
    @Override
    public ArrayList<Sincronizavel> getDadosLocal() {
        ArrayList<Sincronizavel> localData = new ArrayList<>();

        Realm realm = Realm.getDefaultInstance();

        RealmResults<Receita> rData = realm.where(Receita.class).findAll();
        RealmResults<Despesa> dData = realm.where(Despesa.class).findAll();
        RealmResults<Categoria> cData = realm.where(Categoria.class).findAll();
        RealmResults<Nota> nData = realm.where(Nota.class).findAll();
        RealmResults<ContaBanco> cbData = realm.where(ContaBanco.class).findAll();
        RealmResults<Objetivo> objData = realm.where(Objetivo.class).findAll();

        localData.addAll(realm.copyFromRealm(rData));
        localData.addAll(realm.copyFromRealm(dData));
        localData.addAll(realm.copyFromRealm(cData));
        localData.addAll(realm.copyFromRealm(nData));
        localData.addAll(realm.copyFromRealm(cbData));
        localData.addAll(realm.copyFromRealm(objData));

        realm.close();

        callback.status("Carregando...","dados da locais carregados " + localData.size());

        return localData;
    }

    @NonNull
    @Override
    public ArrayList<Sincronizavel> getDadosNuvem() {
        callback.status("Carregando...","dados da nuvem carregados " + dadosDaNuvem.size());
        return dadosDaNuvem;
    }

    @Override
    public void removerDefinitivamenteLocal(Sincronizavel obj) {
        MyRealm.removerPermanentemente((RealmObject) obj);
        callback.status(Debt.binder.get().getApplicationContext().getString(R.string.Removendodefinitivamente), obj.getNome());
    }

    @Override
    public void removerDefinitivamenteNuvem(Sincronizavel obj) {
        opNuvem++;
        callback.status(Debt.binder.get().getApplicationContext().getString(R.string.Removendodefinitivamente) + " (nuvem) ", obj.getNome());

        firebase.removerObjetoPermanentemente(obj, new FirebaseImpl.CallbackDeStatus() {
            @Override
            public void feito(boolean sucesso, String msg) {
                if (!sucesso) {
                    opFalhou = true;
                    causaDaFalha = msg;
                } else opNuvem--;
            }
        });
    }

    @Override
    public void atualizarObjetoLocal(Sincronizavel nuvemObj) {
        MyRealm.insertOrUpdate((RealmObject) nuvemObj);
        callback.status(Debt.binder.get().getApplicationContext().getString(R.string.Atualizando), nuvemObj.getNome());
    }

    @Override
    public void atualizarObjetoNuvem(Sincronizavel localObj) {
        opNuvem++;
        callback.status(Debt.binder.get().getApplicationContext().getString(R.string.Enviando), localObj.getNome());
        firebase.attObjeto(localObj, new FirebaseImpl.CallbackDeStatus() {
            @Override
            public void feito(boolean sucesso, String msg) {
                if (!sucesso) {
                    opFalhou = true;
                    causaDaFalha = msg;
                } else opNuvem--;
            }
        });
    }

    @Override
    public void addNovoObjetoLocal(Sincronizavel nuvemObj) {
        MyRealm.insert((RealmObject) nuvemObj);
        callback.status(Debt.binder.get().getApplicationContext().getString(R.string.Adicionando), nuvemObj.getNome());

    }

    @Override
    public void addNovoObjetoNuvem(Sincronizavel localObj) {
        opNuvem++;
        callback.status(Debt.binder.get().getApplicationContext().getString(R.string.Enviando), localObj.getNome());

        firebase.addObjeto(localObj, new FirebaseImpl.CallbackDeStatus() {
            @Override
            public void feito(boolean sucesso, String msg) {
                if (!sucesso) {
                    opFalhou = true;
                    causaDaFalha = msg;
                } else opNuvem--;
            }
        });
    }

    @Override
    public void sincronismoConluido() {
        callback.status("feito","Codigo executado, aguardando ops pendentes");

        if (opNuvem == 0) callback.feito(true, null);
        else {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (opFalhou) {
                        cancel();
                        callback.feito(false, causaDaFalha);
                    } else if (opNuvem == 0) {
                        cancel();
                        callback.feito(true, null);
                    } else
                        callback.status("Aguardando conclusão...", MessageFormat.format("Restam {0} operações", opNuvem));
                }
            }, 0, 500);
        }

    }
}
