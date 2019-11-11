package com.xenderx.googlemapssdktest.groupieitems

import android.view.View
import com.google.firebase.firestore.GeoPoint
import com.xenderx.googlemapssdktest.R
import com.xenderx.googlemapssdktest.models.User
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.cardview_user.view.*
import java.util.*

public class UserItem(val user: User) : Item() {

    var view: View? = null

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.txt_user_id.text = user.userId
        viewHolder.itemView.txt_user_email.text = user.email
        viewHolder.itemView.txt_user_name.text = user.userName
        view = viewHolder.itemView
    }

    fun setCoordinates(coordinates: GeoPoint) {
        view?.let {
            it.txt_user_coordinates.text = String.format(Locale.US, "[$%f, %f]", coordinates.latitude, coordinates.longitude)
        }
    }

    override fun getLayout(): Int = R.layout.cardview_user
}