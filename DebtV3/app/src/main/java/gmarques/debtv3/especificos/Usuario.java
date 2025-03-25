package gmarques.debtv3.especificos;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pixplicity.easyprefs.library.Prefs;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.HashMap;

import gmarques.debtv3.R;
import gmarques.debtv3.nuvem.FBaseNomes;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.utilitarios.C;

public class Usuario {

    private static FirebaseAuth.AuthStateListener mAuthListener;
    private static FirebaseAuth firebaseAuth;

    public static void estaLogado(final Callbackb callback) {

        if (firebaseAuth == null) firebaseAuth = FirebaseAuth.getInstance();

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            callback.resultado(user != null);
            firebaseAuth.removeAuthStateListener(mAuthListener);
        };
        firebaseAuth.addAuthStateListener(mAuthListener);
    }

    public static Uri getFotoDePerfil() {

        if (firebaseAuth == null) firebaseAuth = FirebaseAuth.getInstance();
        //noinspection ConstantConditions
        return firebaseAuth.getCurrentUser().getPhotoUrl();
    }

    public static String getFotoDePerfilGrande() {

        if (firebaseAuth == null) firebaseAuth = FirebaseAuth.getInstance();
        //noinspection ConstantConditions
        String url = firebaseAuth.getCurrentUser().getPhotoUrl().toString();
        url = url.replace("s96-c", "s400-c");

        return url;

    }


    public static FirebaseUser getUsuario() {
        if (firebaseAuth == null) firebaseAuth = FirebaseAuth.getInstance();
        return firebaseAuth.getCurrentUser();
    }

    public static void sair(final Activity activity, final Callbackb callbackb) {
        if (firebaseAuth == null) firebaseAuth = FirebaseAuth.getInstance();
        //noinspection ConstantConditions

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignIn.getClient(activity, signInOptions)
                .signOut()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        firebaseAuth.signOut();
                        callbackb.resultado(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callbackb.resultado(true);
                    }
                });
    }

    /**
     * Envia os dados do usuario para a nuvem oque inclui nome, foto e data de criação da
     * conta caso essa informação nao exista (o usuario tenha acabado de criar a conta)
     **/
    public static void enviarDadosPublicos() {
        FirebaseUser usuario = getUsuario();

        new FirebaseImpl().getDocumentousuario().get()
                .addOnSuccessListener(documentSnapshot -> {

                    Long dataDeCriaçao = documentSnapshot.getLong(FBaseNomes.campodataDeCriaçaoDaConta);
                    HashMap<String, Object> dadosPublicos = new HashMap<>();

                    dadosPublicos.put(FBaseNomes.campoFoto, getFoto());
                    dadosPublicos.put(FBaseNomes.campoNome, usuario.getDisplayName());

                    if (dataDeCriaçao == null)
                        dadosPublicos.put(FBaseNomes.campodataDeCriaçaoDaConta, new LocalDateTime().toDate().getTime());
                    else dadosPublicos.put(FBaseNomes.campodataDeCriaçaoDaConta, dataDeCriaçao);


                    new FirebaseImpl().getDocumentousuario().set(dadosPublicos)
                            .addOnSuccessListener(aVoid -> Prefs.putLong(C.dadosPublicosEnviados, new LocalDate().toDate().getTime()));


                }).addOnFailureListener(e -> {

        });


    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public static String getFoto() {
        return getUsuario().getPhotoUrl().toString();
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public static String getEmail() {
        return getUsuario().getEmail();
    }

    /**
     * este método retorna a conta de sincronismo salvo nas preferências caso haja uma, caso não haja, retorna o e-mail do usuário local
     */
    public static String getContaDeSincronismo() {
        String email = Prefs.getString(C.contaDeSincronismo, null);
        if (email == null) email = getEmail();
        return email;
    }

    /**Metodo ultilitario para saber se o usuario esta sincronizando com outra conta*/
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean estaSincronizandoComOutraConta() {
        return !getEmail().equals(getContaDeSincronismo());
    }


    public interface Callbackb {
        void resultado(boolean tarefaExecutada);
    }
}
