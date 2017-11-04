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

    private final String testMessage = "<html xmlns=\"http://www .w3.org/1999/xhtml\">\n" +
            "<body bgcolor=\"fbfbfb\" style=\"font-size:12px\" class=\"linkmail\" id=\"android_9024d3ec-5bf9-4241-9a5a-a5dc6933c0ab\">\n" +
            "\n" +
            "    <p style=\"font-size: 1.6em;\" class=\"message-item\">Sent by LinkMail</p>\n" +
            "\n" +
            "<center style=\"background-color: #fbfbfb;width: 100%;min-width:500px;-webkit-text-size-adjust: 100%;-ms-text-size-adjust: 100%; padding-top: 10px; padding-bottom: 10px;border-top: dashed 2px;\">\n" +
            "    <br/>\n" +
            "\n" +
            "    <table style=\"border-collapse:collapse;border-spacing: 0;Margin-left:auto;Margin-right: auto;width:500px;padding:0;border:0\">\n" +
            "        <tbody>\n" +
            "        <!-- image container -->\n" +
            "        <tr>\n" +
            "            <td style=\"background-color:#eeeeee;padding:0\" align=\"center\">\n" +
            "                <img style=\"max-width:500px;background-color:black;display:block\" src=\"http://www.barchick.com/wp-content/uploads/2016/09/Corazon-1-1.jpg\" class=\"page-img\"/>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "\n" +
            "        <!-- title container -->\n" +
            "        <tr>\n" +
            "            <td style=\"background-color:#306850;color:#ffffff;padding:0;padding-left:8px;padding-right:8px\">\n" +
            "                <H1 style=\"margin:0;padding:0;font-weight: 700;letter-spacing: - 0.03em;-webkit-font-smoothing: antialiased;font-size: 2em ;font-family: sans-serif;\" class=\"title\">Corazon Taqueria</H1>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "\n" +
            "        <!-- snippet container -->\n" +
            "        <tr>\n" +
            "            <td style=\"background-color:#e0e8e0;color:#212121;padding:16px;padding-bottom:0px\">\n" +
            "                <p style=\"margin:0;-moz-osx-font-smoothing: grayscale;font-family: sans-serif; -webkit-font-smoothing: antialiased;font-size: 1.6em;font-weight:500;line-height: 24px\" class=\"description\">\n" +
            "<i>BarChick loves a Mexican, so she&#39;ll be first in line when this new taqueria opens its doors on Poland Street this November. There&#39;ll be ten signature tacos on the menu - from 12-hour slow roast lamb rib to House made green chorizo. Yum.</i></p>\n" +
            "            </td>\n" +
            "        <tr>\n" +
            "\n" +
            "        <!-- read more container -->\n" +
            "        <tr>\n" +
            "            <td style=\"background-color:#e0e8e0;color:#212121;padding:16px;\" align=\"right\">\n" +
            "                <p style=\"margin:0;-moz-osx-font-smoothing: grayscale;font-family: sans-serif;-webkit-font-smoothing: antialiased;line-height: 16px;\">\n" +
            "                    <a style=\"color:#212121;font-size:16px;\" href=\"http://www.barchick.com/find-a-bar/london/corazon-taqueria\" class=\"page-url\">Read more on barchick.com</a>\n" +
            "                    <!-- page icon -->\n" +
            "                </p>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "\n" +
            "        <!-- Dates container -->\n" +
            "\n" +
            "        <!-- map container -->\n" +
            "\n" +
            "        <!-- address container -->\n" +
            "\n" +
            "        </tbody>\n" +
            "    </table>\n" +
            "</center>\n" +
            "<br/>\n" +
            "<div class=\"text-footer\" style=\"color:#646464;font-size:12px;font-family:sans-serif;line-height:20px;text-align:center;\">This email was created using <a href=\"http://www.linkmailapp.net\" style=\"color:#646464;text-decoration:underline;font-weight:bold;\" class=\"link-footer\"><span class=\"link-footer\" style=\"color:#646464;text-decoration:underline;font-weight:bold;\">LinkMail</span></a>\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";

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

        init(getIntent());
    }

    protected void init(Intent request) {
        prepareContacts();
        SharedPreferences sharedPreferences = getSharedPreferences("APP", MODE_PRIVATE);
        String email = sharedPreferences.getString("credential_name", null);
        if (StringUtils.isNotEmpty(email)) {
            Log.d(TAG, "email " + email);
            mCredential = getGoogleAccountCredential(email);
            if ((mCredential != null) && (mCredential.getSelectedAccountName() != null)) {
                //testEmail(mCredential);
            } else {
                Log.d(TAG, "No account credential");
            }
            String rqText = "Hello world";
            String rqHtml = testMessage; //"<html><body><p>Hello world</p></body></html>";
            if ((request != null) && (Intent.ACTION_SENDTO == request.getAction())) {
                rqText = request.getStringExtra(Intent.EXTRA_TEXT);
                String rqSubject = request.getStringExtra(Intent.EXTRA_SUBJECT);
                if (StringUtils.isNotEmpty(rqSubject)) {
                    mSubject.setText(rqText);
                }
                List<String> rqRecipients = request.getStringArrayListExtra(Intent.EXTRA_EMAIL);
                Log.d(TAG, "Recipients list: " + rqRecipients);
                // TODO populate recipients as chips
                rqHtml = request.getStringExtra(Intent.EXTRA_HTML_TEXT);
            }
            if (StringUtils.isNotEmpty(rqHtml)) {
                mHtmlContent = rqHtml;
                mAltText = rqText;
                mWebCard.loadData(rqHtml, "text/html", "UTF-8");
            } else {
                mWebCard.setVisibility(View.GONE);
                if (StringUtils.isNotEmpty(rqText)) {
                    mMessage.setText(rqText);
                }
            }
        } else {
            Toast.makeText(this, "No previous signin", Toast.LENGTH_LONG);
            doActionManageAccount();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Menu clicked: " + item);
        if (R.id.action_manage_account == item.getItemId()) {
            doActionManageAccount();
            return true;
        } else if ((R.id.action_send_email == item.getItemId()) && (!mSendingInProgress)) {
            doActionSendEmail();
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
                Toast.makeText(this, "Failed to build email", Toast.LENGTH_LONG);
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
            Toast.makeText(this, "No access to contacts", Toast.LENGTH_SHORT);
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
                Toast.makeText(ComposeActivity.this, "Message sent", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override
            public void onError(Context context, Exception e) {
                Toast.makeText(ComposeActivity.this, "Failed to send message", Toast.LENGTH_LONG).show();
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
        email.setText(bodyText);
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
            Toast.makeText(this, "Your account is not able to send email", Toast.LENGTH_LONG);
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
        DrawableRecipientChip[] chips = mChipsInput.getRecipients();
        if ((chips != null) && (chips.length > 0)) {
            List<InternetAddress> result = new ArrayList<>();
            for (DrawableRecipientChip chip : chips) {
                try {
                    InternetAddress address = new InternetAddress(chip.getEntry().getDestination(), chip.getEntry().getDisplayName());
                    result.add(address);
                } catch (UnsupportedEncodingException e) {
                    // TODO string
                    mChipsInput.setError("Invalid address");
                }
            }
            if (CollectionUtils.isNotEmpty(result)) {
                return  result;
            }
        } else {
            // TODO string
            mChipsInput.setError("No recipients");
        }
        return null;
    }
}
