package sk.jojo.parkovanie_app.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class PriceViewModel: ViewModel() {
    private val TAG = "PriceViewModel"

    private var auth: FirebaseAuth
    private var currentUser: FirebaseUser
    private var db: FirebaseFirestore


    private var address: String = ""
    private var city: String = ""

    private var _price30 = MutableLiveData<String>()
    val price30: LiveData<String>
        get() = _price30

    private var _price45 = MutableLiveData<String>()
    val price45: LiveData<String>
        get() = _price45

    private var _price60 = MutableLiveData<String>()
    val price60: LiveData<String>
        get() = _price60


    fun setAddress(address: String){
        this.address = address
    }

    fun setCity(city: String){
        this.city = city
    }

    init{
        auth = Firebase.auth
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        currentUser = auth.currentUser
        db = FirebaseFirestore.getInstance()
    }

    fun readPriceFromDB(){
        db.collection(city).document(address)
            .get()
            .addOnSuccessListener { result ->
                Log.w(TAG, "Succes: ")
                _price30.value = result.getString("price30")
                _price45.value = result.getString("price45")
                _price60.value = result.getString("price60")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting history_of_reservation documents: ", exception)
            }
    }

    fun readPriceForActualAddress(){
        db.collection("users_reservation").document(currentUser.uid)
            .collection("active_reservation").document("reservation")
            .get()
            .addOnSuccessListener { result->
                address = result.getString("address")!!
                city = result.getString("city")!!
                readPriceFromDB()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting active_reservation documents.", exception)
            }
    }
}