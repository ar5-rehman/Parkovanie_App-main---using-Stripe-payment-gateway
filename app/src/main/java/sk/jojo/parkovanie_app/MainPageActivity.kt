package sk.jojo.parkovanie_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main_page.*
import kotlinx.android.synthetic.main.activity_register.*
import sk.jojo.parkovanie_app.view_model.MainViewModel
import sk.jojo.parkovanie_app.view_model.PriceViewModel


class MainPageActivity : AppCompatActivity() {
    val TAG = "sk.logy"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.readTimeFromDB()

//        val priceViewModel = ViewModelProvider(this).get(PriceViewModel::class.java)
//        priceViewModel.readPriceForActualAddress()

        setContentView(R.layout.activity_main_page)



        val bottomNav = bottom_navigation
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.mainPageFragment, R.id.mapFragment, R.id.moreFragment).build()

        val navController = Navigation.findNavController(this, R.id.navHostFragment)

//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)    //Nastavovanie nazvu Lablu v ktorom si

        NavigationUI.setupWithNavController(bottomNav, navController)

//        bottomNav.setOnClickListener {
//            onBackPressed()
//        }





//        val db: FirebaseFirestore = FirebaseFirestore.getInstance()

//        // Create a new user with a first and last name
//        val user = hashMapOf(
//            "first" to "Ada",
//            "last" to "Lovelace",
//            "born" to 1815
//        )
//
//
//        // Add a new document with a generated ID
//        db.collection("users")
//            .add(user)
//            .addOnSuccessListener { documentReference ->
//                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
//            }
//            .addOnFailureListener { e ->
//                Log.w(TAG, "Error adding document", e)
//            }
//
//
//        // Create a new user with a first, middle, and last name
//        val user2 = hashMapOf(
//            "first" to "Alan",
//            "middle" to "Mathison",
//            "last" to "Turing",
//            "born" to 1912
//        )
//
//        // Add a new document with a  ID uzivatel
//        db.collection("users").document("uzivatel")
//            .set(user2)
//            .addOnSuccessListener {
//                Log.d(TAG, "DocumentSnapshot added with ID: ")
//            }
//            .addOnFailureListener { e ->
//                Log.w(TAG, "Error adding document", e)
//            }



//        db.collection("users").document("uzivatel")
//            .get()
//            .addOnSuccessListener { result ->
////                for (document in result) {
////                    Log.d(TAG, "${document.id} => ${document.data}")
////                }
//
//                Log.d(TAG, "${result.id} => ${result.data}")
//            }
//            .addOnFailureListener { exception ->
//                Log.w(TAG, "Error getting documents.", exception)
//            }


    }

}