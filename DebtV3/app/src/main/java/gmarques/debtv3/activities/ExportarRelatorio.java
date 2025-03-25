package gmarques.debtv3.activities;

import static gmarques.debtv3.outros.Tag.AppTag;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import com.google.gson.GsonBuilder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.joda.time.LocalDate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityExportarRelatorioBinding;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Tag;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;

public class ExportarRelatorio extends MyActivity {
    private ActivityExportarRelatorioBinding ui;
    private EditText emFoco;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_exportar_relatorio);

        setSupportActionBar(ui.toolbar);
        ActionBar actBar = getSupportActionBar();
        assert actBar != null;
        actBar.setTitle(getString(R.string.Exportarrelatorio));


        verificarPermissaoDeEscrita();
        inicializarUI();
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
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        DialogOnAnyDeniedMultiplePermissionsListener.Builder
                                .withContext(ExportarRelatorio.this)
                                .withTitle("Permissão necessária")
                                .withMessage("Não é possível exportar o relatorio sem permissão de escrita")
                                .withButtonText(android.R.string.ok)
                                .withIcon(R.drawable.vec_info)
                                .build();
                        token.continuePermissionRequest();
                    }
                }).check();


    }

    private void inicializarUI() {
        emFoco = ui.edtComeco;


        ui.edtComeco.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                emFoco = ui.edtComeco;
            }
        });


        ui.edtFim.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                emFoco = ui.edtFim;
            }
        });


        ui.calendario.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            LocalDate data = new LocalDate(year, month + 1, dayOfMonth);
            emFoco.setText(FormatUtils.formatarData(data, true).split(",")[1]);
            emFoco.setTag(data.toDate().getTime());
            if (Objects.equals(emFoco, ui.edtComeco)) emFoco = ui.edtFim;
            else if (Objects.equals(emFoco, ui.edtFim)) emFoco = ui.edtComeco;
        });

        ui.exportar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                validarEntradaDoUsuario();
            }
        });

        ui.exportarTudo.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                carregarDados(0, 0);
            }
        });

    }

    private void validarEntradaDoUsuario() {
        if (ui.edtComeco.getTag() == null || ui.edtFim.getTag() == null) {
            UIUtils.erroToasty(getString(R.string.Selecioneumintervalo));
            return;
        }

        long começoPeriodo = (long) ui.edtComeco.getTag();
        long fimPeriodo = (long) ui.edtFim.getTag();

        if (começoPeriodo >= fimPeriodo) {
            UIUtils.erroToasty(getString(R.string.Verifiqueointervaloetentenovamente));
            return;
        }

        carregarDados(começoPeriodo, fimPeriodo);
    }

    private void carregarDados(long começoPeriodo, long fimPeriodo) {

        Realm realm = Realm.getDefaultInstance();

        RealmQuery<Receita> rqReceitas = realm.where(Receita.class)
                .equalTo("removido", false)
                .and().notEqualTo("mesId", Receita.AUTOIMPORTADA)
                .and().notEqualTo("mesId", Receita.RECORRENTE)
                .sort("dataDeRecebimento", Sort.ASCENDING);

        RealmQuery<Despesa> rqDespesas = realm.where(Despesa.class).equalTo("removido", false)
                .and().notEqualTo("mesId", Despesa.RECORRENTE)
                .and().notEqualTo("mesId", Despesa.PARCELADA)
                .sort("dataDePagamento", Sort.ASCENDING);

        if (começoPeriodo != 0 || fimPeriodo != 0) {

            rqReceitas.and().greaterThanOrEqualTo("dataDeRecebimento", começoPeriodo)
                    .and().lessThanOrEqualTo("dataDeRecebimento", fimPeriodo);

            rqDespesas.and().greaterThanOrEqualTo("dataDePagamento", começoPeriodo)
                    .and().lessThanOrEqualTo("dataDePagamento", fimPeriodo);
        }

        List<Receita> receitas = realm.copyFromRealm(rqReceitas.findAll());
        List<Despesa> despesas = realm.copyFromRealm(rqDespesas.findAll());

        realm.close();


        criarRelatorio(receitas, despesas);

    }

    private void criarRelatorio(List<Receita> receitas, List<Despesa> despesas) {


        List<ArrayList<Object>> linhas = criarLinhas(receitas, despesas);
        StringBuilder documento = new StringBuilder();

        for (int i = 0; i < linhas.size(); i++) {
            ArrayList<Object> arrayLinha = linhas.get(i);
            StringBuilder linhaString = new StringBuilder();

            for (int j = 0; j < arrayLinha.size(); j++)
                linhaString.append(arrayLinha.get(j)).append(";");

            Log.d(Tag.AppTag, "ExportarRelatorio.criarRelatorio: " + linhaString.toString());

            documento.append(linhaString.toString().replace("\n", " ").substring(0, linhaString.length() - 1));
            documento.append("\n");
        }


        salvarRelatorioNoArmazenamento(documento);

    }

    private void salvarRelatorioNoArmazenamento(StringBuilder documento) {

        String nomeArquivo = "Debt_relatório.csv";
        File arquivoDaPlanilha = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + nomeArquivo);

        try {
            FileOutputStream out = new FileOutputStream(arquivoDaPlanilha);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "Cp1252"));// "Cp1252" representa o formtao ANSI qeu suporca õ ã ~ e outros aspectos do portugues
            writer.write(documento.toString());
            writer.flush();
            writer.close();
            out.flush();
            out.close();
            compartilharRelatorio(arquivoDaPlanilha);
            //  COMPARTILHA(arquivoDaPlanilha);

        } catch (IOException e) {
            e.printStackTrace();
            UIUtils.erroToasty("Erro criando planilha");
        }
    }

    private void compartilharRelatorio(File arquivoDaPlanilha) {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Context context = getApplicationContext();
        Uri contentUri = FileProvider.getUriForFile(context, "com.login.projetobase.FileProvider", arquivoDaPlanilha);

        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        intentShareFile.setType(URLConnection.guessContentTypeFromName(arquivoDaPlanilha.getName()));
        intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + arquivoDaPlanilha));
        intentShareFile.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentShareFile.putExtra(Intent.EXTRA_STREAM, contentUri);


        //if you need
        //  intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Sharing File Subject");
        //  intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File Description");

        startActivity(Intent.createChooser(intentShareFile, ""));

    }

    private List<ArrayList<Object>> criarLinhas(List<Receita> receitas, List<Despesa> despesas) {
        ArrayList<ArrayList<Object>> linhas = new ArrayList<>();


        /*   TIPO | NOME | VALOR | DATA | ESTADO | CATEGORIA | RECORRENCIA   */
        ArrayList<Object> cabeçalho = new ArrayList<>();
        cabeçalho.add("TIPO");
        cabeçalho.add("NOME");
        cabeçalho.add("VALOR");
        cabeçalho.add("DATA");
        cabeçalho.add("ESTADO");
        cabeçalho.add("CATEGORIA");
        cabeçalho.add("RECORRENCIA");

        linhas.add(cabeçalho);

        for (int i = 0; i < receitas.size(); i++) {

            ArrayList<Object> linha = new ArrayList<>();
            Receita receita = receitas.get(i);

            if (receita != null) {


                linha.add("RECEITA");
                linha.add(receita.getNome());
                linha.add(((receita.getValor() + "").replace(".", ",")));
                linha.add(FormatUtils.formatarDataBasica(receita.getDataDeRecebimento()));
                linha.add(receita.estaRecebido() ? "RECEBIDO" : "PENDENTE");

            }

            linhas.add(linha);
            Log.d(AppTag, "criarLinhas: --- " + new GsonBuilder().create().toJson(linha));

        }

        for (int i = 0; i < despesas.size(); i++) {

            ArrayList<Object> linha = new ArrayList<>();
            Despesa despesa = despesas.get(i);

            if (despesa != null) {


                linha.add("DESPESA");
                linha.add(despesa.getNome());
                linha.add(((despesa.getValor() + "").replace(".", ",")));
                linha.add(FormatUtils.formatarDataBasica(despesa.getDataDePagamento()));
                linha.add(despesa.estaPaga() ? "PAGO" : "PENDENTE");
                linha.add(Categorias.getCategoria(despesa.getCategoriaId()).getNome());
                if (despesa.getParcelas() != null)
                    linha.add((FormatUtils.formatarParcelas(despesa.getParcelas())).toUpperCase(Locale.ROOT));


            }

            linhas.add(linha);
            Log.d(AppTag, "criarLinhas: --- " + new GsonBuilder().create().toJson(linha));


        }

        return linhas;
    }


}