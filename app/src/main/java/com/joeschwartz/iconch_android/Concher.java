package com.joeschwartz.iconch_android;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Concher class - implements iconch's audio features.
 */
public class Concher implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener {


    private static final int         MAX_AMPLITUDE   = 32767;
    private static final double      BAD_THRESHOLD   = -50;
    private static final double      GOOD_THRESHOLD  = -15;
    private static final String       TAG         = "Concher";
    private static final String   REC_PATH        = "/dev/null";
    private static final long     SAMPLE_TIMER    = 250; // in ms

    private enum PLAYER_STATE {STOPPED, BAD, GOOD}

    private PLAYER_STATE                  playerState         = PLAYER_STATE.STOPPED;
    private MediaPlayer                   mediaPlayer         = null;
    private MediaRecorder                 mediaRecorder       = null;
    private Context                       context             = null;
    private ScheduledThreadPoolExecutor   sampleTimer         = null;
    private final android.os.Handler      handler             = new Handler();
    private final Random                  random              = new Random();
    private OnRecTimerListener            recTimerListener    = null;

    public OnRecTimerListener getRecTimerListener() {
        return recTimerListener;
    }

    public void setRecTimerListener(OnRecTimerListener recTimerListener) {
        this.recTimerListener = recTimerListener;
    }

    public interface OnRecTimerListener {
        public void onRecTimer(double recLevel);
    }

    public Concher(Context ctx){
        context = ctx;
    }

    public Concher(Context ctx, OnRecTimerListener listener) {
        context = ctx;
        recTimerListener = listener;
        //  This is intended to be executed in the main UI thread; the handler instance variable is
        //  then associated with that thread and can be used to post tasks to be executed on it.
    }

    public void startConching() {
        if (mediaRecorder != null)
            return;
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setOnErrorListener(this);
            mediaRecorder.setOnInfoListener(this);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(REC_PATH);
            mediaRecorder.prepare();
            mediaRecorder.start();
            sampleTimer = new ScheduledThreadPoolExecutor(1);
            sampleTimer.scheduleAtFixedRate(new Runnable() {
                private final Runnable updateMainThread = new Runnable() {
                    @Override
                    public void run() {
                        double level = getRecLevel();
                        if (recTimerListener != null)
                            recTimerListener.onRecTimer(level);
                    }
                };
                @Override
                public void run() {
                    sampleRecording();
                    handler.post(updateMainThread);
                }
            }, SAMPLE_TIMER, SAMPLE_TIMER, TimeUnit.MILLISECONDS);
            Log.v(TAG, "start() completed.");

        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void stopConching() {
        Log.v(TAG, "stopConching() called.");
        try {
            if (sampleTimer != null) {
                sampleTimer.shutdownNow();
                sampleTimer = null;
            }
            stopPlayer();
            if (mediaRecorder != null) {
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace(System.err);
        }

    }

//    public void pause() {
//
//    }


    protected double getRecLevel() {
        if (mediaRecorder != null) {
//            power_db = 20 * log10(amp / amp_ref);
            double amp = (double)mediaRecorder.getMaxAmplitude();
            return 20 * Math.log10(amp / (0.9 * MAX_AMPLITUDE));
        }

        return 0;
    }

    protected void sampleRecording() {
        double level = getRecLevel();
        if (level == 0.0)
            return;

        if (level < BAD_THRESHOLD) {
            setPlayerState(PLAYER_STATE.STOPPED);
        }
        else {
            if (level < GOOD_THRESHOLD) {
                if (playerState != PLAYER_STATE.BAD)
                    setPlayerState(PLAYER_STATE.BAD);
            }
            else {
                if (playerState != PLAYER_STATE.GOOD)
                    setPlayerState(PLAYER_STATE.GOOD);
            }
        }
    }

    private void setPlayerState(PLAYER_STATE newState) {
        Boolean r = random.nextBoolean();
        switch (newState) {
            case GOOD:
                int goodFile = r ? R.raw.goodconch1 : R.raw.goodconch2;
                playFile(goodFile);
                break;
            case BAD:
                int badFile = r ? R.raw.badconch1 : R.raw.badconch2;
                playFile(badFile);
                break;
            case STOPPED:
                stopPlayer();
                break;
        }
//        if (playerState != newState) {
//            Log.v(TAG, String.format("Set new state: %d", newState));
//        }
        playerState = newState;
    }

    private void playFile(int resourceID) {
        stopPlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnCompletionListener(this);
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resourceID);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepareAsync();
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }

    }

    @Override
    public void onPrepared(MediaPlayer player) {
        Log.v(TAG, "onPrepared() called.");
        mediaPlayer.start();
    }


    private void stopPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        Log.v(TAG, "OnCompletion() called.");
        stopPlayer();
    }


    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                Log.v(TAG, "Malformed playback error; extra=" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Log.v(TAG, "Unsupported playback error; extra=" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.v(TAG, "Playback IO error; extra=" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.v(TAG, "Playback server died; extra=" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Log.v(TAG, "Playback timeout error; extra=" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.v(TAG, "Unknown playback error; extra=" + extra);
                break;
            default:
                Log.e(TAG, "Playback error code=" + what + " extra=" + extra);
                break;
        }
        return true;
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                Log.v(TAG, "Unknown recording error; extra=" + extra);
                break;
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                Log.v(TAG, "Recording server died; extra=" + extra);
                break;
            default:
                Log.e(TAG, "Recording error code=" + what + " extra=" + extra);
                break;
        }

    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                Log.v(TAG, "Unknown recording info; extra=" + extra);
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                Log.v(TAG, "Max recording duration reached; extra=" + extra);
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                Log.v(TAG, "Max recording file size reached; extra=" + extra);
                break;
            default:
                Log.e(TAG, "Recording info code=" + what + " extra=" + extra);
                break;

        }
    }



}
