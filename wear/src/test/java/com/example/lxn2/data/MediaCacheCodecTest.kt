package com.example.lxn2.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaCacheCodecTest {

    // 与生产 createMediaItem 保持一致：extras 中写入 file_path
    private fun item(id: String, title: String, artist: String, album: String, path: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.fromFile(File(path)).toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setExtras(Bundle().apply { putString("file_path", path) })
                    .build()
            )
            .build()

    private fun tempFile(name: String): String =
        File(System.getProperty("java.io.tmpdir"), name).absolutePath

    @Test
    fun emptyContentReturnsEmpty() {
        assertTrue(MediaCacheCodec.decode("").isEmpty())
        assertTrue(MediaCacheCodec.decode("   ").isEmpty())
    }

    @Test
    fun encodeThenDecodeRoundTrips() {
        val p1 = tempFile("a.mp3")
        val p2 = tempFile("b.mp3")
        val items = listOf(
            item("1", "标题", "歌手", "专辑", p1),
            item("2", "Title2", "Artist2", "Album2", p2)
        )

        val json = MediaCacheCodec.encode(items)
        val decoded = MediaCacheCodec.decode(json)

        assertEquals(2, decoded.size)
        assertEquals("1", decoded[0].mediaId)
        assertEquals("标题", decoded[0].mediaMetadata.title)
        assertEquals("歌手", decoded[0].mediaMetadata.artist)
        assertEquals("专辑", decoded[0].mediaMetadata.albumTitle)
        // 关键：file_path 往返一致，且 URI 由同一 File 推得，跨平台成立
        assertEquals(p1, decoded[0].mediaMetadata.extras?.getString("file_path"))
        assertEquals(Uri.fromFile(File(p1)).toString(), decoded[0].localConfiguration?.uri.toString())
        assertEquals("2", decoded[1].mediaId)
        assertEquals(p2, decoded[1].mediaMetadata.extras?.getString("file_path"))
    }

    @Test
    fun decodeOldPipeFormat() {
        val pipe = "1|标题|歌手|专辑|/storage/music/a.mp3|file:///cover/1.jpg\n" +
            "2|Title2|Artist2|Album2|/storage/music/b.mp3|"

        val decoded = MediaCacheCodec.decode(pipe)

        assertEquals(2, decoded.size)
        assertEquals("标题", decoded[0].mediaMetadata.title)
        // 管道格式中 file_path 按字面保留
        assertEquals("/storage/music/a.mp3", decoded[0].mediaMetadata.extras?.getString("file_path"))
        // artworkUri 作为 URI 字符串原样携带
        assertEquals("file:///cover/1.jpg", decoded[0].mediaMetadata.artworkUri.toString())
        assertEquals("/storage/music/b.mp3", decoded[1].mediaMetadata.extras?.getString("file_path"))
    }

    @Test
    fun malformedLinesAreSkipped() {
        val pipe = "onlyTwo|parts|here\n1|标题|歌手|专辑|/storage/music/a.mp3|\n"
        val decoded = MediaCacheCodec.decode(pipe)
        assertEquals(1, decoded.size)
        assertEquals("标题", decoded[0].mediaMetadata.title)
    }
}
