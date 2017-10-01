package com.blazeey.nearbycamera.Adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blazeey.nearbycamera.EndPoint;
import com.blazeey.nearbycamera.R;

import java.util.List;

/**
 * Created by Blazeey on 10/1/2017.
 */

public class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.MyViewHolder> {

    private Context context;
    private List<EndPoint> endPointList;
    private DiscoverInterface discoverInterface;

    public DiscoverAdapter(Context context, List<EndPoint> endPointList, DiscoverInterface discoverInterface) {
        this.context = context;
        this.endPointList = endPointList;
        this.discoverInterface = discoverInterface;
    }

    @Override
    public DiscoverAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View rootView  = LayoutInflater.from(context).inflate(R.layout.discover_item,parent,false);

        return new MyViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(DiscoverAdapter.MyViewHolder holder, int position) {

        final EndPoint endPoint = endPointList.get(position);

        holder.endPointName.setText(endPoint.getEndPointName());

        holder.endPointName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverInterface.onClick(endPoint);
            }
        });
    }

    @Override
    public int getItemCount() {
        return endPointList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView endPointName;


        public MyViewHolder(View itemView) {
            super(itemView);

            endPointName = (TextView)itemView.findViewById(R.id.endpoint_name);
        }
    }

    public interface DiscoverInterface{

        void onClick(EndPoint endPoint);
    }
}
