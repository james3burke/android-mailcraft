#Mailcraft

Mailcraft adds a compose activity from which you can send gmail messages. The extra feature this offers over using gmail directly is that you can attach an html section to the email that you send

##Installation

Add jitpack to your project build.gradle

```
allprojects {
    repositories {
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}
```

Once you've added jitpack you can add the dependency to

`compile 'com.github.james3burke:android-mailcraft:0.1.1'`

##Configuration

You need to add two activities to your AndroidManifest

```
<activity
   android:name="net.roganjosh.mailcraft.ComposeActivity"
   android:theme="@style/AppTheme.NoActionBar"
   android:launchMode="singleTop"></activity>
<activity
   android:name="net.roganjosh.mailcraft.GoogleAccountActivity"
   android:label="@string/title_activity_google_account"
   android:theme="@style/AppTheme.NoActionBar"
   android:parentActivityName="net.roganjosh.mailcraft.ComposeActivity"></activity>
```

In order for mailcraft to be able to send gmail messages you will need to configure you application to use the Gmail API. Follow steps 1 and 2 from these instructions

https://developers.google.com/gmail/api/quickstart/android

##Usage

You can start the ComposeActivity to allow the user to compose and send an email. You can also optionally set the subject, recipients and html section.

```
Intent intent = new Intent(this, ComposeActivity.class);
intent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
intent.putExtra(Intent.EXTRA_TEXT, "Text content");
intent.putExtra(Intent.EXTRA_HTML_TEXT, "<h1>HTML content</h1>");
intent.putStringArrayListExtra(Intent.EXTRA_EMAIL, new ArrayList<>(Arrays.asList("test@gmail.com")));
startActivity(intent);
```

On first usage the activity will redirect you to the GoogleSignInActivity where the user will need to authorize your application to connect to gmail, and access the contacts on the device.

If the EXTRA_HTML_TEXT content is left blank then the EXTRA_TEXT field will become the body of the message. If the EXTRA_HTML_TEXT is populated however then it is assumed that the EXTRA_TEXT content is the alt representation to be placed in the mime message alt section. It doesn't seem like gmail uses this anymore, but it used to be important to reduce the risk of looking like spam.
