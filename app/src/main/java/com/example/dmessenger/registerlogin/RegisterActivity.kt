@file:Suppress("DEPRECATION")

package com.example.dmessenger.registerlogin

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.dmessenger.R
import com.example.dmessenger.messages.LatestMessagesActivity
import com.example.dmessenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var progressDialog: ProgressDialog
    companion object {
        val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        register_button_register.setOnClickListener {
            performRegister()
        }

        already_have_account_text_view.setOnClickListener {
            Log.d(TAG, "Try to show login activity")
            finish()
        }

        selectphoto_button_register.setOnClickListener {
            Log.d(TAG, "Try to show photo selector")

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Photo was selected")

            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            selectphoto_imageview_register.setImageBitmap(bitmap)
        }
    }

    private fun performRegister() {

        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setMessage("Please wait it may take a while")
        progressDialog.show()

        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()
        val username = username_edittext_register.text.toString()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || selectedPhotoUri == null) {
            progressDialog.dismiss()
            Toast.makeText(this, "Please fill all the credentials", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Attempting to create user with email: $email")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful){
                    progressDialog.dismiss()
                    return@addOnCompleteListener
                }else {
                    Log.d(TAG, "Successfully created user with uid: ${it.result!!.user.uid}")
                    uploadImageToFirebaseStorage()
                }
            }
            .addOnFailureListener{
                progressDialog.dismiss()
                Log.d(TAG, "Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebaseStorage(){
//        if (selectedPhotoUri == null){
//            progressDialog.dismiss()
//            return
//        }else {
//            val filename = UUID.randomUUID().toString()
//            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
//
//            ref.putFile(selectedPhotoUri!!)
//                .addOnSuccessListener {
//                    Log.d(TAG, "Successfully uploaded image: ${it.metadata?.path}")
//                    ref.downloadUrl.addOnSuccessListener {
//                        Log.d(TAG, "File Location: $it")
//                        saveUserToFirebaseDatabase(it.toString())
//                    }
//                }
//                .addOnFailureListener {
//                    progressDialog.dismiss()
//                    Log.d(TAG, "Failed to upload image to storage: ${it.message}")
//                }
//        }

        if (selectedPhotoUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

            ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully uploaded image: ${it.metadata?.path}")
                    ref.downloadUrl.addOnSuccessListener {
                        Log.d(TAG, "File Location: $it")
                        saveUserToFirebaseDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Log.d(TAG, "Failed to upload image to storage: ${it.message}")
                }
        }else {
            progressDialog.dismiss()
            Toast.makeText(applicationContext,"Please select image",Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(uid, username_edittext_register.text.toString(), profileImageUrl)

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "Finally we saved the user to Firebase Database")

                progressDialog.dismiss()
                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Log.d(TAG, "Failed to set value to database: ${it.message}")
            }
    }
}