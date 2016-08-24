package org.amoradi.syncopoli;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> implements IBackupItemClickHandler {
    IBackupHandler mBackupHandler;
    private static Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        IBackupItemClickHandler mBackupClickHandler;

        public TextView mProfileTextView;
        public TextView mSrcTextView;
        public View mView;

        public ViewHolder(View v, IBackupItemClickHandler handler) {
            super(v);
            LinearLayout l = (LinearLayout) v.findViewById(R.id.backup_item_info);

            l.setOnClickListener(this);
            l.setOnLongClickListener(this);

            mView = l;
            mBackupClickHandler = handler;
            mProfileTextView = (TextView) v.findViewById(R.id.backup_item_profile_text);
            mSrcTextView = (TextView) v.findViewById(R.id.backup_item_source);
        }

        @Override
        public void onClick(View v) {
            if (v instanceof LinearLayout) {
                mBackupClickHandler.onBackupShowLog(getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (v instanceof LinearLayout) {
                final CharSequence[] items = {"Edit Profile", "Delete Profile"};

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

                builder.setTitle("Select The Action");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            mBackupClickHandler.onBackupEdit(getAdapterPosition());
                        } else if (item == 1) {
                            mBackupClickHandler.onBackupDelete(getAdapterPosition());
                        }
                    }
                });
                builder.show();
                return true;
            }
            return false;
        }
    }

    public BackupAdapter(IBackupHandler handler, Context ctx) {
        mBackupHandler = handler;
        mContext = ctx;
    }

    @Override
    public BackupAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.backup_item, parent, false);
        ViewHolder vh = new ViewHolder(v.findViewById(R.id.backup_item), this);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int pos) {
        holder.mProfileTextView.setText(mBackupHandler.getBackups().get(pos).name);

        if (mBackupHandler.getBackups().get(pos).lastUpdate == null) {
            holder.mSrcTextView.setText("This backup has never run");
        } else {
            holder.mSrcTextView.setText("Last update: " + mBackupHandler.getBackups().get(pos).lastUpdate.toString());
        }

        holder.mView.setTranslationX(holder.mView.getTranslationX() -50f);
        holder.mView.setAlpha(0f);
        holder.mView.animate()
                .setDuration(200)
                .setStartDelay(holder.getLayoutPosition() * 50)
                .translationXBy(50f)
                .alpha(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    @Override
    public int getItemCount() {
        return mBackupHandler.getBackups().size();
    }

    public void onBackupShowLog(int pos) {
        mBackupHandler.showLog(mBackupHandler.getBackups().get(pos));
    }

    public void onBackupDelete(int pos) {
        mBackupHandler.removeBackup(mBackupHandler.getBackups().get(pos));
        notifyDataSetChanged();
    }

    public void onBackupEdit(int pos) {
        mBackupHandler.editBackup(mBackupHandler.getBackups().get(pos));
    }
}
