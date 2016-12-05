package org.inspira.kevingutierrez.filesender;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

public class Servicio extends Service {

    private static final int POLIVOTO_SERVICE = 319;
    private static boolean TRUE = true;
    private ServiceHandler mServiceHandler;
    private NotificationManager mNM;
    private Activity mActivity;
    private LocalBinder mBinder = new LocalBinder();
    private static int id;

    @Override
    public IBinder onBind(Intent i){
        return mBinder;
    }

    public class LocalBinder extends Binder {

        public Servicio getService() {
            return Servicio.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            while(TRUE) {
                BluetoothSocket temp;
                BluetoothSocket socket = null;
                // Así se crea un objeto de "BluetoothDevice" a partir del string de la mac.
                BluetoothDevice btDev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                        //"14:30:C6:39:69:19"); // MotoHellvin
                        //"50:68:0A:A6:6E:6D"); // Huawei
                        "50:A4:C8:B8:D1:0E");
                // "1c:7b:21:f6:79:2c");
                try {
                    // Lo primero que hay que intentar es armar el objeto IOHandler,
                    // que permitirá el envío del archivo en bloques.
                    // Si anteriormente no pudismos crear el socket blutú, aquí hay un
                    // error y saltamos inmediatamente al "catch", terminando la operación.
                    //IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                    // Por pruebas preliminares, se determinó que con esta taza de bytes
                    // no hay problema de escritura de entero.
                    //ioHandler.setRate(512);
                    // Lo que sigue es lo mismo que está en la parte de wifi.
                    //PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    //BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    FileInputStream fis;
                    File directory = new File(MainActivity.SOURCE);
                    File currentFile;
                    String line;
                    String[] fileNameParts;
                    DataInputStream entrada;
                    JSONObject json = new JSONObject();
                    byte[] bytesDeArchivo;
                    int length;
                    ByteArrayOutputStream baosFile;
                    String[] dirList = directory.list();
                    Log.e("Servicio", "La lista está vacía? " + (dirList == null));
                    if(dirList != null)
                    for (String fName : dirList) {
                        currentFile = new File(MainActivity.SOURCE + "/" + fName);
                        if (!currentFile.isDirectory()) {
                            fis = new FileInputStream(currentFile);
                            bytesDeArchivo = new byte[1024];
                            entrada = new DataInputStream(fis);
                            baosFile = new ByteArrayOutputStream();
                            while ((length = entrada.read(bytesDeArchivo)) != -1)
                                baosFile.write(bytesDeArchivo, 0, length);
                            fis.close();
                            // El monstruo que sigue a continuación es para conectarnos por medio de blutú
                            // al dispositovo de la mac que seleccionamos anteriormente.
                            try {
                                socket = btDev.createRfcommSocketToServiceRecord(UUID.fromString("035db532-1f9e-425a-ace8-8b271d33d3d9"));
                                // Los logs son banderas de depuración.
                                Log.e("Melchor", "Connecting...");
                                socket.connect();
                                Log.e("Melchor", "Connected, preparing streams...");
                            } catch (IOException e) {
                                // Si no fue posible conectarse a la buena, lo hacemos a la mala.
                                Log.e("Tulman", "There was an error while establishing Bluetooth connection. Falling back..", e);
                                Class<?> clazz = socket.getRemoteDevice().getClass();
                                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                                try {
                                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                                    Object[] params = new Object[]{Integer.valueOf(1)};
                                    temp = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
                                    temp.connect();
                                    socket = temp;
                                } catch (Exception e2) {
                                    // Si llegamos aquí es que de plano se seleccionó un dispositivo
                                    // que no está al alcance (dispositivos recordados).
                                    Log.e("TulmanSan", "Couldn't fallback while establishing Bluetooth connection. Stopping app..", e2);
                                }
                                e.printStackTrace();
                            }
                            IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                            if(id == -5) {
                                json.put("action", 2);
                                baosFile.reset();
                                ioHandler.setRate(600);
                                ioHandler.sendMessage(json.toString().getBytes());
                                json = new JSONObject(new String(ioHandler.handleIncommingMessage()));
                                Log.d("Servicio", json.toString());
                                if(json.getBoolean("status") && json.getInt("id") != -1){
                                    id = json.getInt("id");
                                    if(mActivity!=null)
                                        ((MainActivity)mActivity).setIdToMenu(id);
                                }
                            }else {
                                fileNameParts = fName.split("\\.");
                                json.put("action", 1);
                                json.put("payload", MainActivity.bytesToString(baosFile.toByteArray()));
                                json.put("nombre", id+"_"+fileNameParts[0].concat("_").concat(new MD5Hash().makeHash(json.getString("payload"))).concat("."+fileNameParts[1]));
                                baosFile.reset();
                                ioHandler.setRate(600);
                                line = json.toString();
                                ioHandler.sendMessage(line.getBytes());
                                json = new JSONObject(new String(ioHandler.handleIncommingMessage()));
                                if (json.getBoolean("status")) {
                                    currentFile.delete();
                                    Log.e("Servicio", "Done sending photo");
                                }
                            }
                            socket.close();
                        }
                    }
                } catch (NullPointerException | JSONException | IOException e) {
                    e.printStackTrace();
                }
                try{
                    synchronized (this){
                        wait(1000);
                    }
                }catch(InterruptedException e){

                }
            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        id = -5;
        Log.d("Servicio", "Volvimos a iniciar");
        // Get the HandlerThread's Looper and use it for our Handler
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the
        // job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        makeNotification();
        // If we get killed, after returning from here, restart (START_STICKY)
        return START_STICKY;
    }

    private void makeNotification(){
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Creates an explicit intent for an Activity in your app
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.logo_notification)
                .setOngoing(true)
//                .setContentIntent(resultPendingIntent)
                .setVibrate(new long[]{100, 100, 100, 600})
                .setContentTitle("ASR")
                .setContentText("Sincronizando");
        mNM.notify(POLIVOTO_SERVICE, mBuilder.build());
    }

    public void setActivity(Activity activity){
        mActivity = activity;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }
}