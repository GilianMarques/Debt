package gmarques.debtv3.activities.ver_despesas;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import io.realm.RealmList;


/**
 * Criado por Gilian Marques
 * Sábado, 20 de Julho de 2019  as 16:51:59.
 */
public class DespesasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Despesa> despesas;
    private final Callback callback;
    private final Activity activity;
    private int corIcone;

    DespesasAdapter(List<Despesa> despesas, Activity activity, Callback callback) {
        this.despesas = despesas;
        this.activity = activity;
        this.callback = callback;
        corIcone = UIUtils.corAttr(android.R.attr.colorPrimary);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.layout_despesas_rv, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        YoYo.with(Techniques.FadeOut).duration(0).playOn(holder.itemView);


        MyViewHolder mHolder = (MyViewHolder) holder;

        Despesa despesa = despesas.get(position);
        Categoria categoria = Categorias.getCategoria(despesa.getCategoriaId());

        mHolder.tvNome.setText(despesa.getNome());
        mHolder.tvValor.setText(FormatUtils.emReal(despesa.getValor()));


        mHolder.ivIcone.setImageDrawable(UIUtils.aplicarTema(Categorias.getIntIcone(categoria.getIcone()), corIcone));

        mHolder.tvPaid.setVisibility(despesa.estaPaga() ? View.VISIBLE : View.GONE);

        mHolder.tvDataPgto.setText(FormatUtils.formatarData(despesa.getDataDePagamento(), false));


        YoYo.with(Techniques.FadeIn).duration(200).playOn(mHolder.itemView);

        ViewGroup.LayoutParams lp = mHolder.parent.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexGrow(1.0f);
            flexboxLp.setAlignSelf(AlignSelf.AUTO);
        }


    }


    @Override
    public int getItemCount() {
        return despesas.size();
    }

    public void atualizarItem(Despesa despesa, int adapterPosition) {
        despesas.set(adapterPosition, despesa);
        notifyItemChanged(adapterPosition);
    }

    public void atualizar(RealmList<Despesa> despesas) {
        this.despesas.clear();
        this.despesas.addAll(despesas);
        notifyDataSetChanged();
    }


    // stores and recycles views as they are scrolled off screen
    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNome;
        private final TextView tvDataPgto;
        private final TextView tvValor;
        private final TextView tvPaid;
        private final LinearLayout iconContainer;
        private final ImageView ivIcone;
        private final CardView cv;
        View parent;

        MyViewHolder(final View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            tvNome = itemView.findViewById(R.id.tvNome);
            tvPaid = itemView.findViewById(R.id.tvpago);
            tvDataPgto = itemView.findViewById(R.id.tvDataPgto);
            cv = itemView.findViewById(R.id.cv);
            parent = itemView.findViewById(R.id.parent);
            tvValor = itemView.findViewById(R.id.tvValor);
            ivIcone = itemView.findViewById(R.id.ivIcone);
            itemView.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);

                    callback.onClick(getAdapterPosition(), despesas.get(getAdapterPosition()), itemView);
                }
            });
        }


    }

    public void removerItem(int pos) {
        despesas.remove(pos);
        notifyItemRemoved(pos);
    }

    public interface Callback {


        void onClick(int adapterPosition, Despesa despesa, View itemView);
    }
}
