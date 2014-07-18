package com.example.crusader.ejemplo001;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class DemoAdquisicionAudio extends Activity {

    private static final String TAG = DemoAdquisicionAudio.class.getSimpleName();
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    FileOutputStream os = null;

    int bufferSize;
    int frequency =  44100; //8000; // Sampling Frecuency
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    boolean started = false;
    RecordAudio recordTask;

    short threshold = 1000;

    boolean debug = false;

    private Button btnInicio;
    private Button btnParar;
    private Button btnCerrar;
    private Button btnReiniciar;

    private TextView txtRawData;

    public void Iniciar() {
        startAquisition();
    }

    public void Detener() {
        stopAquisition();
    }

    public void Cerrar() {
        finish();
    }

    public void Reiniciar() {
        resetAquisition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_adquisicion_audio);

        btnReiniciar = (Button) findViewById(R.id.btnReiniciar);
        btnReiniciar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Reiniciar();
            }
        });

        btnInicio = (Button) findViewById(R.id.btnInicio);
        btnInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Iniciar();
            }
        });

        btnParar  = (Button) findViewById(R.id.btnParar);
        btnParar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Detener();
            }
        });

        btnCerrar = (Button) findViewById(R.id.btnCerrar);
        btnCerrar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Cerrar();
            }
        });

        txtRawData = (TextView) findViewById(R.id.lblRawData);

    }

    @Override
    protected void onResume(){
        Log.w(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.w(TAG, "onDestroy");
        super.onDestroy();
    }

    public class RecordAudio extends AsyncTask<Void, Double, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            Log.w(TAG, "doInBackground");
            try {
                String filename = getTempFilename();
                try {
                    os = new FileOutputStream(filename);
                }
                catch(FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }

                bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration,
                        audioEncoding);

                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short [] buffer = new short [bufferSize];

                audioRecord.startRecording();

                while(started) {
                    int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                    if(AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                        int foundPeak = searchThreshold(buffer, threshold);
                        if(foundPeak > -1){
                            byte [] byteBuffer = ShortToByte(buffer, bufferReadResult);
                            try {
                                os.write(byteBuffer);
                                //Log.i("", "_____________CHUNK DE BITS");
                                for(int i = 0; i < byteBuffer.length; i++)
                                    System.out.println(String.valueOf(byteBuffer[i]));
                                    //Log.i("Datos Crudos(" + i +")", String.valueOf(byteBuffer[i]));
                                //Log.i("", "_____________FIN DE  CHUNK");
                            }
                            catch (IOException e){
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                            }
                        }
                        else {

                        }
                    }
                }

                audioRecord.stop();

                try {
                    os.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }

                copyWaveFile(getTempFilename(), getFileName());
                deleteTempFile();
            }
            catch(Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
                Toast.makeText(getApplicationContext(), "Recording Failed", Toast.LENGTH_LONG).show();
            }
            return null;
        }

        byte [] ShortToByte(short [] input, int elements) {
            int short_index, byte_index;
            int iterations = elements;
            byte [] buffer = new byte[iterations * 2];

            short_index = byte_index = 0;

            for(; short_index != iterations; ) {
                buffer[byte_index] = (byte) (input[short_index] & 0x00FF);
                buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00)>>8);

                ++short_index; byte_index += 2;
            }

            return buffer;
        }

        int searchThreshold(short [] arr, short thr) {
            int peakIndex;
            int arrLen = arr.length;
            for(peakIndex = 0; peakIndex < arrLen; peakIndex++) {
                if((arr[peakIndex] >= thr) || (arr[peakIndex] <= -thr)) {
                    return peakIndex;
                }
            }
            return -1;
        }

        private String getFileName() {
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath, AUDIO_RECORDER_FOLDER);

            if(!file.exists()) {
                file.mkdirs();
            }

            return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);

        }

        private String getTempFilename() {
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath, AUDIO_RECORDER_FOLDER);

            if(!file.exists()){
                file.mkdir();
            }

            File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

            if(tempFile.exists()) {
                tempFile.delete();
            }

            return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
        }

        private void deleteTempFile() {
            File file = new File(getTempFilename());

            file.delete();
        }

        private void copyWaveFile(String inFilename, String outFilename) {
            FileInputStream in = null;
            FileOutputStream out = null;
            long totalAudioLen = 0;
            long totalDataLen = totalAudioLen + 36;
            long longSampleRate = frequency;
            int channels = 1;
            long byteRate = RECORDER_BPP * frequency * channels/8;

            byte data [] = new byte [bufferSize];

            try {
                in = new FileInputStream(inFilename);
                out = new FileOutputStream(outFilename);

                totalAudioLen = in.getChannel().size();
                totalDataLen = totalAudioLen + 36;

                WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate,
                        channels, byteRate);

                while(in.read(data) != -1) {
                    out.write(data);
                }
                in.close();
                out.close();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        private void WriteWaveFileHeader(
                FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                int channels, long byteRate) throws IOException {
            byte [] header = new byte [44];

            header[0] = 'R';
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte)(totalDataLen & 0xff);
            header[5] = (byte)((totalDataLen >> 8) & 0xff);
            header[6] = (byte)((totalDataLen >> 16) & 0xff);
            header[7] = (byte)((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f';
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16;
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1;
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (longSampleRate & 0xff);
            header[25] = (byte) ((longSampleRate >> 8) & 0xff);
            header[26] = (byte) ((longSampleRate >> 16) & 0xff);
            header[27] = (byte) ((longSampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (channels * 16 / 8);
            header[33] = 0;
            header[34] = RECORDER_BPP;
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            out.write(header, 0, 44);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.demo_adquisicion_audio, menu);
        return true;
    }

    public void resetAquisition(){
        Log.w(TAG, "resetAquisition");
        Toast.makeText(getApplicationContext(), "Reset Adquisition", Toast.LENGTH_SHORT).show();
        stopAquisition();
        startAquisition();
    }

    public void stopAquisition(){
        Log.w(TAG, "stopAquisition");
        Toast.makeText(getApplicationContext(), "Stop Adquisition", Toast.LENGTH_SHORT).show();
        if(started) {
            started = false;
            recordTask.cancel(true);
        }
    }

    public void startAquisition() {
        Log.w(TAG, "startAquisition");
        Toast.makeText(getApplicationContext(), "Start Adquisition", Toast.LENGTH_SHORT).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run () {
                started = true;
                recordTask = new RecordAudio();
                recordTask.execute();
            }
        }, 500);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
