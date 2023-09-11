package com.example.blechatdemo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.blechatdemo.databinding.ActivityChatBinding;
import com.example.blechatdemo.file.CopyFileToAppDirTask;
import com.example.blechatdemo.file.FileDownloader;
import com.example.blechatdemo.file.FileHelper;
import com.example.blechatdemo.file.FileTaskListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class ChatActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, FileTaskListener {

    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 120;
    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());//The binding.getRoot() method returns the root view of the layout file
        initView();
        initBtnAction();
        initAdapter();
        listenIncomingMessage();
        askPermission();

        BleManager.getInstance().setMtu(ChatService.getInstance().connectedDevice, 512, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.d("onSetMTUFailure", "" + exception);

            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.d("onMtuChanged", "" + mtu);
            }
        });
    }

    private void askPermission() {
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE) || !EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            String[] perm = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            EasyPermissions.requestPermissions(this, "Permission required to access your documents", EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE, perm);
        } else {
            //showFilePicker(); //just doubleslashed
        }
    }

    ActivityResultLauncher<Intent> fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
//                        String path = FileHelper.getPath(result.getData().getData(),ChatActivity.this);
//                        Log.d("PAth",path);
                new CopyFileToAppDirTask(ChatActivity.this, ChatActivity.this).execute(result.getData().getData());

            }
        }
    });

    private void showFilePicker() {
        //new code picking file
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE) || !EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            String[] perm = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            EasyPermissions.requestPermissions(this, "Permission required to access your documents", EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE, perm);
            return;
        }
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*"); //"*/*" instead of text/plain to select any file
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        fileLauncher.launch(chooseFile);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List perms) {
        // Add your logic here
        showFilePicker();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List perms) {
        // Add your logic here
    }

    private void initView() {
        BleDevice device = ChatService.getInstance().connectedDevice;
        if (device == null) {
            return;
        }
        String chatName = "";
        if (!TextUtils.isEmpty(device.getName())) {
            chatName = device.getName();
        } else {
            chatName = device.getDevice().getAddress();
        }
        binding.chatName.setText(chatName);
    }

    private void initBtnAction() {
        binding.backBtn.setOnClickListener(v -> {
            ChatService.getInstance().disconnect();
            onBackPressed();
        });
        binding.sendBtn.setOnClickListener(v -> {
            sendMsg();
        });
        binding.attachBtn.setOnClickListener(v -> {
            //askPermission();
            showFilePicker();
        });
    }

    private void initAdapter() {
        //new added instead of   adapter = new ChatAdapter(messageList, new ChatAdapter.ChatItemListener() {//new added
        //            @Override
        //            public void onItemClicked(Message message) {
        //                if (message.getConteType().equals("FILE")) {
        //                    openFile(message);
        //                }
        //            }
        //        });
        adapter = new ChatAdapter(messageList, message -> {
            if (message.getText().startsWith("file://")) {
                openFile(message);
            }
        });
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        binding.listview.setLayoutManager(manager);
        binding.listview.setAdapter(adapter);
    }

    private Message buildMessage() { //constructs a Message object
        BleDevice receiver = ChatService.getInstance().connectedDevice;
        Message message = new Message();
        message.setText(binding.editText.getText().toString());
        message.setSender(ChatService.getInstance().currentAddress);
        message.setSenderName("");
        message.setReceiverName(receiver.getName());
        message.setConteType("TEXT"); //new code
        message.setReceiver(receiver.getDevice().getAddress());
        return message;
    }

    //new new new code
    private Message buildMessage(String text) { //constructs a Message object
        BleDevice receiver = ChatService.getInstance().connectedDevice;
        Message message = new Message();
        message.setText(text);
        message.setSender(ChatService.getInstance().currentAddress);
        message.setSenderName("");
        message.setReceiverName(receiver.getName());
        message.setConteType("TEXT"); //new code
        message.setReceiver(receiver.getDevice().getAddress());
        return message;
    }

    private void sendMsg() { //sending a message using the ChatService.
        if (TextUtils.isEmpty(binding.editText.getText())) {
            return;
        }
        Message m = buildMessage();
        addMessage(m);
        binding.editText.setText("");

        ChatService.getInstance().sendMessage((m.getText()), new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {

            }

            @Override
            public void onWriteFailure(BleException exception) {

            }
        });
    }

    private void addMessage(Message message) { //constructed message is added to the chat or message list
        messageList.add(message);
        adapter.reload(messageList);
    }

    private void listenIncomingMessage() { // listener for incoming messages using the ChatService
        ChatService.getInstance().setChatCallback(new ChatService.ChatListener() {
            @Override
            public void onMessageReceived(String text) {
                Log.d("onMessageReceived", text);

//                if (text.equals("file://")) {
//
//                } else {

                BleDevice receiver = ChatService.getInstance().connectedDevice;
                Message message = new Message();
                message.setText(text);
                message.setSender(receiver.getDevice().getAddress());
                message.setSenderName("");
                message.setReceiverName("");
                message.setConteType("TEXT");//new code
                message.setReceiver(ChatService.getInstance().currentAddress);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMessage(message);
                    }
                });
            }
//            }

            @Override //new code
            public void onFileReceived(String path) {
                BleDevice receiver = ChatService.getInstance().connectedDevice;
                Message message = new Message();
                message.setText(path);
                message.setConteType("FILE");
                message.setSender(receiver.getDevice().getAddress());
                message.setSenderName("");
                message.setReceiverName("");
                message.setReceiver(ChatService.getInstance().currentAddress);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMessage(message);
                    }
                });
            }
        });
    }

    //new code
    private void sendNotifFileWritedone() {
        ChatService.getInstance().sendFileNotificationMessage("#FILE_END".getBytes(), new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {

            }

            @Override
            public void onWriteFailure(BleException exception) {

            }
        });
    }

    int totalBytesSent = 0;

    private void sendfile(File file) { // sending a file using the ChatService
        byte[] data = FileHelper.convertBytes(file);
        if (data == null) {
            Log.e("sendFile", "Failed to read file");
            return;
        }
        Log.d("onWriteInit", "file size " + data.length);
        totalBytesSent = 0;
        ChatService.getInstance().sendFileMessage(data, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                Log.d("onWriteSuccess", "current " + total);
                Log.d("onWriteSuccess", "total " + total);
                Log.d("onWriteSuccess", "justWrite " + justWrite.length);
                totalBytesSent += justWrite.length;
                if (totalBytesSent == data.length) {
                    sendNotifFileWritedone();
                }
                Log.d("onWriteSuccess", "totalBytesSent " + totalBytesSent);

            }

            @Override
            public void onWriteFailure(BleException exception) {
                Log.d("onWriteFailure", "Erorr " + exception);

            }
        });

        BleDevice receiver = ChatService.getInstance().connectedDevice;
        Message message = new Message();
        message.setText(file.getPath());
        message.setConteType("FILE");
        message.setSender(ChatService.getInstance().currentAddress);
        message.setSenderName("");
        message.setReceiverName("");
        message.setReceiver(receiver.getDevice().getAddress());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addMessage(message);
            }
        });
    }

    @Override //new code
    public void onFileReceived(File file) {

        ProgressDialog progressDialog2 = new ProgressDialog(ChatActivity.this);
        progressDialog2.setTitle("Sending File...");
        progressDialog2.setMessage("Please wait while we are sending the file");
        progressDialog2.setIndeterminate(false);
        progressDialog2.setCancelable(false);
        progressDialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog2.show();

//        FileUploader fileUploader = new FileUploader(getApplicationContext(), file, "https://sgcap2.in/saquib-client/file-uploader.php", new FileUploader.FileUploadListener() {
        Uri fileUri = Uri.fromFile(file);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("files").child(fileUri.getLastPathSegment());

        UploadTask uploadTask = storageRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Task<Uri> downloadUrlTask = taskSnapshot.getStorage().getDownloadUrl();

            downloadUrlTask.addOnSuccessListener(downloadUrl -> {
                String fileDownloadUrl = downloadUrl.toString();

                progressDialog2.dismiss();
                String msg = "file://" + file.getName() + "<:>" + fileDownloadUrl;
                runOnUiThread(() -> addMessage(buildMessage(msg)));
                ChatService.getInstance().sendMessage(msg, new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {

                    }

                    @Override
                    public void onWriteFailure(BleException exception) {

                    }
                });
                Toast.makeText(ChatActivity.this, "File Sent Successfully", Toast.LENGTH_SHORT).show();

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Failed to retrieve the download URL
                }
            });
        }).addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed Send File", Toast.LENGTH_SHORT).show()).addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog2.setProgress((int) progress);
        });

    }

    private String getMimeType(Uri uri) {
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public void openFile(Message message) {
        // try {
        //            File file = new File(message.getText());
        //            Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        //            Intent intent = new Intent(Intent.ACTION_VIEW);
        //            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //            intent.setDataAndType(photoURI, "*/*");
        //            startActivity(intent);
        //        } catch (ActivityNotFoundException e) {
        //            // no Activity to handle this kind of files
        //        }
        String rootDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File folder = new File(rootDirectoryPath + "/" + "blechat");

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                rootDirectoryPath = getCacheDir().getPath();
                folder = new File(rootDirectoryPath);
//                Toast.makeText(this, "Storage Permission not granted", Toast.LENGTH_SHORT).show();

            }
        }
        String filePath = folder.getPath() + "/" + message.getText().replace("file://", "").split("<:>")[0];
        File file = new File(filePath);
        if (file.exists()) {
            openFileIntent(file);
        } else {
            ProgressDialog progressDialog2 = new ProgressDialog(ChatActivity.this);
            progressDialog2.setTitle("Receiving File...");
            progressDialog2.setMessage("Please wait while we are Receiving the file");
            progressDialog2.setIndeterminate(false);
            progressDialog2.setCancelable(false);
            progressDialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog2.show();
            FileDownloader fileDownloader = new FileDownloader(new FileDownloader.FileDownloadListener() {
                @Override
                public void onDownloadProgress(int progress) {
                    progressDialog2.setProgress(progress);
                }

                @Override
                public void onDownloadCompleted() {
                    Toast.makeText(ChatActivity.this, "File Received Successfully", Toast.LENGTH_SHORT).show();
                    progressDialog2.dismiss();
                    openFileIntent(file);

                }

                @Override
                public void onDownloadFailed() {
                    progressDialog2.dismiss();
                    Toast.makeText(ChatActivity.this, "Failed to Receive File", Toast.LENGTH_SHORT).show();

                }
            });


            fileDownloader.execute(message.getText().replace("file://", "").split("<:>")[1], filePath);
        }
    }

    private void openFileIntent(File path) {
        Uri fileUri = FileProvider.getUriForFile(this, "com.example.blechatdemo.provider", path);
        System.out.println("path " + path.getPath());
        // Create an intent to open the file
        Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
        openFileIntent.setDataAndType(fileUri, getMimeType(fileUri));
        openFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Verify that there is an app available to handle the intent
        if (openFileIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(openFileIntent);
        } else {
            Intent openAnywhereIntent = new Intent(Intent.ACTION_VIEW);
            openAnywhereIntent.setData(fileUri);
            openFileIntent.setDataAndType(fileUri, "*/*");
            openAnywhereIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Verify if there is any app available to handle the file regardless of the MIME type
            if (openAnywhereIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(openAnywhereIntent);
            } else {
                // Handle the case where no app is available to handle the file anywhere
                Toast.makeText(this, "No app found to open the file", Toast.LENGTH_SHORT).show();
            }
            // Handle the case where no app is available to handle the intent

        }
    }
}
