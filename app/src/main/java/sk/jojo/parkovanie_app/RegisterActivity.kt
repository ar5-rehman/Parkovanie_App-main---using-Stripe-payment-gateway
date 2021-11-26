package sk.jojo.parkovanie_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    private val TAG = "sk.registerUser"

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = Firebase.auth

        existingUserBtn.setOnClickListener {
            onBackPressed()
        }
        registerUser()
    }

    private fun registerUser() {
        registerBtn.setOnClickListener {
            when{
                TextUtils.isEmpty(email.text.toString().trim{ it <= ' '}) ->{
                    Toast.makeText(this, "Prosím zadaj email", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(password.text.toString().trim{ it <= ' '}) ->{
                    Toast.makeText(this, "Prosím zadaj heslo", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val email = email.text.toString().trim{ it <= ' '}
                    val password = password.text.toString().trim { it <= ' ' }
                    Log.i(TAG,"som pod heslom")

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val firebaseUser: FirebaseUser = task.result!!.user!!
                                val db: FirebaseFirestore = FirebaseFirestore.getInstance()
                                val user = hashMapOf(
                                    "name" to name.text.toString(),
                                    "surname" to surname.text.toString(),
                                    "email" to email
                                )
                                val info = hashMapOf(
                                    "start" to null,
                                    "end" to null,
                                    "city" to null,
                                    "address" to null,
                                    "idNumber" to null
                                )

                                db.collection("users").document(firebaseUser.uid)
                                    .set(user)
                                    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                db.collection("users_reservation").document(firebaseUser.uid)
                                    .collection("active_reservation").document("reservation")
                                    .set(info)


                                Log.i(TAG,"task splneny")
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success")
                                Toast.makeText(this, "Úspešne si sa zaregistrovali",Toast.LENGTH_SHORT).show()
                                val intent = Intent(this,MainPageActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()

                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w( TAG, task.exception)
                                Toast.makeText(baseContext, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show()

                            }
                        }
                }
            }

        }
    }
}