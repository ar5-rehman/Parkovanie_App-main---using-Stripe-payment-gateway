package sk.jojo.parkovanie_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "sk.loginUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser

        if(currentUser != null){

            startActivity(Intent(this,MainPageActivity::class.java))
            Log.i(TAG,"je prihlaseny pouziatel "+currentUser.uid)
            finish()
        }
        else{
            newUserBtn.setOnClickListener {
                startActivity(Intent(this,RegisterActivity::class.java))
            }
            Log.i(TAG,"Nie je prihlaseny nikto ")
            loginUser()
        }
    }

    private fun loginUser() {
        loginBtn.setOnClickListener {
            when{
                TextUtils.isEmpty(emailLogin.text.toString().trim{ it <= ' '}) ->{
                    Toast.makeText(this, "Prosím zadaj email", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(passwordLogin.text.toString().trim{ it <= ' '}) ->{
                    Toast.makeText(this, "Prosím zadaj heslo", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val email: String = emailLogin.text.toString().trim{ it <= ' '}
                    val password: String = passwordLogin.text.toString().trim { it <= ' ' }
                    Log.i("parking","som pod heslom")

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Log.i("parking","task splneny")
                                // Sign in success, update UI with the signed-in user's information

                                Log.d("parking", "signInWithEmail:success")
                                Toast.makeText(this, "Úspešne si sa prihlásil",Toast.LENGTH_SHORT).show()
                                val intent = Intent(this,MainPageActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()

                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w( "parking", task.exception)
                                Toast.makeText(baseContext, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

        }
    }
}