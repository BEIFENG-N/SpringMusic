package com.example.lxn2.data.source

import android.content.Context
import android.util.Log
import com.example.lxn2.data.AppConstants
import app.cash.quickjs.QuickJs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

// 供 JS 调用的 Kotlin 网络接口
interface HttpBridge {
    fun sendRequest(url: String, optionsJson: String): String
}

// 实现内存级 Cookie 持久化
class MemoryCookieJar : CookieJar {
    private val cookieStore = HashMap<String, List<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
}

object MusicSourceEngine {
    // 必须单线程执行 QuickJS 任务，防止底层 C++ 崩溃
    private val jsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var quickJs: QuickJs? = null
    
    // 配置适用于手表端和 QuickJS 单线程桥接的 OkHttpClient (添加超时保护)
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .cookieJar(MemoryCookieJar()) // 注入 Cookie 管理
        .addInterceptor { chain ->
            // 注入全局 User-Agent 伪装
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; SM-G9910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Mobile Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()
        
    private val initDeferred = CompletableDeferred<Unit>()

    suspend fun initEngine(context: Context, activeScriptPath: String?) = withContext(jsDispatcher) {
        try {
            quickJs?.close()
            quickJs = QuickJs.create()

            // 1. 注入 OkHttp 网络能力
            val httpBridge = object : HttpBridge {
                override fun sendRequest(url: String, optionsJson: String): String {
                    return try {
                        val options = JSONObject(optionsJson)
                        val method = options.optString("method", "GET").uppercase()
                        val headers = options.optJSONObject("headers")
                        val reqBodyStr = if (options.has("body")) options.getString("body") else null

                        Log.d(AppConstants.LOG_TAG, "Engine Request ($method): $url")
                        val reqBuilder = Request.Builder().url(url)

                        // 还原 JS 脚本传递的特定 Headers
                        headers?.keys()?.forEach { key ->
                            reqBuilder.addHeader(key, headers.getString(key))
                        }

                        if (method == "POST") {
                            val mediaType = "application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull()
                            val requestBody = (reqBodyStr ?: "").toRequestBody(mediaType)
                            reqBuilder.post(requestBody)
                        } else {
                            reqBuilder.get()
                        }

                        client.newCall(reqBuilder.build()).execute().use { response ->
                            val body = response.body?.string() ?: ""
                            val out = JSONObject()
                            out.put("status", response.code)
                            out.put("body", body) // 保持为原始字符串，防止 JS replace 报错
                            val h = JSONObject()
                            response.headers.forEach { h.put(it.first, it.second) }
                            out.put("headers", h)
                            out.toString()
                        }
                    } catch (e: Exception) {
                        Log.e(AppConstants.LOG_TAG, "Engine HTTP Error", e)
                        JSONObject().apply { put("error", e.message) }.toString()
                    }
                }
            }
            quickJs!!.set("AndroidHttp", HttpBridge::class.java, httpBridge)

            // 2. 注入 JS Polyfill (lx.request 垫片 - 同步版)
            // 由于 QuickJS 0.9.2 不支持异步事件循环，我们将 Promise 模拟为同步立即执行
            val polyfill = """
                globalThis.lx = {
                    request: function(url, options) {
                        try {
                            const resStr = AndroidHttp.sendRequest(url, JSON.stringify(options || {}));
                            const res = JSON.parse(resStr);
                            const response = { statusCode: res.status, headers: res.headers, body: res.body };
                            
                            // 同步执行的 Thenable 对象
                            return {
                                then: function(cb) {
                                    const r = cb(response);
                                    // 递归支持链式调用
                                    return (r && typeof r.then === 'function') ? r : { 
                                        then: (c) => c(r),
                                        catch: () => this
                                    };
                                },
                                catch: function() { return this; }
                            };
                        } catch(e) {
                            return { 
                                then: function() { return this; }, 
                                catch: function(cb) { cb(e); return this; } 
                            };
                        }
                    }
                };
                // 向下兼容旧版脚本用的 lxHttpRequest
                globalThis.lxHttpRequest = function(url, options = {}) {
                    let b;
                    lx.request(url, options).then(res => { b = res.body; });
                    return b;
                };
            """.trimIndent()
            quickJs!!.evaluate(polyfill)

            // 3. 注入默认音源实现 (同步适配器 - 优先网易云)
            val defaultSource = """
                globalThis.lxApi = {
                    search: (keyword, page) => {
                        try {
                            // 优先使用网易云接口
                            const url = 'https://music.163.com/api/search/get/web?s=' + encodeURIComponent(keyword) + '&type=1&limit=20&offset=' + ((page-1)*20);
                            const res = lxHttpRequest(url);
                            const json = JSON.parse(res);
                            const songs = (json.result && json.result.songs) ? json.result.songs : [];
                            return songs.map(s => ({
                                id: s.id.toString(),
                                title: s.name,
                                artist: s.artists && s.artists[0] ? s.artists[0].name : '未知歌手',
                                source: 'wy'
                            }));
                        } catch (e) {
                            try {
                                // 备选酷我接口
                                const url = 'http://search.kuwo.cn/r.s?client=kt&all=' + encodeURIComponent(keyword) + 
                                            '&pn=' + (page-1) + '&rn=20&uid=794762570&ver=kwplayer_ar_9.2.2.1' +
                                            '&vipver=1&show_copyright_off=1&newver=1&ft=music&encoding=utf8&rformat=json&mobi=1';
                                const res = lxHttpRequest(url);
                                const json = JSON.parse(res.replace(/'/g, '"'));
                                return (json.abslist || []).map(s => ({
                                    id: s.MUSICRID.replace('MUSIC_', ''),
                                    title: s.SONGNAME,
                                    artist: s.ARTIST,
                                    source: 'kw'
                                }));
                            } catch (e2) {
                                return [];
                            }
                        }
                    },
                    getMusicUrl: (songId, source) => {
                        if (source === 'wy') {
                            // 网易云外链直连
                            return 'https://music.163.com/song/media/outer/url?id=' + songId + '.mp3';
                        } else if (source === 'kw') {
                            const url = 'http://antiserver.kuwo.cn/anti.s?format=mp3&rid=MUSIC_' + songId + 
                                        '&type=convert_url&response=url&bitrate=128';
                            return lxHttpRequest(url);
                        }
                        return '';
                    },
                    getLyric: (songId, source) => {
                        if (source === 'wy') {
                            try {
                                const url = 'https://music.163.com/api/song/lyric?id=' + songId + '&lv=1&kv=1&tv=-1';
                                const res = lxHttpRequest(url);
                                const json = JSON.parse(res);
                                return (json.lrc && json.lrc.lyric) ? json.lrc.lyric : '';
                            } catch (e) { return ''; }
                        }
                        return '';
                    }
                };
            """.trimIndent()
            quickJs!!.evaluate(defaultSource)

            // 4. 加载自定义音源
            if (activeScriptPath != null) {
                val scriptFile = File(activeScriptPath)
                if (scriptFile.exists()) {
                    val script = scriptFile.readText()
                    quickJs!!.evaluate(script)
                    Log.d(AppConstants.LOG_TAG, "Loaded User API script from: $activeScriptPath")
                }
            }
            initDeferred.complete(Unit)
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "Engine Init Error", e)
            initDeferred.completeExceptionally(e)
        }
    }

    private suspend fun ensureInitialized() {
        initDeferred.await()
    }

    private val searchCache = object : android.util.LruCache<String, String>(30) {}

    suspend fun searchMusic(keyword: String, page: Int = 1): String = withContext(jsDispatcher) {
        val cacheKey = "$keyword::$page"
        searchCache.get(cacheKey)?.let { return@withContext it }

        ensureInitialized()
        try {
            val qjs = quickJs ?: throw Exception("Engine not initialized")
            val safeKeyword = JSONObject.quote(keyword)
            // IIFE 逻辑：探测并同步提取数据
            val result = qjs.evaluate("""
                (function() {
                    if (typeof lxApi !== 'undefined' && typeof lxApi.search === 'function') {
                        let r = lxApi.search($safeKeyword, $page);
                        // 处理可能返回的同步 Thenable
                        if (r && typeof r.then === 'function') {
                           let d;
                           r.then(x => { d = x; });
                           return JSON.stringify(d || []);
                        }
                        return JSON.stringify(r || []);
                    }
                    return "[]";
                })()
            """.trimIndent()) as String

            if (result != "[]") {
                searchCache.put(cacheKey, result)
            }
            result
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "Search Error", e)
            "[]"
        }
    }

    suspend fun getMusicUrl(songId: String, source: String): String = withContext(jsDispatcher) {
        ensureInitialized()
        try {
            val qjs = quickJs ?: throw Exception("Engine not initialized")
            val safeId = JSONObject.quote(songId)
            val safeSource = JSONObject.quote(source)
            val result = qjs.evaluate("""
                (function() {
                    if (typeof lxApi !== 'undefined' && typeof lxApi.getMusicUrl === 'function') {
                        let r = lxApi.getMusicUrl($safeId, $safeSource);
                        if (r && typeof r.then === 'function') {
                           let d;
                           r.then(x => { d = x; });
                           return d;
                        }
                        return r;
                    }
                    return "";
                })()
            """.trimIndent())
            result as String
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "Url Error", e)
            ""
        }
    }

    suspend fun getLyric(songId: String, source: String): String = withContext(jsDispatcher) {
        ensureInitialized()
        try {
            val qjs = quickJs ?: throw Exception("Engine not initialized")
            val safeId = JSONObject.quote(songId)
            val safeSource = JSONObject.quote(source)
            val result = qjs.evaluate("""
                (function() {
                    if (typeof lxApi !== 'undefined' && typeof lxApi.getLyric === 'function') {
                        let r = lxApi.getLyric($safeId, $safeSource);
                        if (r && typeof r.then === 'function') {
                           let d;
                           r.then(x => { d = x; });
                           return d;
                        }
                        return r;
                    }
                    return "";
                })()
            """.trimIndent())
            result as String
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "Lyric Error", e)
            ""
        }
    }
}
