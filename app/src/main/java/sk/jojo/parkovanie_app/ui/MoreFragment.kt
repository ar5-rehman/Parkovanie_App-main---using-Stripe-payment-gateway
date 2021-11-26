package sk.jojo.parkovanie_app.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import sk.jojo.parkovanie_app.LoginActivity
import sk.jojo.parkovanie_app.MainPageActivity
import sk.jojo.parkovanie_app.R
import sk.jojo.parkovanie_app.RegisterActivity
import sk.jojo.parkovanie_app.databinding.FragmentMainPageBinding
import sk.jojo.parkovanie_app.databinding.FragmentMoreBinding


class MoreFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding: FragmentMoreBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_more, container, false)

        val currentUser = auth.currentUser

        binding.logoutBtn.setOnClickListener {
            auth.signOut()
            activity?.let{
                val intent = Intent (it, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.startActivity(intent)
            }
            val mainPageActivity = MainPageActivity()
            mainPageActivity.finish()
        }

        return binding.root
    }

}