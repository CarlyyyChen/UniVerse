package com.example.universe;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;


public class FragmentDisplayImage extends Fragment {

    protected static final String ARG_URI = "imageUri";
    private Uri imageUri;
    private ImageView imageViewPhoto;
    private Button buttonRetake;
    private Button buttonUpload;
    private IdisplayImageAction mListener;
    private ProgressBar progressBar;
    private ImageButton imageButtonBack;


    public FragmentDisplayImage() {
    }

    public static FragmentDisplayImage newInstance(Uri imageUri) {
        FragmentDisplayImage fragment = new FragmentDisplayImage();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable(ARG_URI);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_display_image, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        imageButtonBack = view.findViewById(R.id.displayImage_imageButton_back);
        imageButtonBack.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().popBackStack());

        imageViewPhoto = view.findViewById(R.id.imageViewPhoto);
        buttonRetake = view.findViewById(R.id.buttonRetake);
        buttonUpload = view.findViewById(R.id.buttonUpload);

        Glide.with(view)
                .load(imageUri)
                .into(imageViewPhoto);

        buttonRetake.setOnClickListener(view12 -> mListener.onRetakePressed());

        buttonUpload.setOnClickListener(view13 -> mListener.onUploadButtonPressed(imageUri, progressBar));
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof FragmentCameraController.DisplayTakenPhoto){
            mListener = (IdisplayImageAction) context;
        }else{
            throw new RuntimeException(context+" must implement RetakePhoto");
        }
    }

    public interface IdisplayImageAction {
        void onRetakePressed();
        void onUploadButtonPressed(Uri imageUri, ProgressBar progressBar);
    }
}