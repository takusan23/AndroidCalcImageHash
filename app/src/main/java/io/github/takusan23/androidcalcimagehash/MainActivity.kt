package io.github.takusan23.androidcalcimagehash

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.androidcalcimagehash.ui.theme.AndroidCalcImageHashTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidCalcImageHashTheme {
                MainScreen()
            }
        }
    }
}

/** ULong を2進数の文字列にする */
private fun ULong.toBinaryString(): String {
    var binString = "0b"
    for (i in 63 downTo 0) {
        binString += if (this and (1UL shl i) != 0UL) "1" else "0"
    }
    return binString
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 画像の Uri
    var image1Uri = remember { mutableStateOf<Uri?>(null) }
    var image2Uri = remember { mutableStateOf<Uri?>(null) }

    // 画像それぞれの aHash、dHash
    val aHash1 = remember { mutableStateOf(0UL) }
    val dHash1 = remember { mutableStateOf(0UL) }
    val aHash2 = remember { mutableStateOf(0UL) }
    val dHash2 = remember { mutableStateOf(0UL) }

    // 比較結果。0 から 1
    val compareAHash = remember { mutableFloatStateOf(0f) }
    val compareDHash = remember { mutableFloatStateOf(0f) }

    val photoPicker1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> image1Uri.value = uri }
    )
    val photoPicker2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> image2Uri.value = uri }
    )

    fun compare() {
        scope.launch {
            val image1Bitmap = ImageHashTool.loadBitmap(context, image1Uri.value!!)!!
            val image2Bitmap = ImageHashTool.loadBitmap(context, image2Uri.value!!)!!

            // それぞれ aHash dHash を求める
            aHash1.value = ImageHashTool.calcAHash(image1Bitmap)
            dHash1.value = ImageHashTool.calcDHash(image1Bitmap)
            aHash2.value = ImageHashTool.calcAHash(image2Bitmap)
            dHash2.value = ImageHashTool.calcDHash(image2Bitmap)

            // 一致していないビットを求める。XOR する
            val aHashXor = aHash1.value xor aHash2.value
            val dHashXor = dHash1.value xor dHash2.value

            // 立っているビットの数を数える
            // 一致しているビットが多ければ少なくなる
            val aHashOneBitCount = aHashXor.countOneBits()
            val dHashOneBitCount = dHashXor.countOneBits()

            // 一致度を出す
            // 64 はハッシュ値が 64 ビットなので
            compareAHash.floatValue = (64 - aHashOneBitCount) / 64f
            compareDHash.floatValue = (64 - dHashOneBitCount) / 64f
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) }) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            Button(onClick = { photoPicker1.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(text = "1枚目の画像を選ぶ")
            }

            Button(onClick = { photoPicker2.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(text = "2枚目の画像を選ぶ")
            }

            Button(onClick = { compare() }) {
                Text(text = "一致度を計算")
            }

            Row {
                if (image1Uri.value != null) {
                    Column {
                        Text(text = "1枚目")
                        UriImagePreview(
                            modifier = Modifier.requiredSize(100.dp),
                            uri = image1Uri.value!!
                        )
                    }
                }
                if (image2Uri.value != null) {
                    Column {
                        Text(text = "2枚目")
                        UriImagePreview(
                            modifier = Modifier.requiredSize(100.dp),
                            uri = image2Uri.value!!
                        )
                    }
                }
            }

            Text(text = "それぞれの aHash dHash")
            Text(text = "aHash1 = ${aHash1.value.toBinaryString()}")
            Text(text = "dHash1 = ${dHash1.value.toBinaryString()}")
            Text(text = "aHash2 = ${aHash2.value.toBinaryString()}")
            Text(text = "dHash2 = ${dHash2.value.toBinaryString()}")

            Text(text = "一致度")
            Text(text = "aHash = ${compareAHash.floatValue}")
            Text(text = "dHash = ${compareDHash.floatValue}")

        }
    }
}

/** Uri の画像を表示する */
@Composable
private fun UriImagePreview(
    modifier: Modifier = Modifier,
    uri: Uri
) {
    val context = LocalContext.current
    val image = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(key1 = uri) {
        // TODO 自分で Bitmap を読み込むのではなく、Glide や Coil を使うべきです
        image.value = ImageHashTool.loadBitmap(context, uri)?.asImageBitmap()
    }

    if (image.value != null) {
        Image(
            modifier = modifier,
            bitmap = image.value!!,
            contentDescription = null
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}