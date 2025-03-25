package gmarques.debtv3.nuvem;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.joda.time.LocalDateTime;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gmarques.debtv3.Debt;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.C;
import gmarques.debtv3.utilitarios.GestorId;

public class FirebaseImpl {

    public final DocumentReference sincronismo;
    private final DocumentReference usuarioDoc;
    private FirebaseFirestore db;

    public FirebaseImpl() {
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        db.setFirestoreSettings(settings);

        String coleçao = FBaseNomes.usuarios;
        if (Debt.MODO_DE_TESTES) coleçao = FBaseNomes.testes;

        usuarioDoc = db.collection(coleçao).document(Usuario.getEmail().toLowerCase());
        sincronismo = db.collection(coleçao).document(Usuario.getContaDeSincronismo())
                .collection(FBaseNomes.dados).document(FBaseNomes.bancoDeDados);
    }

    public DocumentReference getDocumentousuario() {
        return usuarioDoc;
    }

    /***
     * retorna uma array nulo em caso de falha pra baixar ou um array com os dados do tipo recebido
     * */

    public <T extends Sincronizavel> void getDadosDoTipo(final Class<T> tipo, final CallbackDeColeçao callbackDeColeçao) {

        final ArrayList<Sincronizavel> dados = new ArrayList<>();

        sincronismo.collection(tipo.getSimpleName()).get().addOnSuccessListener(queryDocumentSnapshots -> {

            List<DocumentSnapshot> resultado = queryDocumentSnapshots.getDocuments();
            Gson gson = new Gson();

            for (DocumentSnapshot snapshot : resultado) {
                String jsonData = gson.toJson(snapshot.getData());
                Sincronizavel objetoDoTipo = gson.fromJson(jsonData, tipo);
                dados.add(objetoDoTipo);
            }
            callbackDeColeçao.recebido(dados, null);
        }).addOnFailureListener(e ->
                callbackDeColeçao.recebido(null, e.getMessage() + C.eCod1));


    }

    public void enviarRelatorioDeErro(String relatorio, final CallbackDeStatus callback) {

        HashMap<String, String> dados = new HashMap<>();

        dados.put(FBaseNomes.relatorio, relatorio);

        /*    Erros/modeloAparelho/idUsuario/Relatorio    */
        String agora = new LocalDateTime().toString("dd-MM-YYYY HH:mm:ss (SSS)");

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        database.getReference(FBaseNomes.Erros)
                .child(GestorId.getInfoDoAparelho())
                .child(GestorId.getIdDoDispositivo() + "")
                .child(agora)
                .setValue(dados)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) callback.feito(true, null);
                    else callback.feito(false, task.getException().getMessage() + C.eCod12);
                });
    }

    public void removerObjetoPermanentemente(Sincronizavel obj, CallbackDeStatus status) {
        Task<Void> task = sincronismo.collection(obj.getClass().getSimpleName())
                .document(obj.getId() + "")
                .delete();
        addListenersContadores(task, status);


    }

    public void addObjeto(Sincronizavel obj, CallbackDeStatus status) {
        Task<Void> task = sincronismo.collection(obj.getClass().getSimpleName())
                .document(obj.getId() + "")
                .set(map(obj));

        addListenersContadores(task, status);
    }

    public void attObjeto(Sincronizavel obj, CallbackDeStatus status) {
        Task<Void> task = sincronismo.collection(obj.getClass().getSimpleName())
                .document(obj.getId() + "")
                .update(map(obj));

        addListenersContadores(task, status);
    }

    public void attData(long stamp, CallbackDeStatus status) {

        HashMap<String, Long> data = new HashMap<>();
        data.put("data", stamp);

        Task<Void> task = sincronismo.collection("ultimo sincronismo")
                .document("data")
                .set(data);

        addListenersContadores(task, status);
    }

    public void getDataUltimaAtt(final CallbackLong status) {

        sincronismo.collection("ultimo sincronismo")
                .document("data").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Object data = task.getResult().get("data");
                    if (data != null) status.feito((Long) data);
                } else status.feito(-9876);

            }
        });
    }

    private void addListenersContadores(Task<Void> task, final CallbackDeStatus status) {
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                status.feito(true, null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                status.feito(false, e.getMessage() + C.eCod13);
            }
        });
    }

    public static Map<String, Object> map(Sincronizavel obj) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();
        return new Gson().fromJson(new Gson().toJson(obj), mType);
    }

    //total de Sincronizaveis baixados da nuvem
    private int dadosRecebidos;
    // A qtd de objetos que herdam Sincronizavel
    private int tiposDeDados = 6;

    /**
     * AO adicionar um novo objeto sincronizavel e configurar este metodo para obter dado deste objeto da nuvem
     * incremente a variavel tiposDeDados ou o metodo nao vai retornar resultado nunca
     */
    public void getDados(final CallbackDeColeçao callbackDeColeçao) {
        dadosRecebidos = 0;
        final ArrayList<Sincronizavel> sincs = new ArrayList<>();

        getDadosDoTipo(Receita.class, (dados, msg) -> {

            if (dados == null) {
                callbackDeColeçao.recebido(null, msg);
            } else {
                dadosRecebidos++;
                sincs.addAll(dados);
                if (dadosRecebidos == tiposDeDados) callbackDeColeçao.recebido(sincs, null);
            }

        });
        getDadosDoTipo(Despesa.class, (dados, msg) -> {

            if (dados == null) {
                callbackDeColeçao.recebido(null, msg);
            } else {
                dadosRecebidos++;
                sincs.addAll(dados);
                if (dadosRecebidos == tiposDeDados) callbackDeColeçao.recebido(sincs, null);
            }

        });
        getDadosDoTipo(Categoria.class, (dados, msg) -> {

            if (dados == null) {
                callbackDeColeçao.recebido(null, msg);
            } else {
                dadosRecebidos++;
                sincs.addAll(dados);
                if (dadosRecebidos == tiposDeDados) callbackDeColeçao.recebido(sincs, null);
            }

        });
        getDadosDoTipo(Nota.class, (dados, msg) -> {

            if (dados == null) {
                callbackDeColeçao.recebido(null, msg);
            } else {
                dadosRecebidos++;
                sincs.addAll(dados);
                if (dadosRecebidos == tiposDeDados) callbackDeColeçao.recebido(sincs, null);
            }

        });
        getDadosDoTipo(ContaBanco.class, (dados, msg) -> {

            if (dados == null) {
                callbackDeColeçao.recebido(null, msg);
            } else {
                dadosRecebidos++;
                sincs.addAll(dados);
                if (dadosRecebidos == tiposDeDados) callbackDeColeçao.recebido(sincs, null);
            }

        });
        getDadosDoTipo(Objetivo.class, (dados, msg) -> {

            if (dados == null) {
                callbackDeColeçao.recebido(null, msg);
            } else {
                dadosRecebidos++;
                sincs.addAll(dados);
                if (dadosRecebidos == tiposDeDados) callbackDeColeçao.recebido(sincs, null);
            }

        });


    }

    public void getDataConfiavel(final ValueEventListener eventListener) {

        final FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference().child(FBaseNomes.dataConfiavel).setValue(ServerValue.TIMESTAMP)
                .addOnSuccessListener(aVoid -> db.getReference().child(FBaseNomes.dataConfiavel).addListenerForSingleValueEvent(eventListener))
                .addOnFailureListener(e -> eventListener.onCancelled(DatabaseError.fromException(e)));
    }


    public interface CallbackDeColeçao {
        void recebido(ArrayList<Sincronizavel> dados, String msg);
    }

    public interface CallbackDeStatus {
        void feito(boolean sucesso, String msg);
    }

    public interface CallbackLong {
        void feito(long data);
    }


}
