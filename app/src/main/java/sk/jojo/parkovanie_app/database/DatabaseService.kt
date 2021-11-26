package sk.jojo.parkovanie_app.database

import android.util.Log
import com.google.firebase.FirebaseError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.HashMap

class DatabaseService: IDatabaseService {
    private val TAG = "DatabaseService"

    private var auth: FirebaseAuth
    private var currentUser: FirebaseUser
    private var db: FirebaseFirestore
    private var result: DocumentSnapshot? = null

    init {
        // Initialize Firebase Auth
        auth = Firebase.auth
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        currentUser = auth.currentUser
        db = FirebaseFirestore.getInstance()
    }

    override fun updateActiveResevation(minute: Long, city: String, address: String) {
        db.collection("users_reservation").document(currentUser.uid)
            .collection("active_reservation").document("reservation")
            .get()
            .addOnSuccessListener { result ->
                val numberOfParking = result.getLong("idNumber").toString()
                val resultdb = result.data
                val time = resultdb?.get("end") as Timestamp

                val newTimeInMilis = (time.seconds * 1000 + time.nanoseconds / 1000000) + minute * 60 * 1000
                val newTime = Date(newTimeInMilis)
                Log.i(TAG,"Updatnuty cas je " + newTime.time + " alebo " + newTime)

                //Update active_reservation ked si predlzi uzivatel rezervaciu

                db.collection("users_reservation").document(currentUser.uid)
                    .collection("active_reservation").document("reservation")
                    .update("end", Timestamp(newTime))
                    .addOnSuccessListener { Log.d(TAG, "Document active_reservation successfully updated!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updating active_reservation document", e) }

                //ziskavanie posledneho pridaneho zaznamu do history_of_reservation a nasledne update end casu

                val idsRef = db.collection("users_reservation")
                    .document(currentUser.uid).collection("history_of_reservation")
                val query: Query = idsRef.orderBy("end").limitToLast(1)
                query.get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            db.collection("users_reservation").document(currentUser.uid)
                                .collection("history_of_reservation").document(document.id)
                                .update("end", Timestamp(newTime))
                                .addOnSuccessListener { Log.d(TAG, "Document history_of_reservation successfully updated!") }
                                .addOnFailureListener { e -> Log.w(TAG, "Error updating history_of_reservation document", e) }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting history_of_reservation documents: ", exception)
                    }

                /**
                 * Update endOFReservation casu pre danu adresu aby bolo miesto brane ako obsadene
                 */
                db.collection(city).document(address)
                    .collection("idNumber").document(numberOfParking)
                    .update("endOfReservation", Timestamp(newTime))
                    .addOnSuccessListener {
                        Log.d(TAG, "Document places successfully updated!")
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updating places document", e) }

            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    override fun writingToActiveReservation(reservation: HashMap<String, Any>) {
        db.collection("users_reservation").document(currentUser.uid)
            .collection("active_reservation").document("reservation")
            .update(reservation)
            .addOnSuccessListener { Log.d(TAG, "Document active_reservation successfully updated!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating active_reservation document", e) }
    }


    override fun writingToHistoryOfReservation(reservation: HashMap<String, Any>) {
        db.collection("users_reservation").document(currentUser.uid)
            .collection("history_of_reservation").document()
            .set(reservation)
            .addOnSuccessListener {
                Log.d(TAG, "Document history_of_reservation successfully set!")
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error setting history_of_reservation document", e) }
    }

    override fun writingReservationToAddress(city: String, address: String, numberOfParking: Int, time: Date) {
        db.collection(city).document(address)
            .collection("idNumber").document(numberOfParking.toString())
            .update("isReserved",true,"endOfReservation",Timestamp(time))
            .addOnSuccessListener {
                Log.d(TAG, "Document places successfully updated!")
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating places document", e) }
    }


}