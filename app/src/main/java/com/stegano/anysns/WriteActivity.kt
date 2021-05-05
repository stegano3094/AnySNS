package com.stegano.anysns

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.recyclerView
import kotlinx.android.synthetic.main.activity_write.*
import kotlinx.android.synthetic.main.card_background.view.*

class WriteActivity : AppCompatActivity() {
    var currentBgPosition = 0

    val bgList = mutableListOf(
        "android.resource://com.stegano.anysns/drawable/default_bg",
        "android.resource://com.stegano.anysns/drawable/bg2",
        "android.resource://com.stegano.anysns/drawable/bg3",
        "android.resource://com.stegano.anysns/drawable/bg4",
        "android.resource://com.stegano.anysns/drawable/bg5",
        "android.resource://com.stegano.anysns/drawable/bg6",
        "android.resource://com.stegano.anysns/drawable/bg7",
        "android.resource://com.stegano.anysns/drawable/bg8",
        "android.resource://com.stegano.anysns/drawable/bg9"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)
        supportActionBar?.title = "글쓰기"

        recyclerViewSet()

        sendButton.setOnClickListener {
            if(TextUtils.isEmpty(input.text)) {
                Toast.makeText(this, "메세지를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val post = Post()
            val newRef = FirebaseDatabase.getInstance().getReference("Posts").push()
            post.writeTime = ServerValue.TIMESTAMP
            post.bgUri = bgList[currentBgPosition]
            post.message = input.text.toString()
            post.writerId = getMyId()
            post.postId = newRef.key.toString()
            newRef.setValue(post)
            Toast.makeText(this, "공유되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    private fun getMyId(): String {
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun recyclerViewSet() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdapter()
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView = itemView.imageView

    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate = LayoutInflater.from(this@WriteActivity).inflate(R.layout.card_background, parent, false)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            Picasso.get()
                .load(Uri.parse(bgList[position]))
                .fit()
                .centerCrop()
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                Picasso.get()
                    .load(Uri.parse(bgList[position]))
                    .fit()
                    .centerCrop()
                    .into(writeBackground)
            }
        }

        override fun getItemCount(): Int {
            return bgList.size
        }
    }
}