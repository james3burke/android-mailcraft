package net.roganjosh.mailcraft;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;
import com.pchmn.materialchips.ChipsInput;
import com.pchmn.materialchips.model.Chip;
import com.pchmn.materialchips.model.ChipInterface;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ComposeActivity extends AppCompatActivity {

    private final String TAG = ComposeActivity.class.getSimpleName();

    ChipsInput mChipsInput;

    private final String[] PERMISSIONS = {android.Manifest.permission.READ_CONTACTS};

    private final int READ_CONTACTS_ALLOWED = 234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mChipsInput = (ChipsInput) findViewById(R.id.ci_recipients);
        mChipsInput.addChipsListener(new ChipsInput.ChipsListener() {
            @Override
            public void onChipAdded(ChipInterface chip, int newSize) {
                // chip added
                // newSize is the size of the updated selected chip list
            }

            @Override
            public void onChipRemoved(ChipInterface chip, int newSize) {
                // chip removed
                // newSize is the size of the updated selected chip list
            }

            @Override
            public void onTextChanged(CharSequence text) {
                // text changed
                Log.d("CA","Text is " + text);
            }

            @Override
            public boolean onTextEntered(CharSequence charSequence) {
                String name = charSequence.toString();
                if (name.contains("@")) {
                    String[] parts = name.split("@");
                    name = parts[0];
                }
                mChipsInput.addChip(name, charSequence.toString());
                return true;
            }
        });
        init();
    }

    protected void init() {
        prepareContacts();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(READ_CONTACTS_ALLOWED)
    private void prepareContacts() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            beginLoadContacts();
        } else if (EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(PERMISSIONS))) {
            Toast.makeText(this, "No access to contacts", Toast.LENGTH_SHORT);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_read_contacts), READ_CONTACTS_ALLOWED, PERMISSIONS);
        }
    }

    private void beginLoadContacts() {
        Tasks.executeInBackground(this, new BackgroundWork<List<Chip>>() {
            @Override
            public List<Chip> doInBackground() throws Exception {
                List<Chip> contactList = new ArrayList<>();
                contactList.add(new Chip("Bob Label", "Bob info"));
                contactList.add(new Chip("Fred label", "Fred info"));
                return contactList;
            }
        }, new Completion<List<Chip>>() {
            @Override
            public void onSuccess(Context context, List<Chip> result) {
                if (CollectionUtils.isNotEmpty(result)) {
                    mChipsInput.setFilterableList(result);
                } else {
                    Log.d(TAG, "No contacts loaded");
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Failed to load contacts", e);
            }
        });
    }

}
