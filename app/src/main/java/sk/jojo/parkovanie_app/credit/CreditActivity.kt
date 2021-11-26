package sk.jojo.parkovanie_app.credit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle



import sk.jojo.parkovanie_app.databinding.ActivityCreditBinding


class CreditActivity : AppCompatActivity() {



    private lateinit var binding: ActivityCreditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreditBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.buyCredit10Btn.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("amount", 10)
            startActivity(intent)
        }

        binding.buyCredit15Btn.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("amount", 15)
            startActivity(intent)
        }

        binding.buyCredit20Btn.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("amount", 20)
            startActivity(intent)
        }

    }

}