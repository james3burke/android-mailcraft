package net.roganjosh.mailcraft;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ComposeActivity extends AppCompatActivity {

    private static final String MESSAGE_TEXT = "MessageText";
    private final String TAG = ComposeActivity.class.getSimpleName();

    private RecipientEditTextView mChipsInput;
    private EditText mSubject;
    private EditText mMessage;
    private WebView mWebCard;
    private String mAltText;
    private String mHtmlContent;
    private boolean mSendingInProgress = false;

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
        mChipsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mChipsInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //
            }
        });
        mSubject = (EditText)findViewById(R.id.et_subject);
        mMessage = (EditText)findViewById(R.id.et_message);
        mWebCard = (WebView)findViewById(R.id.wv_card);
        /*
        mWebCard.getSettings().setBuiltInZoomControls(true);
        mWebCard.getSettings().setSupportZoom(true);
        mWebCard.getSettings().setUseWideViewPort(true);
        mWebCard.getSettings().setLoadWithOverviewMode(false);
        */

        init(getIntent(), savedInstanceState);
    }

    protected void init(Intent request, Bundle savedInstanceState) {
        prepareContacts();
        SharedPreferences sharedPreferences = getSharedPreferences("APP", MODE_PRIVATE);
        String email = sharedPreferences.getString("credential_name", null);
        if (StringUtils.isNotEmpty(email)) {
            mCredential = getGoogleAccountCredential(email);
            if ((mCredential != null) && (mCredential.getSelectedAccountName() != null)) {
                Log.d(TAG, "Signed in as " + mCredential.getSelectedAccountName());
            } else {
                Log.d(TAG, "No account credential");
            }
            String rqText = null;
            String rqHtml = null;
            String rqSubject = null;
            List<String> rqRecipients = null;
            String rqMessage = null;
            if ((request != null) && (Intent.ACTION_SEND == request.getAction())) {
                Log.d(TAG, "Receiving send intent");
                rqText = request.getStringExtra(Intent.EXTRA_TEXT);
                rqSubject = request.getStringExtra(Intent.EXTRA_SUBJECT);
                rqRecipients = request.getStringArrayListExtra(Intent.EXTRA_EMAIL);
                rqHtml = request.getStringExtra(Intent.EXTRA_HTML_TEXT);
                if (StringUtils.isEmpty(rqHtml)) {
                    rqMessage = rqText;
                    rqText = null;
                } else {
                    rqMessage = null;
                }
            } else if (savedInstanceState != null) {
                Log.d(TAG, "Restoring state");
                rqText = savedInstanceState.getString(Intent.EXTRA_TEXT);
                rqSubject = savedInstanceState.getString(Intent.EXTRA_SUBJECT);
                rqRecipients = savedInstanceState.getStringArrayList(Intent.EXTRA_EMAIL);
                rqHtml = savedInstanceState.getString(Intent.EXTRA_HTML_TEXT);
                rqMessage = savedInstanceState.getString(MESSAGE_TEXT);
            }
            if (CollectionUtils.isNotEmpty(rqRecipients)) {
                Log.d(TAG, "Recipients list: " + rqRecipients);
                for (String rqRecipient : rqRecipients) {
                    try {
                        InternetAddress ia = new InternetAddress(rqRecipient);
                        if (ia != null) {
                            mChipsInput.submitItem(ia.getPersonal(), ia.getAddress());
                        }
                    } catch (AddressException e) {
                        Log.e(TAG, "Failed to handle recipient: " + rqRecipient);
                    }

                }
            }
            if (StringUtils.isNotEmpty(rqSubject)) {
                mSubject.setText(rqSubject);
            }
            if (StringUtils.isNotEmpty(rqHtml)) {
                mHtmlContent = rqHtml;
                mAltText = rqText;
                mWebCard.loadData(rqHtml, "text/html", "UTF-8");
            } else {
                mWebCard.setVisibility(View.GONE);
            }
            if (rqMessage != null) {
                mMessage.setText(rqMessage);
            }
        } else {
            doActionManageAccount();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (StringUtils.isNotEmpty(mAltText)) {
            outState.putString(Intent.EXTRA_TEXT, mAltText);
        } else {
            outState.remove(Intent.EXTRA_TEXT);
        }
        if (StringUtils.isNotEmpty(mHtmlContent)) {
            Log.d(TAG, "Saving instance state");
            outState.putString(Intent.EXTRA_HTML_TEXT, mHtmlContent);
        } else {
            Log.d(TAG, "Clearing instance state");
            outState.remove(Intent.EXTRA_HTML_TEXT);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_manage_account == item.getItemId()) {
            doActionManageAccount();
            return true;
        } else if ((R.id.action_send_email == item.getItemId()) && (!mSendingInProgress)) {
            doActionSendEmail();
            return true;
        } else if (R.id.action_discard == item.getItemId()) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void doActionManageAccount() {
        Intent intent = new Intent(this, GoogleAccountActivity.class);
        startActivity(intent);
    }

    public void doActionSendEmail() {
        mSendingInProgress = true;
        String sender = mCredential.getSelectedAccountName();
        MailModel mailModel = validateAndBuildMailModel(sender);
        if (mailModel != null) {
            try {
                MimeMessage email = createEmail(mailModel);
                if (email != null) {
                    sendGmail(mCredential, email);
                    return;
                }
            } catch (MessagingException e) {
                Log.e(TAG, "Error creating email", e);
                String errorMsg = e.getMessage();
                if (StringUtils.isEmpty(errorMsg)) {
                    errorMsg = getString(R.string.error_email_building);
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        }
        mSendingInProgress = false;
    }

    private GoogleAccountCredential getGoogleAccountCredential(String accountName) {
        return GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(GmailScopes.GMAIL_COMPOSE))
                .setBackOff(new ExponentialBackOff()).setSelectedAccountName(accountName);
    }

    private Gmail createGmailService(GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new Gmail.Builder(transport, jsonFactory, mCredential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(READ_CONTACTS_ALLOWED)
    private void prepareContacts() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            BaseRecipientAdapter baseRecipientAdapter = new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_EMAIL, this);
            mChipsInput.setAdapter(baseRecipientAdapter);
        } else if (EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(PERMISSIONS))) {
            Toast.makeText(this, getString(R.string.permission_denied_contacts), Toast.LENGTH_SHORT).show();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_read_contacts), READ_CONTACTS_ALLOWED, PERMISSIONS);
        }
    }

    /*
     * GMAIL
     */

    public void sendGmail(GoogleAccountCredential credential, MimeMessage message) {
        asyncSendGmail(createGmailService(credential), credential.getSelectedAccountName(), message);
    }

    public void asyncSendGmail(final Gmail service, final String accountName, final MimeMessage message) {
        Tasks.executeInBackground(this, new BackgroundWork<Message>() {
            @Override
            public Message doInBackground() throws Exception {
                return sendMessage(service, accountName, message);
            }
        }, new Completion<Message>() {
            @Override
            public void onSuccess(Context context, Message result) {
                Toast.makeText(ComposeActivity.this, getString(R.string.success_sent), Toast.LENGTH_LONG).show();
                finish();
            }
            @Override
            public void onError(Context context, Exception e) {
                Toast.makeText(ComposeActivity.this, getString(R.string.error_failed_to_send), Toast.LENGTH_LONG).show();
                mSendingInProgress = false;
            }
        });
    }

    public static MimeMessage createEmail(MailModel model) throws MessagingException {
        if (StringUtils.isNotEmpty(model.getMessage())) {
            if (StringUtils.isNotEmpty(model.getTextContent())) {
                model.setTextContent(model.getMessage() + "\n" + model.getTextContent());
            } else {
                model.setTextContent(model.getMessage());
            }
            if (StringUtils.isNotEmpty(model.getHtmlContent())) {
                model.setHtmlContent(model.getHtmlContent().replace("<body>", "<body><p>" + StringEscapeUtils.escapeHtml4(model.getMessage()) + "</p>"));
            }
        }
        if (StringUtils.isNotEmpty(model.getHtmlContent())) {
            return createMultipartEmail(model.getRecipients(), model.getSender(), model.getSubject(), model.getTextContent(), model.getHtmlContent());
        } else {
            return createSimpleEmail(model.getRecipients(), model.getSender(), model.getSubject(), model.getTextContent());
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
    public static MimeMessage createSimpleEmail(List<InternetAddress> to, InternetAddress from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email = addHeaderInfo(email, from, to, subject);
        if (StringUtils.isNotEmpty(bodyText)) {
            email.setText(bodyText);
        } else {
            email.setText("");
        }
        return email;
    }

    public static MimeMessage createMultipartEmail(List<InternetAddress> to, InternetAddress from, String subject, String bodyText, String bodyContent) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email = addHeaderInfo(email, from, to, subject);
        email.setContent(buildMimeContent(bodyText, bodyContent));

        return email;
    }

    public static MimeMessage addHeaderInfo(MimeMessage email, InternetAddress from, List<InternetAddress> recipients, String subject) throws MessagingException {

        email.setSubject(subject, "UTF-8");

        email.setFrom(from);
        for(InternetAddress recipient : recipients) {
            if (StringUtils.isNotBlank(recipient.getAddress())) {
                try {
                    recipient.validate();
                    email.addRecipient(javax.mail.Message.RecipientType.TO, recipient);
                } catch (AddressException ae) {
                    throw new MessagingException("Invalid email address \"" + recipient.getAddress() + "\" for contact " + recipient.getPersonal());
                }
            } else {
                throw new MessagingException("No email address selected for contact " + recipient.getPersonal());
            }
        }

        return email;
    }

    public static MimeMultipart buildMimeContent(String textEmailContent, String htmlEmailContent) throws MessagingException {
        MimeMultipart cover = new MimeMultipart("alternative");

        BodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlEmailContent, "text/html; charset=utf-8");

        BodyPart textPart = null;
        if (StringUtils.isNotEmpty(textEmailContent)) {
            textPart = new MimeBodyPart();
            textPart.setContent(textEmailContent, "text/plain; charset=utf-8");
        }

        if (textPart != null) {
            cover.addBodyPart(textPart);
        }
        cover.addBodyPart(htmlPart);

        // put the content in a wrapper
        MimeBodyPart altWrapper = new MimeBodyPart();
        altWrapper.setContent(cover);

        // put the wrapper in the main container (related)
        MimeMultipart content = new MimeMultipart("related");
        content.addBodyPart(altWrapper);

        return content;
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
        return service.users().messages().send(userId, message).execute();
    }

    public MailModel validateAndBuildMailModel(String sender) {
        MailModel mailModel = new MailModel();
        try {
            mailModel.setSender(new InternetAddress(sender));
        } catch (AddressException e) {
            Toast.makeText(this, getString(R.string.error_unable_to_send), Toast.LENGTH_LONG).show();
            return null;
        }
        List<InternetAddress> recipients = validateAndBuildRecipients();
        if (CollectionUtils.isNotEmpty(recipients)) {
            mailModel.setRecipients(recipients);
        } else {
            return null;
        }
        String subject = mSubject.getText().toString();
        if (StringUtils.isNotEmpty(subject)) {
            mailModel.setSubject(subject);
        }
        String message = mMessage.getText().toString();
        if (StringUtils.isNotEmpty(message)) {
            mailModel.setMessage(message);
        }
        if (StringUtils.isNotEmpty(mHtmlContent)) {
            mailModel.setHtmlContent(mHtmlContent);
        }
        if (StringUtils.isNotEmpty(mAltText)) {
            mailModel.setTextContent(mAltText);
        }
        return mailModel;
    }

    private List<InternetAddress> validateAndBuildRecipients() {
        return validateAndBuildRecipients(true);
    }

    private List<InternetAddress> validateAndBuildRecipients(boolean setErrors) {
        DrawableRecipientChip[] chips = mChipsInput.getRecipients();
        if ((chips != null) && (chips.length > 0)) {
            List<InternetAddress> result = new ArrayList<>();
            for (DrawableRecipientChip chip : chips) {
                try {
                    InternetAddress address = new InternetAddress(chip.getEntry().getDestination(), chip.getEntry().getDisplayName());
                    result.add(address);
                } catch (UnsupportedEncodingException e) {
                    if (setErrors) {
                        mChipsInput.setError(getString(R.string.validation_invalid_email));
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(result)) {
                return  result;
            }
        } else {
            if (setErrors) {
                mChipsInput.setError(getString(R.string.validation_no_recipients));
            }
        }
        return null;
    }
}
