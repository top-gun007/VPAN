package com.nit.myapplication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import StepCounter.StepDetector;
import StepCounter.StepListener;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, SensorEventListener, StepListener{
    private ListView mListView;
    private FloatingActionButton mButtonSend;
    private EditText mEditTextMessage;
    private ImageView mImageView;
    private ChatMessageAdapter mAdapter;
    private TextToSpeech tts;
    //CAmera and File
    final static private int NEW_PICTURE = 1;
    private String mCameraFileName;
    //MultiPart Upload
    BinaryUploadRequest binaryUploadRequest;
    //sppech code
    private final int REQ_CODE_SPEECH_INPUT = 100;
    pathfinder nextNode;
    int flag=0;
    //private float currentDegree = 0f;
    private float final_bearing=138f;
    private SensorManager sensorManager;
    private SensorManager sensorManager1;
    private float degree=0f;
    private int numSteps=0;
    private StepDetector simpleStepDetector;
    private Sensor accel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tts = new TextToSpeech(this, this);
        mListView = (ListView) findViewById(R.id.listView);
        mButtonSend = (FloatingActionButton) findViewById(R.id.btn_send);
        mEditTextMessage = (EditText) findViewById(R.id.et_message);
        mImageView = (ImageView) findViewById(R.id.iv_image);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
        mAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());
        mListView.setAdapter(mAdapter);

        //// TODO: 13/12/17 compass and step-counter
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager1 = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        //but resume pai to recursion nahi ho raha tha?

//code for sending the message
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                sendMessage(message);
                mEditTextMessage.setText("");
                mListView.setSelection(mAdapter.getCount() - 1);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
        //fine? // TODO: 13/12/17 now step count sensor remaining! 
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void sendMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true, false);
        mAdapter.add(chatMessage);
        //respond as Helloworld
        float acw=0f,cw=0f;
        if(flag==1){
            Constants.final_placeName=message;
            nextNode=new pathfinder(Integer.parseInt(Constants.intital_placeName),Integer.parseInt(Constants.final_placeName));
            int nextCheckpoint=nextNode.findNextNode();
            if(Integer.parseInt(Constants.intital_placeName)<Integer.parseInt(Constants.final_placeName)){
                final_bearing=317f;
                //ye kafi hai i think no need of another if
            }else if(Integer.parseInt(Constants.intital_placeName)>Integer.parseInt(Constants.final_placeName)){
                final_bearing=138f;
            }
            float bearingValue=final_bearing;


            //angle from destination to source (not the opposite)
            // if clockwise angle is obtained, the user has to turn anticlockwise to make the deviation zero
            if(degree <= bearingValue)
            {
                acw = bearingValue - degree;
                cw = (360f - bearingValue) + degree;
            }
            else
            {
                acw = bearingValue + (360 - degree);
                cw = degree - bearingValue;
            }
            if(cw < acw)
            {
//                compassAngle.setText("Heading: " + Float.toString(degree) + " degrees, move " + cw + "deg anti - clockwise");
                mimicOtherMessage("Rotate anti-clocwise by"+(cw)+
                        "Move to checkpoint "+nextCheckpoint+ "for 5 meters :" );
            }
            else
            {
//                compassAngle.setText("Heading: " + Float.toString(degree) + " degrees, move " + acw + "deg clockwise");
                mimicOtherMessage("Rotate clockwise by "+(acw)+
                        "Move to checkpoint"+nextCheckpoint+ "for 5 meters :" );
            }
            sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

            flag=0;
        }
        if(message.equalsIgnoreCase("hi") || message.contains("hi") || message.equalsIgnoreCase("hello"))
            mimicOtherMessage("Hi User");
        else if(message.contains("help")||message.contains("Help"))
            mimicCameraMessage("OK firing up Camera");
        else if(message.equalsIgnoreCase("a"))
            mimicOtherMessage("Ang: "+degree+" final:"+ final_bearing+" total:"+(cw)+" "+acw);
        else
            mimicOtherMessage("Sorry! I didn't catch that.");

    }

    private void mimicOtherMessage(String message) {
        //t1.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        ChatMessage chatMessage = new ChatMessage(message, false, false);
        mAdapter.add(chatMessage);
        speakOut(message);
    }

    private void mimicCameraMessage(String message){
        ChatMessage chatMessage = new ChatMessage(message, false, false);
        mAdapter.add(chatMessage);
        speakOut(message);
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent();
        // Picture from camera
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        // This is not the right way to do this, but for some reason, having
        // it store it in
        // MediaStore.Images.Media.EXTERNAL_CONTENT_URI isn't working right.

        Date date = new Date();
        DateFormat df = new SimpleDateFormat("-mm-ss");

        String newPicFile = "Bild"+ df.format(date) + ".jpg";
        String outPath = "/sdcard/" + newPicFile;
        File outFile = new File(outPath);
        Constants.prev_file_name=outPath;

        mCameraFileName = outFile.toString();
        Uri outuri = Uri.fromFile(outFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
        startActivityForResult(intent, NEW_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)    {
        if (resultCode == RESULT_OK && null != data && requestCode==REQ_CODE_SPEECH_INPUT) {

            ArrayList<String> result = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //txtSpeechInput.setText(result.get(0));
            //Toast.makeText(this, "Hi "+result.get(0), Toast.LENGTH_SHORT).show();
            sendMessage(result.get(0));
        }
        if (requestCode == NEW_PICTURE){
            // return from file upload
            if (resultCode == Activity.RESULT_OK){
                Uri uri = null;
                if (data != null){
                    uri = data.getData();
                }if (uri == null && mCameraFileName != null){
                    uri = Uri.fromFile(new File(mCameraFileName));
                }
                File file = new File(mCameraFileName);
                if (!file.exists()) {
                    file.mkdir();
                }
            }
            try {
                String uploadId = UUID.randomUUID().toString();
                binaryUploadRequest=new BinaryUploadRequest(this,uploadId,Constants.msgSendURL);
                binaryUploadRequest.setFileToUpload(Constants.prev_file_name);
                binaryUploadRequest.addHeader("Prediction-Key",Constants.Prediction_Key);
                binaryUploadRequest.addHeader("Content-Type",Constants.Content_Type);
                binaryUploadRequest.setNotificationConfig(new UploadNotificationConfig());
                binaryUploadRequest.setMaxRetries(2);
                binaryUploadRequest.setDelegate(new UploadStatusDelegate() {
                    @Override
                    public void onProgress(UploadInfo uploadInfo) {
                    }
                    @Override
                    public void onError(UploadInfo uploadInfo, Exception exception) {
                    }
                    @Override
                    public void onCompleted(UploadInfo uploadInfo, ServerResponse serverResponse) {
                        // Toast.makeText(getContext(), ""+serverResponse.getBodyAsString().toString(), Toast.LENGTH_SHORT).show();
                        Log.d("ashu",serverResponse.getBodyAsString().toString());
                        Log.d("ashu",serverResponse.toString());
                        try {
                            JSONObject jsonObject=new JSONObject(serverResponse.getBodyAsString());
                            JSONArray jsonArray= jsonObject.getJSONArray("Predictions");
                            for(int xi=0;xi<jsonArray.length();xi++){
                                JSONObject temp=jsonArray.getJSONObject(xi);
                                if(xi==0){
                                    String tempString =temp.getString("Tag");
                                    Toast.makeText(MainActivity.this, ""+tempString, Toast.LENGTH_SHORT).show();
                                    int length=tempString.length();
                                    Constants.intital_placeName= ""+tempString.charAt(length-1);
                                    mimicOtherMessage("Where do you want to go");
                                    flag=1;
                                    //now compass data is remaining

                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onCancelled(UploadInfo uploadInfo) {

                    }
                });
                binaryUploadRequest.startUpload();
            } catch (Exception exc) {
                Toast.makeText(this, exc.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("ashu",Constants.prev_file_name+"error:"+exc.getMessage());
            }

        }
    }

    private void sendMessage() {
        ChatMessage chatMessage = new ChatMessage(null, true, true);
        mAdapter.add(chatMessage);

        mimicOtherMessage();
    }

    private void mimicOtherMessage() {
        ChatMessage chatMessage = new ChatMessage(null, false, true);
        mAdapter.add(chatMessage);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                //btnSpeak.setEnabled(true);
                speakOut("Hi User");
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private void speakOut(String text) {

        //String text = txtText.getText().toString();

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
        degree = Math.round(event.values[0]);
        //koi gal nahi lol mujhe bhi nahi idea koi
        Log.e("anglevalue", Float.toString(degree));
        // create a rotation animation (reverse turn degree degrees)
        //yaha...
       // currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        Toast.makeText(this, "Step taken"+numSteps, Toast.LENGTH_SHORT).show();
        if(numSteps>=9){
            sensorManager.unregisterListener(MainActivity.this);
            speakOut("Ok user you have reached your next checkpoint type help again to continue navigation");
            mimicOtherMessage("Ok user you have reached your next checkpoint type help again to continue navigation");
            numSteps=0;
        }
    }
}
