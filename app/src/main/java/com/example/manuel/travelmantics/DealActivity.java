package com.example.manuel.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private EditText title_editText,price_editText,description_editText;
    private static final int PICTURE_RESULT=42;
    ImageView imageView;
    TravelDeal deal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference =FirebaseUtil.mDatabaseReference;
        title_editText= (EditText) findViewById(R.id.title_editText);
        price_editText=(EditText)findViewById(R.id.price_editText);
        description_editText=(EditText)findViewById(R.id.description_editText);
        imageView=(ImageView)findViewById(R.id.image);

        final Intent intent= getIntent();
        TravelDeal deal=(TravelDeal) intent.getSerializableExtra("Deal");
        if(deal==null){
             deal= new TravelDeal();
        }
        this.deal=deal;
        title_editText.setText(deal.getTitle());
        description_editText.setText(deal.getDescription());
        price_editText.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        Button btnImage=(Button) findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loadImage= new Intent(Intent.ACTION_GET_CONTENT);
                loadImage.setType("image/jpeg");
                loadImage.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(loadImage.createChooser(loadImage,"Insert Picture"),PICTURE_RESULT);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater= getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
            findViewById(R.id.btnImage).setEnabled(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
            findViewById(R.id.btnImage).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){

            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this,"Deal successfully saved",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;

            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this,"Deal successfully deleted",Toast.LENGTH_SHORT).show();
                backToList();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PICTURE_RESULT&& resultCode==RESULT_OK){
            Uri imageUri= data.getData();
            final StorageReference ref= FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> task= taskSnapshot.getMetadata().getReference().getDownloadUrl();
                    task.addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url=uri.toString();
                            deal.setImageUrl(url);Log.d("Url: ", url);
                            showImage(url);
                        }
                    });
                }
            });
        }
    }


    private void saveDeal() {
        deal.setTitle(title_editText.getText().toString());
        deal.setDescription(description_editText.getText().toString());
        deal.setPrice(price_editText.getText().toString());

        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    public void backToList(){
        Intent intent= new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    public void deleteDeal(){
        if (deal== null) {
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }

        mDatabaseReference.child(deal.getId()).removeValue();
        Log.d("image name", deal.getImageName());
        if(deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image", "Image Successfully Deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());
                }
            });
        }
    }

    private void clean(){
        title_editText.setText("");
        description_editText.setText("");
        price_editText.setText("");
        title_editText.requestFocus();
    }

    private void enableEditTexts(boolean isEnabled){
        title_editText.setEnabled(isEnabled);
        description_editText.setEnabled(isEnabled);
        price_editText.setEnabled(isEnabled);
    }

    private void showImage(String url){
        if(url!=null&& !url.isEmpty()){
            int width= Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width,width*2/3)
                    .centerCrop()
                    .into(imageView);
        }
    }
}
