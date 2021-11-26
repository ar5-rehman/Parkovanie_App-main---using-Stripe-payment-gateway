package sk.jojo.parkovanie_app.database


import com.google.firebase.firestore.DocumentSnapshot
import java.util.*
import kotlin.collections.HashMap

interface IDatabaseService {
    fun updateActiveResevation(minute: Long, city: String, address: String)
    fun writingToActiveReservation(reservation: HashMap<String, Any>)
    fun writingToHistoryOfReservation(reservation: HashMap<String, Any>)
    fun writingReservationToAddress(city: String, address: String, numberOfParking: Int, time: Date)

}