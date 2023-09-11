package com.example.blechatdemo;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blechatdemo.databinding.ReceiverViewBinding;
import com.example.blechatdemo.databinding.SenderViewBinding;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private int SENDER_VIEW = 0;
    private int RECEIVER_VIEW = 1;
    List<Message> messageList;
    ChatItemListener callback; //new code
    interface ChatItemListener {//new code
         void onItemClicked(Message message);
     }

    public void reload(List<Message> data){
        this.messageList = data;
        notifyDataSetChanged();
    }

    public ChatAdapter(List<Message>data,ChatItemListener delegate){

        this.messageList = data;
        this.callback = delegate; //new code
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if(message.getSender()!= null && message.getSender().equals(ChatService.getInstance().currentAddress)) {
            return SENDER_VIEW;
        }
        return RECEIVER_VIEW;
//        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SENDER_VIEW) {
            SenderViewBinding binding = SenderViewBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new Senderholder(binding);
        }else {
            ReceiverViewBinding binding = ReceiverViewBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new Receiverholder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if(holder instanceof Senderholder) {
            Senderholder sederholder = (Senderholder) holder;
            //new code instead of if ( message.getConteType() != null && message.getConteType().equals("FILE"))
            if (message.getText().startsWith("file://")) { //message.getConteType() != null &&
                sederholder.binding.messageView.setText("View File");
            } else {
                sederholder.binding.messageView.setText(message.getText());
            }

        }else{

            Receiverholder receiverholder = (Receiverholder)holder;
            //new code instead of if( message.getConteType() != null && message.getConteType().equals("FILE"))
            if (message.getText().startsWith("file://")) { //message.getConteType() != null &&
                receiverholder.binding.messageView.setText("View File"); //new code
            }else {   //new code if (message != null)
            receiverholder.binding.messageView.setText(message.getText());
        }

    }
            holder.itemView.setOnClickListener(v->{ //new code
                Log.d("Open message",message.getConteType());
                callback.onItemClicked(message);

            });
        }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class  Senderholder extends  RecyclerView.ViewHolder {
        SenderViewBinding binding;
        public Senderholder(@NonNull SenderViewBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;
        }
    }
    public class Receiverholder extends  RecyclerView.ViewHolder {
        ReceiverViewBinding binding;
        public Receiverholder(@NonNull ReceiverViewBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;
        }
    }
}
