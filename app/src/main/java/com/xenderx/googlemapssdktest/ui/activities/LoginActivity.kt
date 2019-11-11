package com.xenderx.googlemapssdktest.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.xenderx.googlemapssdktest.utils.Constants
import com.xenderx.googlemapssdktest.R
import com.xenderx.googlemapssdktest.UserClient
import com.xenderx.googlemapssdktest.models.User
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_login.setOnClickListener {
            if (!validateFields()) {
                return@setOnClickListener
            }
            login()
        }
    }

    override fun onResume() {
        super.onResume()

        if (FirebaseAuth.getInstance().currentUser != null) {
            btn_login.isVisible = false
            progress_login.isVisible = true
            getUserInfo()
        }
    }

    private fun login() {
        btn_login.isVisible = false
        progress_login.isVisible = true
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
            edittext_email.editText?.text.toString(),
            edittext_password.editText?.text.toString()
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                getUserInfo()
            } else {
                Toast.makeText(this, "Login Failed ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                btn_login.isVisible = true
                progress_login.isVisible = false
            }
        }
    }

    private fun getUserInfo() {
        FirebaseFirestore.getInstance()
            .collection(getString(R.string.collection_users))
            .document(FirebaseAuth.getInstance().uid!!)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = it.result?.toObject(User::class.java)
                    (applicationContext as UserClient).user = user
                    Toast.makeText(this, "Got the user's info", Toast.LENGTH_SHORT).show()
                    btn_login.isVisible = true
                    progress_login.isVisible = false
                    gotoMainActivity(user)
                } else {
                    Toast.makeText(this, "Could not get user data", Toast.LENGTH_SHORT).show()
                    btn_login.isVisible = true
                    progress_login.isVisible = false
                }
            }
    }

    private fun gotoMainActivity(user: User?) {
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(Constants.USER, user)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    private fun validateFields(): Boolean {
        if (edittext_email.editText?.text?.isBlank()!!) {
            edittext_email.error = "Enter Email"
            return false
        }

        if (edittext_password.editText?.text?.isBlank()!!) {
            edittext_password.error = "Enter Password"
            return false
        }

        return true
    }
}
