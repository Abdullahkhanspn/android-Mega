package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.RecentsItem;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;

public class RecentsAdapter extends RecyclerView.Adapter<RecentsAdapter.ViewHolderBucket> implements View.OnClickListener, SectionTitleProvider {

    private Object fragment;
    private Context context;
    private MegaApiAndroid megaApi;

    private DisplayMetrics outMetrics;

    private ArrayList<RecentsItem> recentsItems;

    public RecentsAdapter (Context context, Object fragment, ArrayList<RecentsItem> items) {
        log("new RecentsAdapter");
        this.context = context;
        this.fragment = (RecentsFragment)fragment;
        setItems(items);

        megaApi = ((MegaApplication)((Activity)context).getApplication()).getMegaApi();
    }

    public class ViewHolderBucket extends RecyclerView.ViewHolder {

        RelativeLayout headerLayout;
        TextView headerText;
        RelativeLayout itemBucketLayout;
        ImageView imageThumbnail;
        TextView title;
        TextView actionBy;
        TextView subtitle;
        ImageView sharedIcon;
        ImageView actionIcon;
        TextView time;
        ImageButton threeDots;
        LinearLayout mediaLayout;
        RecyclerView mediaRecycler;

        public ViewHolderBucket(View itemView) {
            super(itemView);
        }
    }

    @Override
    public RecentsAdapter.ViewHolderBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bucket,parent,false);
        ViewHolderBucket holder = new ViewHolderBucket(v);

        holder.headerLayout = (RelativeLayout) v.findViewById(R.id.header_layout);
        holder.headerText = (TextView) v.findViewById(R.id.header_text);
        holder.itemBucketLayout = (RelativeLayout) v.findViewById(R.id.item_bucket_layout);
        holder.itemBucketLayout.setTag(holder);
        holder.imageThumbnail = (ImageView) v.findViewById(R.id.thumbnail_view);
        holder.threeDots = (ImageButton) v.findViewById(R.id.three_dots);
        holder.threeDots.setTag(holder);
        holder.title = (TextView) v.findViewById(R.id.first_line_text);
        holder.actionBy = (TextView) v.findViewById(R.id.second_line_text);
        holder.subtitle = (TextView) v.findViewById(R.id.name_text);
        holder.sharedIcon = (ImageView) v.findViewById(R.id.shared_image);
        holder.actionIcon = (ImageView) v.findViewById(R.id.action_image);
        holder.time = (TextView) v.findViewById(R.id.time_text);
        holder.mediaLayout = (LinearLayout) v.findViewById(R.id.media_bucket_layout);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecentsAdapter.ViewHolderBucket holder, int position) {
        log("onBindViewHolder: "+position);
        RecentsItem item = getItemtAtPosition(position);
        if (item == null) return;

        if (item.getViewType() == RecentsItem.TYPE_HEADER) {
            log("onBindViewHolder: TYPE_HEADER");
            holder.itemBucketLayout.setVisibility(View.GONE);
            holder.headerLayout.setVisibility(View.VISIBLE);
            holder.headerText.setText(item.getDate());
        }
        else if (item.getViewType() == RecentsItem.TYPE_BUCKET) {
            log("onBindViewHolder: TYPE_BUCKET");
            holder.itemBucketLayout.setVisibility(View.VISIBLE);
            holder.itemBucketLayout.setOnClickListener(this);
            holder.headerLayout.setVisibility(View.GONE);

            MegaRecentActionBucket bucket = item.getBucket();
            if (bucket == null || bucket.getNodes() == null || bucket.getNodes().size() == 0) return;

            MegaNodeList nodeList = bucket.getNodes();
            MegaNode node = nodeList.get(0);
            if (node == null) return;

            MegaNode parentNode = megaApi.getNodeByHandle(bucket.getParentHandle());
            if (parentNode == null) return;

            holder.subtitle.setText(parentNode.getName());
            if (parentNode.isShared()) {
                holder.actionBy.setVisibility(View.VISIBLE);
                holder.sharedIcon.setVisibility(View.VISIBLE);
                String userAction;
                String mail = bucket.getUserEmail();
                String user;
                if (mail.equals(megaApi.getMyEmail())) {
                    user = context.getString(R.string.bucket_word_me);
                }
                else {
                    user = ((RecentsFragment) fragment).findUserName(mail);
                }
                if (bucket.isUpdate()) {
                    userAction = context.getString(R.string.update_action_bucket, user);
                }
                else {
                    userAction = context.getString(R.string.create_action_bucket, user);
                }

                holder.actionBy.setText(formatUserAction(userAction));
                if (parentNode.isInShare()) {
                    holder.sharedIcon.setImageResource(R.drawable.ic_folder_incoming_list);
                }
                else if (parentNode.isOutShare()) {
                    holder.sharedIcon.setImageResource(R.drawable.ic_folder_outgoing_list);
                }
            }
            else {
                holder.actionBy.setVisibility(View.GONE);
                holder.sharedIcon.setVisibility(View.GONE);
            }
            holder.time.setText(item.getTime());
            holder.imageThumbnail.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            holder.mediaLayout.setVisibility(View.GONE);

            if (nodeList.size() == 1) {
                holder.threeDots.setVisibility(View.VISIBLE);
                holder.threeDots.setOnClickListener(this);
                holder.title.setText(node.getName());
            }
            else {
                holder.threeDots.setVisibility(View.INVISIBLE);
                holder.threeDots.setOnClickListener(null);

                if (bucket.isMedia()) {
                    holder.mediaLayout.setVisibility(View.VISIBLE);
                    holder.title.setText(getMediaTitle(nodeList));
                    holder.imageThumbnail.setImageResource(R.drawable.media);
                }
                else {
                    holder.title.setText(context.getString(R.string.title_bucket, node.getName(), (nodeList.size()-1)));
                }
            }

            if (bucket.isUpdate()){
                holder.actionIcon.setImageResource(R.drawable.ic_versions_small);
            }
            else {
                holder.actionIcon.setImageResource(R.drawable.ic_recents_up);
            }
        }
    }

    private Spanned formatUserAction (String userAction) {
        try{
            userAction = userAction.replace("[A]", "<font color=\'#7a7a7a\'>");
            userAction = userAction.replace("[/A]", "</font>");
        }
        catch (Exception e){}

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(userAction,Html.FROM_HTML_MODE_LEGACY);
        }
        else {
            return Html.fromHtml(userAction);
        }
    }

    private String getMediaTitle(MegaNodeList nodeList) {
        int numImages = 0;
        int numVideos = 0;
        String mediaTitle = null;

        for (int i=0; i<nodeList.size(); i++) {
            if (MimeTypeList.typeForName(nodeList.get(i).getName()).isImage()) {
                numImages++;
            }
            else {
                numVideos++;
            }
        }

        if (numImages > 0 && numVideos == 0) {
            mediaTitle = context.getString(R.string.title_media_bucket_only_images, numImages);
        }
        else if (numImages == 0 && numVideos > 0) {
            mediaTitle = context.getString(R.string.title_media_bucket_only_videos, numVideos);
        }
        else if (numImages == 1 && numVideos == 1) {
            mediaTitle = context.getString(R.string.title_media_bucket_image_and_video);
        }
        else if (numImages == 1 && numVideos > 1) {
            mediaTitle = context.getString(R.string.title_media_bucket_image_and_videos, numVideos);
        }
        else if (numImages > 1 && numVideos == 1) {
            mediaTitle = context.getString(R.string.title_media_bucket_images_and_video, numImages);
        }
        else {
            mediaTitle = context.getString(R.string.title_media_bucket_images_and_videos, numImages, numVideos);
        }

        return mediaTitle;
    }

    public void setItems (ArrayList<RecentsItem> recentsItems) {
        this.recentsItems = recentsItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (recentsItems == null || recentsItems.isEmpty()) return 0;

        return recentsItems.size();
    }

    @Override
    public void onClick(View v) {

        ViewHolderBucket holder = (ViewHolderBucket) v.getTag();
        if (holder == null) return;

        RecentsItem item = getItemtAtPosition(holder.getAdapterPosition());
        if (item == null) return;

        MegaNode node = getNodeOfItem(item);

        switch (v.getId()) {
            case R.id.three_dots:{
                log("three_dots click");
                if (node != null) {
                    ((ManagerActivityLollipop) context).showNodeOptionsPanel(node);
                }
                break;
            }
            case R.id.item_bucket_layout: {
                log("item_bucket_layout click");
                MegaNodeList nodeList = getMegaNodeListOfItem(item);
                if (nodeList == null) break;
                if (nodeList.size() == 1) {

                    break;
                }
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int pos) {
        if (recentsItems == null || recentsItems.isEmpty() || pos >= recentsItems.size()) return super.getItemViewType(pos);

        return recentsItems.get(pos).getViewType();
    }

    private RecentsItem getItemtAtPosition (int pos) {
        if (recentsItems == null || recentsItems.isEmpty() || pos >= recentsItems.size()) return null;

        return recentsItems.get(pos);
    }

    private MegaRecentActionBucket getBucketOfItem (RecentsItem item) {
        if (item == null) return null;

        return item.getBucket();
    }

    private MegaNodeList getMegaNodeListOfItem (RecentsItem item) {
       MegaRecentActionBucket bucket = getBucketOfItem(item);
        if (bucket == null) return null;

        return bucket.getNodes();
    }

    private MegaNode getNodeOfItem (RecentsItem item) {
        MegaNodeList nodeList = getMegaNodeListOfItem(item);
        if (nodeList == null || nodeList.size() > 1) return null;

        return nodeList.get(0);
    }

    @Override
    public String getSectionTitle(int position) {
        if (recentsItems == null || recentsItems.isEmpty()) return null;

        return recentsItems.get(position).getDate();
    }

    private static void log(String log) {
        Util.log("RecentsAdapter",log);
    }
}
