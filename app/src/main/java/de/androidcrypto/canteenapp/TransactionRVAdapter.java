package de.androidcrypto.canteenapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TransactionRVAdapter extends RecyclerView.Adapter<TransactionRVAdapter.ViewHolder> {

    // variable for our array list and context
    private ArrayList<TransactionModel> transactionModelArrayList;
    private Context context;

    // constructor
    public TransactionRVAdapter(ArrayList<TransactionModel> transactionModelArrayList, Context context) {
        this.transactionModelArrayList = transactionModelArrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // on below line we are inflating our layout
        // file for our recycler view items.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // on below line we are setting data
        // to our views of recycler view item.
        TransactionModel model = transactionModelArrayList.get(position);
        holder.timestampTV.setText(model.getTimestamp());
        holder.valueTV.setText(model.getValue());
        holder.bookingTV.setText(model.getBooking());
        holder.machineTV.setText(model.getMachine());
        holder.goodTV.setText(model.getGood());

        /*
        // long click means copy the entryPassword
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // There are other constants available in HapticFeedbackConstants like VIRTUAL_KEY, KEYBOARD_TAP
                //HapticFeedbackConstants.CONFIRM
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                //v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);

                // copy to clipboard
                // Gets a handle to the clipboard service.
                ClipboardManager clipboard = (ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", Cryptography.decryptStringAesGcmFromBase64(model.getEntryLoginPassword()));
                // Set the clipboard's primary clip.
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Passwort kopiert", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

         */

        /*
        // below line is to add on click listener for our recycler view item.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // on below line we are calling an intent.
                Intent i = new Intent(context, UpdateEntryActivity.class);
                // below we are passing all our values.
                i.putExtra("entryName", model.getEntryName());
                i.putExtra("entryLoginName", model.getEntryLoginName());
                i.putExtra("entryLoginPassword", model.getEntryLoginPassword());
                i.putExtra("entryCategory", model.getEntryCategory());
                i.putExtra("entryFavourite", model.getEntryFavourite());
                i.putExtra("entryId", model.getEntryId());
                // starting our activity.
                context.startActivity(i);
            }
        });
         */
    }

    @Override
    public int getItemCount() {
        // returning the size of our array list
        return transactionModelArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        // creating variables for our text views.
        private TextView timestampTV, valueTV, bookingTV, machineTV, goodTV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // initializing our text views
            timestampTV = itemView.findViewById(R.id.idTvTrTimestamp);
            valueTV = itemView.findViewById(R.id.idTvTrValue);
            bookingTV = itemView.findViewById(R.id.idTvTrBooking);
            machineTV = itemView.findViewById(R.id.idTvTrMachine);
            goodTV = itemView.findViewById(R.id.idTvTrGood);
        }
    }
}

