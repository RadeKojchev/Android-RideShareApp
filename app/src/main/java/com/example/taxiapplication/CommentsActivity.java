package com.example.taxiapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private RecyclerView commentsRecyclerView;
    private CommentsAdapter commentsAdapter;
    private List<Comment> commentsList;
    private FirebaseFirestore db;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        db = FirebaseFirestore.getInstance();

        // Get driverId passed from DriverActivity
        driverId = getIntent().getStringExtra("driverId");

        // Initialize RecyclerView
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsList = new ArrayList<>();
        commentsAdapter = new CommentsAdapter(commentsList);
        commentsRecyclerView.setAdapter(commentsAdapter);

        // Fetch and display the comments for the driver
        fetchCommentsForDriver(driverId);
    }

    private void fetchCommentsForDriver(String driverId) {
        CollectionReference commentsRef = db.collection("drivers")
                .document(driverId)
                .collection("comments");

        Query query = commentsRef.orderBy("timestamp");

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        String text = documentSnapshot.getString("text");
                        String passengerName = documentSnapshot.getString("passengerName");
                        String timestamp = documentSnapshot.getDate("timestamp").toString();

                        // Add the comment to the list
                        commentsList.add(new Comment(passengerName, text, timestamp));
                    }
                    commentsAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(CommentsActivity.this, "No comments found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CommentsActivity.this, "Failed to load comments.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
