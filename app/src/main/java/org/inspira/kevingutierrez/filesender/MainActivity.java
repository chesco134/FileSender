package org.inspira.kevingutierrez.filesender;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends AppCompatActivity {

    public static final String SOURCE = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/WiFiUFO/UFO_Photo");
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, Servicio.class));
    }

    @Override
    protected void onStart(){
        super.onStart();
        doBindService();
    }

    @Override
    protected void onStop(){
        super.onStop();
        doUnbindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        if(mBoundService != null)
            menu.getItem(0).setTitle(String.valueOf(mBoundService.getId()));
        return true;
    }

    private Servicio mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mIsBound = true;
            mBoundService = ((Servicio.LocalBinder) service).getService();
            mBoundService.setActivity(MainActivity.this);
            if(menu != null)
            menu.getItem(0).setTitle(String.valueOf(mBoundService.getId()));
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };

    private boolean mIsBound;

    void doBindService() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        if(!mIsBound) {
            bindService(new Intent(this, Servicio.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            Log.d("DBZ","Bounded");
        }
    }

    public void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d("DBZ","unBounded");
        }
    }

    public static String bytesToString(byte[] byteData){
        // convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return sb.toString();
    }

    public void setIdToMenu(final int id){
        if(menu != null){
            runOnUiThread(new Runnable(){ @Override public void run(){ menu.getItem(0).setTitle(String.valueOf(id)); } } );
        }
    }
}
