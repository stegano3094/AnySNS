package com.stegano.anysns

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_post.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    var ref = FirebaseDatabase.getInstance().getReference("test")  // 키값으로 읽어옴

    val posts: MutableList<Post> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "글목록"

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            startActivity(intent)
        }

        ref.addValueEventListener(object : ValueEventListener {
            // 데이터 변경 감지시 호출됨
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // test 키를 가진 데이터 스냅샷에서 값을 읽고 문자열로 변경한다
                val message = dataSnapshot.value.toString()
                // 읽은 문자 로깅
                Log.e(TAG, "onDataChange: " + message)
                // 파이어베이스에서 전달받은 메세지로 제목을 변경한다
                supportActionBar?.title = message
            }

            // 데이터 읽기가 취소된 경우
            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })


        val layoutManager = LinearLayoutManager(this@MainActivity)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdapter()

        // Firebase에서 Post 데이터를 가져온 후 posts 변수에 저장
        FirebaseDatabase.getInstance().getReference("/Posts")
            .orderByChild("writeTime").addChildEventListener(object: ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {  // 글이 추가된 경우
                    snapshot.let { snapshot ->
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            if(previousChildName == null) {
                                posts.add(it)
                                recyclerView.adapter?.notifyItemChanged(posts.size - 1)
                            } else {
                                val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                                posts.add(prevIndex + 1, post)
                                recyclerView.adapter?.notifyItemInserted(prevIndex + 1)
                            }
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {  // 글이 변경된 경우
                    snapshot.let {
                        snapshot ->
                        val post = snapshot.getValue(Post::class.java)
                        post?.let { post ->
                            val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                            posts[prevIndex + 1] = post
                            recyclerView.adapter?.notifyItemChanged(prevIndex + 1)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {  // 글이 삭제된 경우
                    snapshot.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let { post ->
                            val exitsIndex = posts.map { it.postId }.indexOf(post.postId)
                            posts.removeAt(exitsIndex)
                            recyclerView.adapter?.notifyItemRemoved(exitsIndex)
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {  // 글의 순서가 변경된 경우
                    snapshot.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let { post ->
                            val existIndex = posts.map { it.postId }.indexOf(post.postId)
                            posts.removeAt(existIndex)
                            recyclerView.adapter?.notifyItemChanged(existIndex)

                            if(previousChildName == null) {
                                posts.add(post)
                                recyclerView.adapter?.notifyItemChanged(posts.size + 1)
                            } else {
                                val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                                posts.add(prevIndex + 1, post)
                                recyclerView.adapter?.notifyItemChanged(prevIndex + 1)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {  // 취소된 경우
                    error.toException().printStackTrace()
                }
            })
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.imageView
        val contentsText: TextView = itemView.contentsText
        val timeTextView: TextView = itemView.timeTextView
        val commentCountText: TextView = itemView.commentCountText
    }

    inner class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.card_post, parent, false))
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = posts[position]

            Picasso.get()
                .load(Uri.parse(post.bgUri))
                .fit()
                .centerCrop()
                .into(holder.imageView)
            holder.contentsText.text = post.message
            holder.timeTextView.text = getDiffTimeText(post.writeTime as Long)
            holder.commentCountText.text = "0"
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra("postId", post.postId)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return posts.size
        }
    }

    private fun getDiffTimeText(targetTime: Long): String {
        val curDateTime = DateTime()
        val targetDateTime = DateTime().withMillis(targetTime)

        val diffDay = Days.daysBetween(curDateTime, targetDateTime).days
        val diffHours = Hours.hoursBetween(targetDateTime, curDateTime).hours
        val diffMinutes = Minutes.minutesBetween(targetDateTime, curDateTime).minutes

        if(diffDay == 0) {
            if(diffHours == 0 && diffMinutes == 0) {
                return "방금 전"
            }
            return if(diffHours > 0) {
                "" + diffHours + "시간 전"
            } else {
                "" + diffMinutes + "분 전"
            }
        } else {
            val format = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")
            return format.format(Date(targetTime))
        }

    }
}