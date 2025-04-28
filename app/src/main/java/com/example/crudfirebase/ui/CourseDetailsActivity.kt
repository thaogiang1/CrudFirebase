package com.example.crudfirebase

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.crudfirebase.ui.theme.CrudFirebaseTheme
import com.google.firebase.firestore.FirebaseFirestore

class CourseDetailsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrudFirebaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val courseList = mutableStateListOf<Course?>()
                    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

                    db.collection("Courses").get()
                        .addOnSuccessListener { queryDocumentSnapshots ->
                            if (!queryDocumentSnapshots.isEmpty) {
                                val list = queryDocumentSnapshots.documents
                                for (d in list) {
                                    val c: Course? = d.toObject(Course::class.java)
                                    c?.courseID = d.id
                                    Log.e("TAG", "Course id is : ${c!!.courseID}")
                                    courseList.add(c)
                                }
                            } else {
                                Toast.makeText(
                                    this@CourseDetailsActivity,
                                    "No data found in Database",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this@CourseDetailsActivity,
                                "Fail to get the data.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "Danh sách khóa học",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    ) { innerPadding ->
                        FirebaseUI(
                            context = LocalContext.current,
                            courseList = courseList,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

private fun deleteDataFromFirebase(courseID: String?, context: Context) {
    val db = FirebaseFirestore.getInstance()
    db.collection("Courses").document(courseID.toString()).delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Course Deleted successfully..", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(context, CourseDetailsActivity::class.java))
        }
        .addOnFailureListener {
            Toast.makeText(context, "Fail to delete course..", Toast.LENGTH_SHORT).show()
        }
}

@Composable
fun FirebaseUI(
    context: Context,
    courseList: SnapshotStateList<Course?>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (courseList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có khóa học nào",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        itemsIndexed(courseList) { _, item ->
            CourseCard(
                course = item,
                onClick = {
                    val intent = Intent(context, UpdateCourse::class.java).apply {
                        putExtra("courseName", item?.courseName)
                        putExtra("courseDuration", item?.courseDuration)
                        putExtra("courseDescription", item?.courseDescription)
                        putExtra("courseID", item?.courseID)
                    }
                    context.startActivity(intent)
                },
                onDelete = { deleteDataFromFirebase(item?.courseID, context) }
            )
        }
    }
}

@Composable
fun CourseCard(
    course: Course?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Course Name
            course?.courseName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Course Image
            course?.courseDescription?.let { description ->
                if (description.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = description.takeIf { it.isNotBlank() && it.startsWith("http") }
                                ?: "https://via.placeholder.com/150"
                        ),
                        contentDescription = "Course Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Course Duration
            course?.courseDuration?.let {
                Text(
                    text = "Thời lượng: $it",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Course Description
            course?.courseDescription?.let {
                if (!it.startsWith("http")) { // Chỉ hiển thị nếu không phải URL hình ảnh
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                        maxLines = 3
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sửa", fontSize = 14.sp)
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Xóa", fontSize = 14.sp)
                }
            }
        }
    }
}

private fun updateDataToFirebase(
    courseID: String?,
    name: String?,
    duration: String?,
    description: String?,
    context: Context
) {
    val updatedCourse = Course(courseID, name, duration, description)
    val db = FirebaseFirestore.getInstance()
    db.collection("Courses").document(courseID.toString()).set(updatedCourse)
        .addOnSuccessListener {
            Toast.makeText(context, "Course Updated successfully..", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(context, CourseDetailsActivity::class.java))
        }.addOnFailureListener {
            Toast.makeText(context, "Fail to update course : ${it.message}", Toast.LENGTH_SHORT).show()
        }
}