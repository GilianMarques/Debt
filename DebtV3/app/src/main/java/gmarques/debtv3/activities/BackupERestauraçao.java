package gmarques.debtv3.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityBackupBinding;
import gmarques.debtv3.especificos.RealmBackup;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.outros.Broadcaster;

public class BackupERestauraçao extends MyActivity {
    private ActivityBackupBinding ui;
    private RealmBackup realmBackup;
    private Typeface fonte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_backup);

        setSupportActionBar(ui.toolbar);
        realmBackup = new RealmBackup();
        verificarPermissaoDeEscrita();
        inicializarBotoes();
    }

    private void verificarPermissaoDeEscrita() {

        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (!report.areAllPermissionsGranted()) finish();
                else {
                    carregarBackups();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                token.continuePermissionRequest();
            }
        }).check();


    }

    private void carregarBackups() {
        File pasta = realmBackup.getBackupPasta();
        if (!pasta.exists()) return;

        ui.container.removeAllViews();

        File[] arquivos = pasta.listFiles();
        if (arquivos == null) return;

        Arrays.sort(arquivos, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName());
            }
        });

        for (File backup : arquivos) {

            View view = criarView(backup);
            ui.container.addView(view);

        }

    }

    private View criarView(final File backup) {

        View view = getLayoutInflater().inflate(R.layout.layout_backup_item, null, false);
        final TextView tvNome = view.findViewById(R.id.tvNome);

        String nome = backup.getName().split("\\.")[0];
        Spannable span = new SpannableString(nome);
        span.setSpan(new RelativeSizeSpan(0.55f), nome.split("\n")[0].length(), nome.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvNome.setText(span);

        view.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                confirmarRestauraçao(backup, tvNome.getText().toString());
            }
        });
        return view;

    }

    private void confirmarRestauraçao(final File backup, final String nome) {
        final String nomeDeUmaLinha = nome.split("\n")[0];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(nomeDeUmaLinha);
        builder.setMessage("Deseja mesmo restaurar este backup? \n\nEsta ação pode ser desfeita posteriormente.");
        builder.setCancelable(true)
                .setPositiveButton("Resuaturar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        realmBackup.fazerBackup(nomeDeUmaLinha);
                        realmBackup.restaurar(backup);
                        Broadcaster.atualizarMainActivity();
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        final AlertDialog alertdialog = builder.create();
        alertdialog.show();

        //       alertdialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void inicializarBotoes() {
        ui.btnBackup.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                realmBackup.fazerBackup(null);
                carregarBackups();
            }
        });
    }


}