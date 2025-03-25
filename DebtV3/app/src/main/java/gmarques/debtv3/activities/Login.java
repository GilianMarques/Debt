package gmarques.debtv3.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.text.MessageFormat;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityLoginBinding;
import gmarques.debtv3.especificos.ConexaoComAInternet;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.interface_.AnimatedClickListener;

public class Login extends MyActivity {


    private static final int reqCodLogin = 123;
    private ActivityLoginBinding ui;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_login);

        verificarConexaoComAInternet();
        inicializarBotaoTentarNovamente();

    }

    private void inicializarBotaoTentarNovamente() {
        ui.btnTentarNovamente.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                ui.progressBar.setVisibility(View.VISIBLE);
                ui.btnTentarNovamente.setVisibility(View.GONE);
                ui.tvInfo.setText("");
                verificarConexaoComAInternet();
            }
        });
    }

    private void verificarConexaoComAInternet() {

        new ConexaoComAInternet().verificar(new ConexaoComAInternet.Callback() {
            @Override
            public void conclusao(boolean conectado) {
                if (conectado) {
                    inicializarObjetos();
                    fazerLogin();
                } else {
                    ui.progressBar.setVisibility(View.GONE);
                    ui.btnTentarNovamente.setVisibility(View.VISIBLE);

                    ui.tvInfo.setText(R.string.Vocenaoestaconectadoainternetouadata);
                }
            }
        });
    }

    private void inicializarObjetos() {
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }


    /**
     * a exeuçao continua a partir do onActivityResult
     */
    private void fazerLogin() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, reqCodLogin);
    }

    @Override
    public void onActivityResult(int reqCodeLogin, int resultCode, Intent data) {
        super.onActivityResult(reqCodeLogin, resultCode, data);


        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            //noinspection ConstantConditions
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            ui.progressBar.setVisibility(View.GONE);
            ui.btnTentarNovamente.setVisibility(View.VISIBLE);
            if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                ui.tvInfo.setText(R.string.Vocecancelouologin);
            } else if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS) {
                ui.tvInfo.setText(R.string.Hamaisdeumprocessodeloginemandamentoquantas);
            } else if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_FAILED) {
                ui.tvInfo.setText(MessageFormat.format(getString(R.string.Ologinfalhou), e.getStatusCode()));
            } else {
                ui.tvInfo.setText(MessageFormat.format(getString(R.string.Houveumerroaocontactaraapidogoole), e.getStatusCode()));
            }

        }
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {


        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            FirebaseUser user = mAuth.getCurrentUser();
                            assert user != null;
                            ui.progressBar.setVisibility(View.GONE);
                            String nome = user.getDisplayName() != null ? user.getDisplayName() : "?";
                            ui.tvInfo.setText(MessageFormat.format(getString(R.string.BemvindoX), nome.split(" ")[0]));
                            Usuario.enviarDadosPublicos();
                            new Handler().postDelayed(() -> {

                                startActivity(new Intent(getApplicationContext(), TermosDeUso.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                finishAffinity();

                            }, 2000);

                        } else {
                            ui.progressBar.setVisibility(View.GONE);
                            ui.btnTentarNovamente.setVisibility(View.VISIBLE);
                            Exception ex = task.getException();
                            if (ex != null)
                                ui.tvInfo.setText(MessageFormat.format(getString(R.string.Houveumerroaoautenticarsuacontatente), ex.getMessage()));
                        }


                    }
                });
    }


}