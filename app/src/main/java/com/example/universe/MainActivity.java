package com.example.universe;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.universe.Models.Chat;
import com.example.universe.Models.Event;
import com.example.universe.Models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements Login.IloginFragmentAction,
        HomeFragment.IhomeFragmentAction, ChatAdapter.IChatListRecyclerAction,
        ChatManager.IchatManagerFragmentAction, Register.IRegisterFragmentAction,
        Profile.IProfileFragmentAction, Setting.ISettingFragmentAction,
        FragmentCameraController.DisplayTakenPhoto, FragmentDisplayImage.IdisplayImageAction,
        ChatRoom.IchatFragmentButtonAction, FragmentDisplayFile.IdisplayFileAction,
        HomeEventAdapter.IEventListRecyclerAction, PostFragment.IPostFragmentAction,
        FollowerAdapter.IFollowerListRecyclerAction, SearchFragment.ISearchFragmentAction,
        EventFragment.IEventFragmentAction, Followers.IFollowerFragmentAction,
        ParticipantAdapter.IEventListRecyclerAction, MessageAdapter.IMessageListRecyclerAction {

    private Util util;
    private FirebaseAuth mAuth;
    private String otherUserId;
    private Boolean cameraAllowed;
    private Boolean readAllowed;
    private Boolean writeAllowed;
    private static final int PERMISSIONS_CODE_POSTEVENT = 0x100;
    private static final int PERMISSIONS_CODE_SETTING = 0x200;
    private static final int PERMISSIONS_CODE_CHATROOM = 0x300;
    private static final int PERMISSIONS_CODE_FILE = 0x400;
    private static final int PERMISSIONS_CODE_HOME = 0x500;

    private static final String HOME_FRAGMENT = "FragmentHome";
    private static final String PROFILE_FRAGMENT = "FragmentProfile";
    private static final String OTHER_PROFILE_FRAGMENT = "FragmentOtherProfile";
    private static final String LOGIN_FRAGMENT = "FragmentLogin";
    private static final String CHAT_FRAGMENT = "FragmentChat";
    private static final String EVENT_FRAGMENT = "FragmentEvent";
    private static final String CHAT_MANAGER_FRAGMENT = "FragmentChatManager";
    private static final String FOLLOWERS_FRAGMENT = "FragmentFollowers";
    private static final String REGISTER_FRAGMENT = "FragmentRegister";
    private static final String SETTING_FRAGMENT = "FragmentSetting";
    private static final String SEARCH_FRAGMENT = "FragmentSearch";
    private static final String CAMERA_FRAGMENT = "FragmentCamera";
    private static final String FILE_FRAGMENT = "FragmentFile";
    private static final String DISPLAY_FILE_FRAGMENT = "FragmentDisplayFile";
    private static final String DISPLAY_IMAGE_FRAGMENT = "FragmentDisplayImage";
    private static final String POST_FRAGMENT = "FragmentPost";


    private GoogleSignInClient googleSignInClient;


    // For Google sign in
    private final ActivityResultLauncher<Intent> startActivityForResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Task<GoogleSignInAccount> signInAccountTask =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            if (signInAccountTask.isSuccessful()) {
                                Toast.makeText(this, "Google sign in successful", Toast.LENGTH_SHORT).show();
                                try {
                                    GoogleSignInAccount googleSignInAccount = signInAccountTask.getResult(ApiException.class);
                                    if (googleSignInAccount != null) {
                                        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);
                                        mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, task -> {
                                            if (task.isSuccessful()) {
                                                util.getDB().collection("users")
                                                        .document(util.getCurrentUser().getUid()).get().addOnCompleteListener(task1 -> {
                                                            DocumentSnapshot snapshot = task1.getResult();
                                                            if (task1.isSuccessful()) {
                                                                if (!snapshot.exists()) {
                                                                    User tempUser = new User(mAuth.getUid(),
                                                                            util.getCurrentUser().getDisplayName()
                                                                            , util.getCurrentUser().getEmail());
                                                                    util.getDB().collection("users")
                                                                            .document(util.getCurrentUser().getUid())
                                                                            .set(tempUser).addOnSuccessListener(unused -> populateScreen());
                                                                } else {
                                                                    populateScreen();
                                                                }
                                                            }
                                                        });
                                            } else {
                                                Toast.makeText(MainActivity.this, "Authentication Failed :"
                                                        + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();


        //For Google sign in
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        cameraAllowed = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        readAllowed = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        writeAllowed = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        Boolean locationAllowed = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Boolean coarseLocationAllowed = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Boolean notificationAllowed = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationAllowed = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        }


        if (cameraAllowed && readAllowed && writeAllowed && locationAllowed && coarseLocationAllowed && Boolean.TRUE.equals(notificationAllowed)) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS

            }, PERMISSIONS_CODE_HOME);
        }
        util = Util.getInstance();
    }



    @Override
    protected void onStart() {
        super.onStart();
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackCount < 2) {
            populateScreen();
        }
    }

    private void populateScreen() {
        if (util.getCurrentUser() != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, HomeFragment.newInstance(), HOME_FRAGMENT)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, Login.newInstance(), LOGIN_FRAGMENT)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void populateMainFragment(FirebaseUser mUser) {
        populateScreen();
    }


    @Override
    public void populateRegisterFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, Register.newInstance(), REGISTER_FRAGMENT)
                .addToBackStack(LOGIN_FRAGMENT).commit();
    }

    @Override
    public void openChatManager() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, ChatManager.newInstance(), CHAT_MANAGER_FRAGMENT)
                .addToBackStack(HOME_FRAGMENT).commit();
    }

    @Override
    public void openProfile(User user) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, Profile.newInstance(user), PROFILE_FRAGMENT)
                .addToBackStack(HOME_FRAGMENT).commit();
    }

    @Override
    public void openPost(Event event) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, PostFragment.newInstance(event), POST_FRAGMENT)
                .addToBackStack(HOME_FRAGMENT).commit();
    }

    @Override
    public void showResult(String query, User user) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, SearchFragment.newInstance(query, user), SEARCH_FRAGMENT)
                .addToBackStack(HOME_FRAGMENT).commit();
    }

    @Override
    public void logOut() {
        mAuth.signOut();
        populateScreen();
    }

    @Override
    public void setAvatar() {
        if (cameraAllowed && readAllowed && writeAllowed) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, FragmentCameraController.newInstance(), CAMERA_FRAGMENT)
                    .addToBackStack(SETTING_FRAGMENT).commit();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSIONS_CODE_SETTING);
        }
    }

    @Override
    public void chatClickedFromRecyclerView(Chat chat) {
        util.readChat(chat.getOtherUserId(), unused -> startChatPage(chat.getOtherUserId()), Util.DEFAULT_F_LISTENER);
    }

    @Override
    public void startChatPage(String otherUserId) {
        Bundle bundle = new Bundle();
        this.otherUserId = otherUserId;
        bundle.putString("otherUserId", otherUserId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, ChatRoom.newInstance().getClass(),
                        bundle, CHAT_FRAGMENT)
                .addToBackStack(CHAT_MANAGER_FRAGMENT)
                .commit();
    }

    public void signWithGoogle() {
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult.launch(intent);
    }

    @Override
    public void backToPrevious() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void editPost(Event event) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, PostFragment.newInstance(event), POST_FRAGMENT)
                .addToBackStack(EVENT_FRAGMENT).commit();
    }

    @Override
    public void startChatPageFromEvent(String otherUserId) {
        Bundle bundle = new Bundle();
        this.otherUserId = otherUserId;
        bundle.putString("otherUserId", otherUserId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, ChatRoom.newInstance().getClass(),
                        bundle, CHAT_FRAGMENT)
                .addToBackStack(EVENT_FRAGMENT)
                .commit();
    }

    @Override
    public void openHostProfile(User user) {
        openProfile(user);
    }

    @Override
    public void populateSettingFragment(User user) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, Setting.newInstance(user), SETTING_FRAGMENT)
                .addToBackStack(PROFILE_FRAGMENT).commit();
    }

    @Override
    public void populateFollowerFragment(User user, int tabNum) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, Followers.newInstance(user, tabNum), FOLLOWERS_FRAGMENT)
                .addToBackStack(PROFILE_FRAGMENT).commit();
    }

    @Override
    public void startChatPageFromProfile(String otherUserId) {
        Bundle bundle = new Bundle();
        this.otherUserId = otherUserId;
        bundle.putString("otherUserId", otherUserId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, ChatRoom.newInstance().getClass(),
                        bundle, CHAT_FRAGMENT)
                .addToBackStack(PROFILE_FRAGMENT)
                .commit();
    }

    @Override
    public void onTakePhoto(Uri imageUri) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, FragmentDisplayImage.newInstance(imageUri), DISPLAY_IMAGE_FRAGMENT)
                .addToBackStack(null).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 2 && requestCode == PERMISSIONS_CODE_POSTEVENT) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, FragmentCameraController.newInstance(), CAMERA_FRAGMENT)
                    .addToBackStack(POST_FRAGMENT).commit();

            //Log.d(TAG, "onRequestPermissionsResult: permission granted + open camera from post event");
        } else if (grantResults.length > 2 && requestCode == PERMISSIONS_CODE_SETTING) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, FragmentCameraController.newInstance(), CAMERA_FRAGMENT)
                    .addToBackStack(SETTING_FRAGMENT).commit();

            //Log.d(TAG, "onRequestPermissionsResult: permission granted + open camera from profile");
        } else if (grantResults.length > 2 && requestCode == PERMISSIONS_CODE_CHATROOM) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, FragmentCameraController.newInstance(), CAMERA_FRAGMENT)
                    .addToBackStack(CHAT_FRAGMENT).commit();

            //Log.d(TAG, "onRequestPermissionsResult: permission granted + open camera from chatroom");
        } else if (requestCode == PERMISSIONS_CODE_FILE && grantResults.length > 0 && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {
            selectFile();
        }
    }

    //Retrieving an image from gallery....
    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Intent data = result.getData();
                    Uri selectedImageUri = data.getData();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.containerMain,
                                    FragmentDisplayImage.newInstance(selectedImageUri), DISPLAY_IMAGE_FRAGMENT)
                            .addToBackStack(null).commit();
                }
            }
    );

    @Override
    public void onOpenGalleryPressed() {
//        takePhotoNotFromGallery = false;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRetakePressed() {
       backToPrevious();
    }

    @Override
    public void onUploadButtonPressed(Uri imageUri, ProgressBar progressBar) {
//        ProgressBar.......
        progressBar.setVisibility(View.VISIBLE);
//        Upload an image from local file....
        List<String> arrayList = Arrays.stream(imageUri.getPath().split("/")).collect(Collectors.toList());

        StorageReference storageReference = util.getStorage().getReference().child("images/"
                + arrayList.get(arrayList.size() - 1));
        UploadTask uploadImage = storageReference.putFile(imageUri);
        uploadImage.addOnFailureListener(
                e -> Toast.makeText(MainActivity.this, "Upload Failed! Try again!", Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    int count = getSupportFragmentManager().getBackStackEntryCount();
                    String name = getSupportFragmentManager().getBackStackEntryAt(count - 2).getName();
                    if (name != null) {
                        switch (name) {
                            case POST_FRAGMENT:
                                PostFragment p = (PostFragment) getSupportFragmentManager().findFragmentByTag(POST_FRAGMENT);
                                if (p != null) {
                                    p.setPostPicPath(storageReference.getPath());
                                }
                                break;
                            case SETTING_FRAGMENT:
                                Setting s = (Setting) getSupportFragmentManager().findFragmentByTag(SETTING_FRAGMENT);
                                if (s != null) {
                                    s.setNewAvatarPath(storageReference.getPath());
                                }
                                break;
                            case CHAT_FRAGMENT:
                                ChatRoom fragment = (ChatRoom) getSupportFragmentManager().findFragmentByTag(CHAT_FRAGMENT);
                                if (fragment != null) {
                                    fragment.sendImage(storageReference.getPath());
                                }
                                break;
                        }
                        getSupportFragmentManager().popBackStack(getSupportFragmentManager().getBackStackEntryAt(count - 3).getName(), 0);
                    } else {
                        String otherName = getSupportFragmentManager().getBackStackEntryAt(count - 3).getName();
                        switch (Objects.requireNonNull(otherName)) {
                            case POST_FRAGMENT:
                                PostFragment p = (PostFragment) getSupportFragmentManager().findFragmentByTag(POST_FRAGMENT);
                                if (p != null) {
                                    p.setPostPicPath(storageReference.getPath());
                                }
                                break;
                            case SETTING_FRAGMENT:
                                Setting s = (Setting) getSupportFragmentManager().findFragmentByTag(SETTING_FRAGMENT);
                                if (s != null) {
                                    s.setNewAvatarPath(storageReference.getPath());
                                }
                                break;
                            case CHAT_FRAGMENT:
                                ChatRoom fragment = (ChatRoom) getSupportFragmentManager().findFragmentByTag(CHAT_FRAGMENT);
                                if (fragment != null) {
                                    fragment.sendImage(storageReference.getPath());
                                }
                                break;
                        }
                        getSupportFragmentManager().popBackStack(otherName, 0);
                    }
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) progress);
                });
    }


    @Override
    public void sendImage() {
        if (cameraAllowed && readAllowed && writeAllowed) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, FragmentCameraController.newInstance(), CAMERA_FRAGMENT)
                    .addToBackStack(CHAT_FRAGMENT).commit();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSIONS_CODE_CHATROOM);
        }
    }

    //Retrieving a file....
    ActivityResultLauncher<Intent> fileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data != null) {
                    Uri fileUri = data.getData();
                    String filePath = fileUri.getPath();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.containerMain, FragmentDisplayFile.newInstance(fileUri, filePath), DISPLAY_FILE_FRAGMENT)
                            .addToBackStack(null).commit();
                }
            }
    );

    @Override
    public void sendFile() {
        if (ActivityCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission
                        .READ_EXTERNAL_STORAGE)
                != PackageManager
                .PERMISSION_GRANTED) {
            // When permission is not granted
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission
                            .READ_EXTERNAL_STORAGE},
                    PERMISSIONS_CODE_FILE);
        } else {
            selectFile();
        }
    }


    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        String[] mimeTypes = {"application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        fileLauncher.launch(intent);

    }

    @Override
    public void onReselectPressed() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onUploadFileButtonPressed(Uri fileUri, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        List<String> arrayList = Arrays.stream(fileUri.getPath().split("/")).collect(Collectors.toList());

        StorageReference storageReference = util.getStorage().getReference()
                .child("files/" + arrayList.get(arrayList.size() - 1));

        UploadTask uploadFile = storageReference.putFile(fileUri);
        uploadFile.addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Upload Failed! Try again!", Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(MainActivity.this, "File sent successful!", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) progress);
                });

        Task<Uri> urlTask = uploadFile.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return storageReference.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                ChatRoom fragment = (ChatRoom) getSupportFragmentManager().findFragmentByTag(CHAT_FRAGMENT);
                if (fragment != null) {
                    fragment.sendFile(downloadUri);
                }
                backToPrevious();
            } else {
                Toast.makeText(MainActivity.this, "Sending file failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void eventClickedFromRecyclerView(Event event, User user) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, EventFragment.newInstance(event, user), EVENT_FRAGMENT)
                .addToBackStack(HOME_FRAGMENT).commit();
    }

    @Override
    public void setEventPic() {
        if (cameraAllowed && readAllowed && writeAllowed) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerMain, PostFragment.newInstance(null), POST_FRAGMENT)
                    .addToBackStack(null).commit();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSIONS_CODE_POSTEVENT);
        }
    }


    @Override
    public void followerClickedFromRecyclerView(User user) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, Profile.newInstance(user), OTHER_PROFILE_FRAGMENT)
                .addToBackStack(FOLLOWERS_FRAGMENT).commit();
    }

    @Override
    public void saveEvent(Event event) {
        if (event != null) {
            util.saveDraftEvent(event, unused -> {
                Toast.makeText(MainActivity.this, "Event draft saved!", Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().popBackStack();
            }, Util.DEFAULT_F_LISTENER);
        }
    }

    @Override
    public void postEvent(Event event) {
        if (event != null) {
            util.postEvent(event, unused -> {
                Toast.makeText(MainActivity.this, "Event posted!", Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().popBackStack();
            }, Util.DEFAULT_F_LISTENER);
        }
    }

    @Override
    public void eventClickedFromRecyclerView(User user) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, Profile.newInstance(user), OTHER_PROFILE_FRAGMENT)
                .addToBackStack(EVENT_FRAGMENT).commit();
    }

    @Override
    public void openFile(String url) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMain, FileViewer.newInstance(url),FILE_FRAGMENT)
                .addToBackStack(CHAT_FRAGMENT).commit();
    }
}