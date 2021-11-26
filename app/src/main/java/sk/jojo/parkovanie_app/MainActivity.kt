package sk.jojo.parkovanie_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.ViewModelProvider
import sk.jojo.parkovanie_app.view_model.MainViewModel
import java.lang.Thread.sleep


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Handler().postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

//            startActivity(Intent(this,MainPageActivity::class.java))        // Druhy sposob spustana intentu

            finish()
        }, 2000)


    }
}