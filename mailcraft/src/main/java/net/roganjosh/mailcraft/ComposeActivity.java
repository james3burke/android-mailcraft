package net.roganjosh.mailcraft;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ComposeActivity extends AppCompatActivity {

    private final String TAG = ComposeActivity.class.getSimpleName();

    RecipientEditTextView mChipsInput;

    private final String[] PERMISSIONS = {android.Manifest.permission.READ_CONTACTS};

    private final int READ_CONTACTS_ALLOWED = 234;

    GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mChipsInput = (RecipientEditTextView) findViewById(R.id.retv_recipients);
        mChipsInput.setTokenizer(new Rfc822Tokenizer());
        BaseRecipientAdapter baseRecipientAdapter = new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_EMAIL, this);
        mChipsInput.setAdapter(baseRecipientAdapter);
        init();
    }

    protected void init() {
        prepareContacts();
        SharedPreferences sharedPreferences = getSharedPreferences("APP", MODE_PRIVATE);
        String email = sharedPreferences.getString("credential_name", null);
        if (StringUtils.isNotEmpty(email)) {
            Log.d(TAG, "email " + email);
            // Initialize credentials and service object.
            mCredential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(GmailScopes.GMAIL_COMPOSE))
                    .setBackOff(new ExponentialBackOff());
            mCredential.setSelectedAccountName(email);
            if ((mCredential != null) && (mCredential.getSelectedAccountName() != null)) {
                Log.d(TAG, "Account credential " + mCredential.getSelectedAccountName());

                HttpTransport transport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                final Gmail service = new Gmail.Builder(transport, jsonFactory, mCredential)
                        .setApplicationName(getResources().getString(R.string.app_name))
                        .build();
                Tasks.executeInBackground(this, new BackgroundWork<Boolean>() {
                    @Override
                    public Boolean doInBackground() throws Exception {
                        sendGmailMessage(service, mCredential);
                        return true;
                    }
                }, new Completion<Boolean>() {
                    @Override
                    public void onSuccess(Context context, Boolean result) {
                        Toast.makeText(ComposeActivity.this, "Sent mail", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Context context, Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ComposeActivity.this, "Failed! " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                Log.d(TAG, "No account credential");
            }
        } else {
            Toast.makeText(this, "No previous signin", Toast.LENGTH_LONG);
            Intent intent = new Intent(this, GoogleAccountActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(READ_CONTACTS_ALLOWED)
    private void prepareContacts() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            // do stuff
        } else if (EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(PERMISSIONS))) {
            Toast.makeText(this, "No access to contacts", Toast.LENGTH_SHORT);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_read_contacts), READ_CONTACTS_ALLOWED, PERMISSIONS);
        }
    }

    /*
     * GMAIL
     */

    public void sendGmailMessage(Gmail service, GoogleAccountCredential credential) {
        try {
            MimeMessage mm = createEmail("shops@roganjosh.net", credential.getSelectedAccountName(), "hello", "test");
            sendMessage(service, "me", mm);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to email address of the receiver
     * @param from email address of the sender, the mailbox account
     * @param subject subject of the email
     * @param bodyText body text of the email
     * @return the MimeMessage to be used to send email
     * @throws MessagingException
     */
    public static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param emailContent Email to be sent.
     * @return The sent message
     * @throws MessagingException
     * @throws IOException
     */
    public static Message sendMessage(Gmail service, String userId, MimeMessage emailContent)
            throws MessagingException, IOException, UserRecoverableAuthIOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }

    public void clickSignIn(View v) {
        Intent intent = new Intent(this, GoogleAccountActivity.class);
        startActivity(intent);
    }
}
