package com.netlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netlens.ui.theme.NetlensTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val client by lazy { (application as DemoApp).client }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetlensTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DemoScreen(
                        onFireRequests = ::fireSampleRequests,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    /** Fires a spread of calls so the NetLens viewer has interesting traffic. */
    private fun fireSampleRequests() {
        val json = "application/json".toMediaType()
        val requests = listOf(
            Request.Builder().url("https://jsonplaceholder.typicode.com/users/1").build(),
            Request.Builder().url("https://jsonplaceholder.typicode.com/posts").build(),
            Request.Builder()
                .url("https://jsonplaceholder.typicode.com/posts")
                .post("""{"title":"NetLens","body":"hello","userId":1}""".toRequestBody(json))
                .header("Authorization", "Bearer demo-secret-token") // redacted in cURL/HAR
                .build(),
            Request.Builder().url("https://jsonplaceholder.typicode.com/posts/999999").build(),
            Request.Builder().url("https://httpbin.org/status/500").build(),
            Request.Builder().url("https://picsum.photos/300").build(),
        )
        requests.forEach { request ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = Unit
                override fun onResponse(call: Call, response: Response) = response.close()
            })
        }
    }
}

@Composable
private fun DemoScreen(
    onFireRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rounds by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("NetLens Demo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tap below to make sample API calls, then tap the floating bubble " +
                "(or shake the device) to inspect them.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                onFireRequests()
                rounds++
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Fire sample requests")
        }
        if (rounds > 0) {
            Text(
                "Fired $rounds ${if (rounds == 1) "batch" else "batches"} — open the bubble to inspect 👀",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
