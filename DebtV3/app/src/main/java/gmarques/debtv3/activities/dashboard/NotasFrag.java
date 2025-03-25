package gmarques.debtv3.activities.dashboard;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.FragmentNotasBinding;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyFragment;
import gmarques.debtv3.outros.UIRunnable;

import static gmarques.debtv3.gestores.Meses.mesAtual;

/**
 * A simple {@link Fragment} subclass.
 */
public class NotasFrag extends MyFragment {
    FragmentNotasBinding ui;


    public NotasFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_notas, container, false);
        return ui.getRoot();
    }

    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return Broadcaster.atualizarFragNotas;
    }

    @Override
    protected void inicializar() {
        mesAtual = Meses.mesAtual;
        carregarNotas();
    }

    private void carregarNotas() {
        final ArrayList<Nota> notas = mesAtual.getNotas();

        new Thread(new UIRunnable() {

            private ArrayList<View> views = new ArrayList<>();

            @Override
            public void workerThread() {
                for (Nota nota : notas) {
                    View vNota = carregarView(nota);
                    views.add(vNota);
                }
                views.add(0, carregarViewAddNota());

            }

            @Override
            public void uiThread() {
                ui.container.removeAllViews();
                for (View view : views) {
                    ui.container.addView(view);
                }
            }
        }).start();


    }

    private View carregarViewAddNota() {

        View notaView = getLayoutInflater().inflate(R.layout.layout_notas_add, null);

        notaView.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                addNota();
            }
        });
        return notaView;
    }

    private View carregarView(final Nota nota) {
        View notaView = getLayoutInflater().inflate(R.layout.layout_notas, null);
        TextView tvNota = notaView.findViewById(R.id.tvNota);
        tvNota.setText(" • " + nota.getNome());
        if (nota.estaConcluido())
            tvNota.setPaintFlags(tvNota.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        notaView.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirDialogDaNota(nota);
            }
        });
        return notaView;
    }

    private void exibirDialogDaNota(final Nota nota) {
        View addNotaView = getLayoutInflater().inflate(R.layout.layout_add_edit_nota, null);

        final EditText edtTitulo = addNotaView.findViewById(R.id.edtTitulo);
        final EditText edtNota = addNotaView.findViewById(R.id.edtNota);
        final Switch sConcluido = addNotaView.findViewById(R.id.sConcluido);

        edtTitulo.setText(nota.getNome());
        edtNota.setText(nota.getDados());
        sConcluido.setChecked(nota.estaConcluido());

        Sheettalogo dialogo = new Sheettalogo(getActivity());
        dialogo.titulo(getString(R.string.Editarnota))
                .icone(R.drawable.vec_nota)
                .contentView(addNotaView)
                .botaoPositivo(getString(R.string.Concluir), v -> {
                    /*Checando se a nota foi editada*/
                    if (nota.estaConcluido() != sConcluido.isChecked()
                            || !nota.getNome().equals(edtTitulo.getText().toString())
                            || !nota.getDados().equals(edtNota.getText().toString())) {

                        nota.setConcluido(sConcluido.isChecked());
                        nota.setNome(edtTitulo.getText().toString());
                        nota.setDados(edtNota.getText().toString());

                        mesAtual.attNota(nota);
                        inicializar();

                    }
                })
                .botaoNegativo(getString(R.string.Remover), v -> confirmarRemocaoDaNota(nota))

                .show();
    }

    private void confirmarRemocaoDaNota(final Nota nota) {


        Sheettalogo dialogo = new Sheettalogo(getActivity());
        dialogo.titulo(getString(R.string.Porfavorconfirme))
                .icone(R.drawable.vec_nota)
                .mensagem(getString(R.string.Desejamesmoremoverestanota))
                .botaoPositivo(getString(R.string.Cancelar), v -> {
                })
                .botaoNegativo(getString(R.string.Remover), v -> {
                    mesAtual.removerNota(nota);
                    inicializar();
                }).show();


    }

    private void addNota() {
        View addNotaView = getLayoutInflater().inflate(R.layout.layout_add_edit_nota, null);

        final EditText edtTitulo = addNotaView.findViewById(R.id.edtTitulo);
        final EditText edtNota = addNotaView.findViewById(R.id.edtNota);
        final Switch sConcluido = addNotaView.findViewById(R.id.sConcluido);


        Sheettalogo dialogo = new Sheettalogo(getActivity());
        dialogo.titulo(getString(R.string.Novanota))
                .icone(R.drawable.vec_nota)
                .contentView(addNotaView)
                .botaoPositivo(getString(R.string.Concluir), v -> {
                    mesAtual.addNota(new Nota(edtTitulo.getText().toString(), edtNota.getText().toString(), sConcluido.isChecked()));
                    inicializar();
                })
                .botaoNegativo(getString(R.string.Cancelar), v -> {

                }).show();


    }
}
