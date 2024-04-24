package com.example.app2;


import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.jpardogo.android.googleprogressbar.library.ChromeFloatingCirclesDrawable;
import com.jpardogo.android.googleprogressbar.library.GoogleProgressBar;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int CALL_PERMISSION = 2;
    private static final int SENDSMS = 3;
    private static final int READCONTACTS = 4;

    private static final int LOCATION =5;

    private static final int READCONTACTS1 = 6;


    private TextToSpeech tts;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private static final String fileName = "example.txt";


    @SuppressLint("LongLogTag")
    private void resetSpeechRecognizer() {

        if (speech != null)
            speech.destroy();
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        if (SpeechRecognizer.isRecognitionAvailable(this))
            speech.setRecognitionListener(this);
        else
            finish();
    }

    private void setRecogniserIntent() {

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        recognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE,CALL_PHONE,ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });

        // UI initialisation

        progressBar = findViewById(R.id.progressBar1);
        try{
            progressBar.setIndeterminateDrawable(new ChromeFloatingCirclesDrawable.Builder(this).colors(getResources().getIntArray(R.array.progressLoader)).build());
        }
        catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            Log.d("mBar", "onCreate() returned: " + e);
        }

        // start speech recogniser
        resetSpeechRecognizer();

        // start progress bar
        progressBar.setIndeterminate(true);

        // check for permission
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        setRecogniserIntent();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                speech.startListening(recognizerIntent);
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied!", Toast
                        .LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                call("9108242279");
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied!", Toast
                        .LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == SENDSMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // when permission granted
                sendSms("message");
            } else {
                // permission denied
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == READCONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // when permission granted
                callContact("mom");
            } else {
                // permission denied
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // when permission granted
                location();
            } else {
                // permission denied
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == READCONTACTS1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // when permission granted
                whatsapp("whats app");
            } else {
                // permission denied
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public void onResume() {
        Log.i(LOG_TAG, "resume");
        super.onResume();
        resetSpeechRecognizer();
        speech.startListening(recognizerIntent);
    }

    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "pause");
        super.onPause();
        speech.stopListening();
    }

    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "stop");
        super.onStop();
        if (speech != null) {
            speech.destroy();
        }
    }


    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        progressBar.setIndeterminate(true);
        speech.stopListening();
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> create = new ArrayList<String>(Arrays.asList("create","write","right"));
        ArrayList<String> delete = new ArrayList<>(Arrays.asList("delete","remove"));
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        String text1="";
        String text2="";

        for (String result : matches){
            text += result + "\n";
            try {
            text = matches.get(0);
            String[] arrOfStr = text.split(" ", 2);
            for (String a : arrOfStr){
                text1=arrOfStr[1];
                text2=arrOfStr[0];
            }

            if(create.contains(text2)){
                save(text1);
            }
            else if(text1.equals("date")){
                date();
            }
            else if (text1.equals("time now")){
                time();
            }
            else if (text2.equals("thank")){
                tts.speak("your welcome", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            else if (text2.equals("load")){
                load();
            }
            else if (text2.equals("search")){
                searchNet(text1);
            }
            else if(text2.equals("stop")){
                speech.stopListening();
                speech.destroy();
            }
            else if(text2.equals("dial")) {
                call(text1);
            }
            else if(text2.equals("call")) {
                callContact(text1);
            }
            else if(delete.contains(text2)) {
                delete();
            }
            else if(text2.equals("SMS")) {
                sendSms(text1);
                }
            else if(text1.contains("location")) {
                location();

            } else if (text2.equals("message")) {
                whatsapp(text1);
            }


            }
        catch (Exception e){

            tts.speak("recognition failed ",TextToSpeech.QUEUE_FLUSH,null,null);
        }


        speech.startListening(recognizerIntent);

    }



}
    private void whatsapp(String data) {
        //number that message
        String number = "";
        String message = "";
        String[] arr = data.split(" that ", 2);
        for (String i : arr) {
            number = arr[0];
            message = arr[1];
        }
        if (ContextCompat.checkSelfPermission(this,
                READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, READCONTACTS1);
        } else {
            try {
                Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE},
                        "DISPLAY_NAME = '" + number + "'", null, null);

                if (cursor != null) {
                    cursor.moveToFirst();
                    number = cursor.getString(0);
                }
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + message));
                startActivity(i);

            } catch (Exception e) {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void location(){

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{ACCESS_FINE_LOCATION},LOCATION);
        }
        else{

            fusedLocationProviderClient = getFusedLocationProviderClient(this);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override

                public void onSuccess(Location location) {
                    if(location != null){
                        Geocoder geocoder = new Geocoder(MainActivity.this,Locale.getDefault());
                        try {
                            List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            tts.speak("You are currently close to " + address.get(0).getAddressLine(0), TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }


    private void sendSms(String data) {
        //number that message
        String number = "";
        String message = "";
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SENDSMS);
        } else {
            String[] arr = data.split(" that ", 2);
            for (String i : arr) {
                number = arr[0];
                message = arr[1];
            }
            if (ContextCompat.checkSelfPermission(this,
                    READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, READCONTACTS);
            }
            else {
                try {
                    Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE},
                            "DISPLAY_NAME = '" +number + "'", null, null);

                    if(cursor!=null){
                        cursor.moveToFirst();
                        number = cursor.getString(0);
                    }
                    SmsManager mySmsManager = SmsManager.getDefault();
                    mySmsManager.sendTextMessage(number,null,message,null,null);
                    tts.speak("Message sent to"+data,TextToSpeech.QUEUE_FLUSH,null,null);
                }
                catch (Exception e) {
                    Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void call(String data){
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION);
            } else {
                // passing intent
                String dial = "tel:" + data;

                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }


        }
        catch (Exception e){
            Toast.makeText(this,"error",Toast.LENGTH_SHORT).show();
        }
    }
    private void callContact(String data) {
        if (ContextCompat.checkSelfPermission(this,
                READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, READCONTACTS);
        }
        else {
            try {
                String number= null ;
                Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE},
                        "DISPLAY_NAME = '" +data + "'", null, null);
                if(cursor!=null){
                        cursor.moveToFirst();
                        number = cursor.getString(0);
                }
                tts.speak("calling "+data,TextToSpeech.QUEUE_FLUSH,null,null);
                for (int i = 0; i<1; i++) {
                    //Pause for 2 seconds
                    Thread.sleep(1000);
                }
                call(number);

            }
            catch (Exception e) {
                    Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                }
            }
        }
    private void time() throws InterruptedException {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tts.speak("time is "+ currentTime,TextToSpeech.QUEUE_FLUSH,null,null);
        for (int i = 0; i<1; i++) {
            //Pause for 5 seconds
            Thread.sleep(5000);
        }
    }

    private void date() throws InterruptedException {
        Calendar calender = Calendar.getInstance();
        String formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(calender.getTime());
        String splitDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            splitDate = Arrays.toString(Arrays.stream(formattedDate.split(",")).toArray());
        }
        tts.speak("The date is "+ splitDate,TextToSpeech.QUEUE_FLUSH,null,null);
        for (int i = 0; i<1; i++) {
            //Pause for 5 seconds
            Thread.sleep(5000);
        }
    }
    public void save(String text1){
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(fileName, Context.MODE_APPEND);
            fos.write(text1.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            Toast.makeText(this,"Saved to "+getFilesDir()+"/"+fileName,Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fos!=null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void delete() {
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(" ".getBytes(StandardCharsets.UTF_8));
            fos.flush();
            tts.speak("file is empty",TextToSpeech.QUEUE_FLUSH,null,null);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fos!=null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void searchNet(String text1){
        try {
            Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
            i.putExtra(SearchManager.QUERY,text1);
            startActivity(i);
            tts.speak("your results for "+text1,TextToSpeech.QUEUE_FLUSH,null,null);
        }
        catch (ActivityNotFoundException e){
            e.printStackTrace();
            searchNetCompact(text1);
        }
    }
    public void searchNetCompact(String text1){
        try {
            Uri uri = Uri.parse("http://www.google.com/#q="+text1);
            Intent in = new Intent(Intent.ACTION_VIEW,uri);
            startActivity(in);

        }
        catch (ActivityNotFoundException e){
            e.printStackTrace();
            Toast.makeText(this,"ERROR",Toast.LENGTH_SHORT).show();
        }
    }

    public void load(){
        FileInputStream fis = null;
        try {
            fis = openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text ;
            while((text = br.readLine())!= null){
                sb.append(text).append(" ");
            }

            tts.speak(sb,TextToSpeech.QUEUE_FLUSH,null,null);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = null;
        try {
            errorMessage = getErrorText(errorCode);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.i(LOG_TAG, "FAILED " + errorMessage);


        // rest voice recogniser
        resetSpeechRecognizer();
        speech.startListening(recognizerIntent);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        progressBar.setProgress((int) rmsdB);
    }

    public String getErrorText(int errorCode) throws InterruptedException {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                Toast.makeText(this,"Client side ERROR",Toast.LENGTH_SHORT).show();
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                tts.speak("make sure u are connected to a network",TextToSpeech.QUEUE_FLUSH,null,null);
                for (int i = 0; i<1; i++) {
                    //Pause for 4 seconds
                    Thread.sleep(8000);
                }
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

}