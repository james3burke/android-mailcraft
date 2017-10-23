package net.roganjosh.mailcraft;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.pchmn.materialchips.ChipsInput;
import com.pchmn.materialchips.model.Chip;
import com.pchmn.materialchips.model.ChipInterface;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import butterknife.OnEditorAction;

public class ComposeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ChipsInput chipsInput = (ChipsInput) findViewById(R.id.ci_recipients);
        List<Chip> contactList = new ArrayList<>();
        contactList.add(new Chip("Bob Label", "Bob info"));
        contactList.add(new Chip("Fred label", "Fred info"));
        chipsInput.setFilterableList(contactList);
        chipsInput.addChipsListener(new ChipsInput.ChipsListener() {
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
                chipsInput.addChip(name, charSequence.toString());
                return true;
            }
        });
    }

}
