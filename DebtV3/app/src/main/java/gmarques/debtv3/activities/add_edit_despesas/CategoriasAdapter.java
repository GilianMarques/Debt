package gmarques.debtv3.activities.add_edit_despesas;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.ArrayList;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;

public class CategoriasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Categoria> categorias;
    private Callback callback;
    private int indiceSelecionado = -999;
    private int corSelecionada = UIUtils.corAttr(android.R.attr.colorPrimary);
    private int corPadrao = UIUtils.corAttr(R.attr.appTextSecondary);

    public CategoriasAdapter(ArrayList<Categoria> categorias, Callback callback) {
        this.categorias = categorias;
        this.callback = callback;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoriasAdapter.Holder(Debt.binder.get().activity().getLayoutInflater().inflate(R.layout.layout_categoria, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder hdr, int position) {
        Holder viewHolder = (Holder) hdr;
        Categoria categoria = categorias.get(position);

        if (position == indiceSelecionado) {
            viewHolder.ivIcone.setImageDrawable(UIUtils.aplicarTema(Categorias.getIntIcone(categoria.getIcone()), corSelecionada));
            viewHolder.tvNome.setTextColor(corSelecionada);
        } else {
            viewHolder.ivIcone.setImageDrawable(UIUtils.aplicarTema(Categorias.getIntIcone(categoria.getIcone()), corPadrao));
            viewHolder.tvNome.setTextColor(corPadrao);
        }

        viewHolder.tvNome.setText(categoria.getNome());

        ViewGroup.LayoutParams lp = viewHolder.parent.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexGrow(1.0f);
            flexboxLp.setAlignSelf(AlignSelf.AUTO);
        }

    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    public int setSeleçaoEChamarCallback(long categoriaId) {
        for (int i = 0; i < categorias.size(); i++)
            if (categorias.get(i).getId() == categoriaId) {
                indiceSelecionado = i;
                break;
            }
        notifyItemChanged(indiceSelecionado);
        callback.categoriaSelecionada(categorias.get(indiceSelecionado));
        return indiceSelecionado;
    }

    class Holder extends RecyclerView.ViewHolder {

        ImageView ivIcone;
        TextView tvNome;
        View parent;

        public Holder(@NonNull View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            tvNome = itemView.findViewById(R.id.tvNome);
            ivIcone = itemView.findViewById(R.id.ivIcone);
            itemView.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    int pos = getAdapterPosition();
                    indiceSelecionado = pos;
                    callback.categoriaSelecionada(categorias.get(pos));
                    notifyDataSetChanged();


                }
            });
        }
    }

    interface Callback {
        void categoriaSelecionada(Categoria categoria);
    }
}
