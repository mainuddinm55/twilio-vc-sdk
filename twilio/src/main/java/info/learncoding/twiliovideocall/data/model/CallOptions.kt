package info.learncoding.twiliovideocall.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CallOptions(
    @SerializedName("token_url")
    val tokenUrl: String,
    @SerializedName("room_name")
    val roomName: String,
    @SerializedName("remote_user_id")
    val remoteUserId: String,
    @SerializedName("action_data")
    val actionData: String?,
    @SerializedName("remote_identity")
    val remoteIdentity: String,
    @SerializedName("attachment")
    val attachments: List<String>?,
    @SerializedName("user_type")
    val userType: UserType
) : Parcelable