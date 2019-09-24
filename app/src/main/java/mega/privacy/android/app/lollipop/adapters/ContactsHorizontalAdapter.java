package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.utils.CacheFolderManager;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.FileUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

import static mega.privacy.android.app.utils.LogUtil.*;

public class ContactsHorizontalAdapter extends RecyclerView.Adapter<ContactsHorizontalAdapter.ContactViewHolder> implements View.OnClickListener {

    private Activity context;

    private RecentChatsFragmentLollipop recentChatsFragment;

    private List<MegaContactGetter.MegaContact> contacts;

    private MegaApiAndroid megaApi;

    public ContactsHorizontalAdapter(Activity context, RecentChatsFragmentLollipop recentChatsFragment, List<MegaContactGetter.MegaContact> data) {
        this.context = context;
        this.contacts = data;
        if (megaApi == null) {
            megaApi = ((MegaApplication) context.getApplication()).getMegaApi();
        }
        this.recentChatsFragment = recentChatsFragment;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_avatar, parent, false);

        ContactViewHolder holder = new ContactViewHolder(v);
        holder.itemLayout = v.findViewById(R.id.chip_layout);
        holder.contactInitialLetter = v.findViewById(R.id.contact_list_initial_letter);
        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidth(Util.px2dp(60, outMetrics));
        holder.avatar = v.findViewById(R.id.add_rounded_avatar);
        holder.avatar.setOnClickListener(this);
        holder.avatar.setTag(holder);
        holder.addIcon = v.findViewById(R.id.add_icon_chip);
        holder.addIcon.setOnClickListener(this);
        holder.addIcon.setTag(holder);
        v.setTag(holder);
        return holder;
    }

    @Override
    public void onClick(View v) {
        ContactViewHolder holder = (ContactViewHolder) v.getTag();
        int currentPosition = holder.getAdapterPosition();
        contacts.remove(currentPosition);
        recentChatsFragment.onContactsCountChange(contacts);
        notifyDataSetChanged();

        String email = holder.contactMail;
        logDebug("sent invite to: " + email);
        //ignore the callback
        megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD);
        Util.showSnackBar(context, Constants.SNACKBAR_TYPE, context.getString(R.string.context_contact_request_sent, email), -1);
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactViewHolder holder, int position) {
        final MegaContactGetter.MegaContact megaContact = getItem(position);
        String email = megaContact.getEmail();
        holder.contactMail = email;
        holder.textViewName.setText(megaContact.getLocalName());
        UserAvatarListener listener = new UserAvatarListener(context, holder);
        setDefaultAvatar(megaContact, holder);
        File avatar = CacheFolderManager.buildAvatarFile(context, email + ".jpg");
        Bitmap bitmap;
        if (FileUtils.isFileAvailable(avatar)) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    if (avatar.delete()) {
                        logDebug("delete avatar successfully.");
                    }
                    megaApi.getUserAvatar(email, avatar.getAbsolutePath(), listener);
                } else {
                    holder.contactInitialLetter.setVisibility(View.GONE);
                    holder.avatar.setImageBitmap(bitmap);
                }
            } else {
                megaApi.getUserAvatar(email, avatar.getAbsolutePath(), listener);
            }
        } else {
            megaApi.getUserAvatar(email, avatar.getAbsolutePath(), listener);
        }
    }

    public void setDefaultAvatar(MegaContactGetter.MegaContact contact, ContactViewHolder holder) {
        String color = megaApi.getUserAvatarColor(contact.getId());
        Bitmap defaultAvatar = Util.createDefaultAvatar(color, getFirstLetter(holder));
        holder.avatar.setImageBitmap(defaultAvatar);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public MegaContactGetter.MegaContact getItem(int position) {
        logDebug("getItem");
        return contacts.get(position);
    }

    public static class ContactViewHolder extends MegaContactsLollipopAdapter.ViewHolderContacts {

        TextView textViewName;

        ImageView addIcon;

        public RoundedImageView avatar;

        RelativeLayout itemLayout;

        ContactViewHolder(View itemView) {
            super(itemView);
        }
    }

    private String getFirstLetter(ContactViewHolder holder) {
        CharSequence name = holder.textViewName.getText();
        if(TextUtils.isEmpty(name)) {
            return "";
        }
        return String.valueOf(name.charAt(0));
    }
}
