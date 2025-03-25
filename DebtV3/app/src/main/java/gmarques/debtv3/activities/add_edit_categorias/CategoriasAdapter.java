package gmarques.debtv3.activities.add_edit_categorias;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.ArrayList;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;

/**
 * Criado por Gilian Marques
 * Quinta-feira, 25 de Julho de 2019  as 19:45:06.
 */
class CategoriasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Callback callback;
    private ArrayList<Categoria> categories = new ArrayList<>();

    CategoriasAdapter(final Callback callback) {
        this.callback = callback;
    }

    public void update() {
        categories=new ArrayList<>(Categorias.getCategorias());
        notifyDataSetChanged();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(Debt.binder.get().activity().getLayoutInflater().inflate(R.layout.layout_categoria, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder hdr, int position) {
        MyViewHolder viewHolder = (MyViewHolder) hdr;
        Categoria categoria = categories.get(position);

        viewHolder.ivIcon.setImageDrawable(UIUtils.aplicarTema(Categorias.getIntIcone(categoria.getIcone()), categoria.getCor()));
        viewHolder.tvName.setText(categoria.getNome());

        YoYo.with(Techniques.FadeInUp).duration(200).interpolate(new FastOutSlowInInterpolator()).playOn(viewHolder.itemView);

        ViewGroup.LayoutParams lp = viewHolder.parent.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexGrow(1.0f);
            flexboxLp.setAlignSelf(AlignSelf.AUTO);
        }

    }

    @Override public int getItemCount() {
        return categories.size();
    }

    public void remove(int adapterPosition) {
        categories.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivIcon;
        final TextView tvName;
        public View parent;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            ivIcon = itemView.findViewById(R.id.ivIcone);
            tvName = itemView.findViewById(R.id.tvNome);
            itemView.setOnClickListener(new AnimatedClickListener() {
                @Override public void onClick(View view) {
                    super.onClick(view);
                    callback.onClick(categories.get(getAdapterPosition()), getAdapterPosition());
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    callback.onLongClick(categories.get(getAdapterPosition()), getAdapterPosition());
                    return true;
                }
            });
        }

    }

    public interface Callback {
        void onClick(Categoria categoria, int adapterPosition);

        void onLongClick(Categoria categoria, int adapterPosition);
    }
}
