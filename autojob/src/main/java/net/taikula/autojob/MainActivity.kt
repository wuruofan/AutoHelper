package net.taikula.autojob

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import net.taikula.autojob.ui.theme.AutoHelperTheme
import net.taikula.autojob.utils.AppUtils
import net.taikula.autojob.worker.AutoWorkerManager
import net.taikula.autojob.worker.LarkWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHelperTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting(getString(R.string.app_name))
                        ButtonList(this@MainActivity)
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonList(context: Context) {
    Column {
        Button(
            onClick = {
                AppUtils.startLark(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "打开Lark")
        }
        Button(
            onClick = {
                val uploadWorkRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<LarkWorker>()
                        .build()

                WorkManager.getInstance(context)
                    .enqueue(uploadWorkRequest)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "start a OneTimeWorkRequest")
        }

        Button(
            onClick = {
                AutoWorkerManager.startLarkJob(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "start a PeriodicWorkRequest")
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(
        text = "Hello $name!",
        fontSize = 32.sp
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun GreetingPreview() {
    AutoHelperTheme {
        Column {
            Greeting("Android")
            ButtonList(LocalContext.current)
        }
    }
}