package com.vijaya.speechtotext;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView mVoiceInputTv, mNameView, mHintView;
    private ImageButton mSpeakBtn;
    TextToSpeech mTts;
    private SharedPreferences preferences;//used for editing
    private SharedPreferences.Editor editor;//used for editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVoiceInputTv = (TextView) findViewById(R.id.voiceInput);
        mNameView = (TextView) findViewById(R.id.nameView);//used to save name info
        mHintView = (TextView) findViewById(R.id.hintView);//used to provide hints
        mSpeakBtn = (ImageButton) findViewById(R.id.btnSpeak);//used for button
        mSpeakBtn.setOnClickListener(new View.OnClickListener() {//when the mic button is pushed
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });
        //beginning tts
        mTts = new TextToSpeech(this, this);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mVoiceInputTv.setText(result.get(0));

                //this will answer the 3 questions the user asks. check the response function for more
                response(result.get(0));
            }
        }
    }

    @Override
    public void onInit(int status) {
        switch (status) {
            case TextToSpeech.SUCCESS: {
                int result = mTts.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    Toast.makeText(this, "English (US) not supported", Toast.LENGTH_SHORT).show();
                else
                    //opens the app with the hello greeting
                    mTts.speak("Hello", TextToSpeech.QUEUE_ADD, null);
                //gives the user a hint on how to begin
                mHintView.setText("Tap on mic to start. Try to say Hello!");
                break;
            }
            //error handling. Should notify user if tts does not work for some reason
            case TextToSpeech.ERROR: {
                Toast.makeText(this, "Text to speech did not function as intended", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    /**This fuction will set the user's name into the nameView. It will also have the app respond
     *
     * @param name
     */
    private void setName(String name) {
        preferences = getSharedPreferences("prefs",0);
        editor = preferences.edit();//allows editing of nameview
        editor.putString("name", name).apply();

        mNameView.setText(String.format("Hello, %s!", name));
        mNameView.setVisibility(View.VISIBLE);//makes nameview box show

        //app responds with greeting and user's name
        mTts.speak(String.format("Hello, %s!", name), TextToSpeech.QUEUE_ADD, null);
        //app will inform user what they may ask
        mHintView.setText("This is a medical assistant. You may ask or state the following: \n \"I am not feeling well. What should I do?\"\n\"What medicines " +
                "should I take?\"\n\"What time is it?\"\n\"Thank you, my Medical Assistant.\"");
    }

    /**This function will handle all user questions. App will respond based off keywords
     *
     * @param input
     */
    private void response(String input) {

        //if the user responds hello back, the program will ask the user for their name
        if (input.equals("hello")|| input.contains("hi")) {
            mTts.speak("What is your name?", TextToSpeech.QUEUE_FLUSH, null);
            mHintView.setText("Tap the mic and say \"My name is <name>\"");
        }
        //If the user gives their name, the program will save the name in the setName function
        else if (input.contains("my name is") || input.contains("my name's") || input.contains("i am")) {
            setName(input.split(" ")[3]);
        }
        //One of the three questions. The user can state that they are not feeling good
        else if (input.contains("not feeling good") || input.contains("well")) {
            mTts.speak("I can understand. Please tell your symptoms in short.", TextToSpeech.QUEUE_FLUSH, null);
        }
        //if the user describes any of these symptoms it will tell them to speak with a professional
        else if (input.contains("headache") || input.contains("fever") || input.contains("breathing") || input.contains("nausea")){
            mTts.speak("You should speak with a medical professional. You may have the flu or corona virus.", TextToSpeech.QUEUE_FLUSH, null);
        }
        //Once the user has described their symptoms, they may ask for medicine recommendations. IT will give a default statement
        else if (input.contains("medicines") || input.contains("medication")) {
            mTts.speak("I think you have a fever. Please take this medicine.", TextToSpeech.QUEUE_FLUSH, null);
        }
        else if (input.contains("thanks") || input.contains("thank you")) {
            mTts.speak("Thank you. Take care.", TextToSpeech.QUEUE_FLUSH, null);
        }
        //the user may ask what time it is. This section will provide 12hr time with AM/PM
        else if (input.contains("time")) {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY) == 0 ? 12 : now.get(Calendar.HOUR_OF_DAY);
            String min = now.get(Calendar.MINUTE) == 0 ? "o'clock" : String.format(Locale.US,"%d", now.get(Calendar.MINUTE));
            String half = "AM";

            if (hour > 11 && hour < 23) {
                hour = hour - 12;
                half = "PM";
            } else if (hour == 0) {
                hour = 12;
                half = "AM";
            }
            mTts.speak(String.format(Locale.US,"It's %d:%s %s", hour, min, half), TextToSpeech.QUEUE_FLUSH, null);
        }
        //if the user provides any unknown response it will give this default answer. It will give the hint telling what to ask
        else{
            mTts.speak("I'm sorry. I do not recognize that response. Please say again.", TextToSpeech.QUEUE_FLUSH, null);
            mHintView.setText("This is a medical assistant. You may ask or state the following: \n \"I am not feeling well. What should I do?\"\n\"What medicines " +
                    "should I take?\"\n\"What time is it?\"\n\"Thank you, my Medical Assistant.\"");
        }
    }
}